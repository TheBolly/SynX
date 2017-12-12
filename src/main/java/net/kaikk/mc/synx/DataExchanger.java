package net.kaikk.mc.synx;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import net.kaikk.mc.kaiscommons.mysql.MySQLConnection;
import net.kaikk.mc.synx.packets.ChannelListener;
import net.kaikk.mc.synx.packets.Node;
import net.kaikk.mc.synx.packets.NodePacket;
import net.kaikk.mc.synx.packets.Packet;

public class DataExchanger {
	protected long lastHourlyTask;
	protected final Queue<NodePacket> sendQueue = new LinkedBlockingQueue<>();
	protected final SynX instance;
	protected final MySQLConnection<SQLQueries> connection;
	protected final DataReceiver receiver;
	protected final DataSender sender;
	protected final Maintenance maintenance;
	protected final PacketsDispatcher dispatcher;
	
	protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

	public DataExchanger(SynX instance) throws SQLException {
		this.instance = instance;
		this.connection = new MySQLConnection<>(instance.config.getDataSource(), SQLQueries.class);
		this.receiver = new DataReceiver();
		this.sender = new DataSender();
		this.maintenance = new Maintenance();
		this.dispatcher = new PacketsDispatcher();
	}

	public void init() throws SQLException {
		try {
			this.connection.queries().createTables();
			ResultSet rs = this.connection.queries().getNodes();
			List<Node> nodesToBeAdded = new ArrayList<Node>();
			while (rs.next()) {
				nodesToBeAdded.add(new Node(rs.getInt(1), rs.getString(2), rs.getString(4).split(",")));
			}

			Iterator<Node> it = nodesToBeAdded.iterator();
			while (it.hasNext()) {
				Node n = it.next();
				if (n.getName().equals(instance.config().nodeName)) {
					if (!SynXUtils.compareCollections(Arrays.asList(n.getTags()), instance.config().tags)) {
						it.remove(); // database data needs update!
						instance.log("Node data needs to be updated!");
					} else {
						instance.node = n;
					}
					break;
				}
			}

			for (Node n : nodesToBeAdded) {
				instance.addNode(n);
			}

			if (instance.node == null) {
				instance.log("Initializing node");
				rs = this.connection.queries().insertNode(instance.config().nodeName, String.join(",", instance.config().tags));
				rs.next();
				int id = rs.getInt(1);

				instance.node = new Node(id, instance.config().nodeName, instance.config().tags.toArray(new String[instance.config().tags.size()]));
				instance.addNode(instance.node);
			}

			instance.log("Loaded "+instance.nodes.size()+" nodes");
		} finally {
			this.connection.close();
		}
	}
	
	public void start() {
		this.scheduler.scheduleWithFixedDelay(this.getMaintenance(), 0, 1, TimeUnit.HOURS);
		this.scheduler.scheduleWithFixedDelay(this.getSender(), 0, instance.config.interval, TimeUnit.MILLISECONDS);
		this.scheduler.scheduleWithFixedDelay(this.getReceiver(), 0, instance.config.interval, TimeUnit.MILLISECONDS);
		this.dispatcher.start();
	}

	public void shutdown() {
		scheduler.shutdownNow();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				instance.log("Timeout while waiting for pending operations to complete!");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.dispatcher.shutdown();
	}

	public void sendPacket(NodePacket packet) {
		this.sendQueue.add(packet);
	}

	public class DataReceiver implements Runnable {
		final Collection<Long> idsToDelete = new ArrayList<Long>();

		@Override
		public void run() {
			if (instance.registeredChannelsSQLList.isEmpty()) {
				return;
			}
			try {
				ResultSet rs = connection.queries().dataReceiverSelect(instance.node.getId());
				while (rs.next()) {
					idsToDelete.add(rs.getLong(1));
					Node remoteNode = instance.nodesById.get(rs.getInt(2));
					Packet packet = new Packet(remoteNode != null ? remoteNode : new Node(rs.getInt(2), "UNKNOWN_NODE", new String[] {}), rs.getString(3), rs.getBytes(4), rs.getLong(5));
					dispatcher.dispatchQueue.put(packet);
				}

				if (!idsToDelete.isEmpty()) {
					connection.queries().deleteTransfers(idsToDelete);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {

			} finally {
				connection.close();
				idsToDelete.clear();
			}
		}
	}

	public class DataSender implements Runnable {
		@Override
		public void run() {
			NodePacket packet;
			boolean useConnection = false;
			try {
				while ((packet = sendQueue.poll()) != null) {
					instance.debug("Sending " + packet);

					final Set<Node> nodes = new HashSet<Node>();
					for (Node node : packet.getNodes()) {
						nodes.add(node);
					}

					nodes.remove(null);

					if (nodes.remove(instance.node)) {
						// This packet has to be sent to this server too, so instead of sending it to the MySQL server, just send it to the dispatcher.
						try {
							dispatcher.dispatchQueue.put(packet);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					if (nodes.isEmpty()) {
						continue;
					}

					try {
						useConnection = true;
						ResultSet rs = connection.queries().insertData(packet.getFrom().getId(), packet.getChannel(), packet.getTimeOfDeath(), packet.getData());
						rs.next();
						int id = rs.getInt(1);
						connection.queries().insertTransfers(nodes, id);
					} catch (SQLException e) {
						// TODO handle connection issues, so the packet is sent one more time
						e.printStackTrace();
					}
				}
			} finally {
				if (useConnection) {
					connection.close();
				}
			}
		}
	}

	public class Maintenance implements Runnable {
		@Override
		public void run() {
			try {
				connection.queries().updateLastActivity(instance.getNode().getId());
				connection.queries().purgeTransfers();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				connection.close();
			}
		}
	}

	public class PacketsDispatcher extends Thread {
		protected SynchronousQueue<Packet> dispatchQueue = new SynchronousQueue<>();
		protected volatile boolean running = true;

		public PacketsDispatcher() {
			super("SynX-PacketDispatcher");
		}
		
		@Override
		public void run() {
			while(running) {
				try {
					this.processPacket(dispatchQueue.take());
				} catch (InterruptedException e) {
					Packet packet;
					while ((packet = dispatchQueue.poll()) != null) {
						this.processPacket(packet);
					}
					break;
				}
			}
		}
		
		protected void processPacket(Packet packet) {
			Map<?, ChannelListener> map = instance.registeredListeners.get(packet.getChannel());
			if (map == null) {
				return;
			}
			for (Entry<?, ChannelListener> entry : map.entrySet()) {
				try {
					instance.debug("Dispatching ", packet, " to ", entry.getKey());
					entry.getValue().onPacketReceived(packet);
				} catch (Throwable e) {
					instance.log("An exception has been thrown by " + entry.getKey() + " while executing onPacketReceived.");
					e.printStackTrace();
				}
			}
		}
		
		public void shutdown() {
			this.running = false;
			this.interrupt();
		}
	}

	public DataReceiver getReceiver() {
		return receiver;
	}

	public DataSender getSender() {
		return sender;
	}

	public Maintenance getMaintenance() {
		return maintenance;
	}

	public PacketsDispatcher getDispatcher() {
		return dispatcher;
	}
}
