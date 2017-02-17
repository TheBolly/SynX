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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import net.kaikk.mc.kaiscommons.mysql.MySQLConnection;
import net.kaikk.mc.synx.packets.ChannelListener;
import net.kaikk.mc.synx.packets.Node;
import net.kaikk.mc.synx.packets.NodePacket;
import net.kaikk.mc.synx.packets.Packet;

public class DataExchanger {
	protected long lastHourlyTask;
	protected final Queue<NodePacket> sendQueue = new LinkedBlockingQueue<>();
	protected final SynX instance;
	protected final MySQLConnection<MySQLQueries> connection;
	protected final DataReceiver receiver;
	protected final DataSender sender;
	protected final Maintenance maintenance;
	protected final PacketsDispatcher dispatcher;

	public DataExchanger(SynX instance) throws SQLException {
		this.instance = instance;
		this.connection = new MySQLConnection<>(instance.config.getDataSource(), MySQLQueries.class);
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

	public void sendPacket(NodePacket packet) {
		this.sendQueue.add(packet);
	}

	public class DataReceiver extends Locked implements Runnable {
		final Collection<Long> idsToDelete = new ArrayList<Long>();

		@Override
		public void run() {
			if (instance.registeredChannelsSQLList.isEmpty()) {
				return;
			}
			if (!lock.tryLock()) {
				return;
			}
			try {
				try {
					ResultSet rs = connection.queries().dataReceiverSelect(instance.node.getId());
					while (rs.next()) {
						idsToDelete.add(rs.getLong(1));
						Node remoteNode = instance.nodesById.get(rs.getInt(2));
						Packet packet = new Packet(remoteNode != null ? remoteNode : new Node(rs.getInt(2), "UNKNOWN_NODE", new String[] {}), rs.getString(3), rs.getBytes(4), rs.getLong(5));
						dispatcher.dispatchQueue.add(packet);
					}

					if (!idsToDelete.isEmpty()) {
						connection.queries().deleteTransfers(idsToDelete);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					connection.close();
					idsToDelete.clear();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public class DataSender extends Locked implements Runnable {
		@Override
		public void run() {
			if (!lock.tryLock()) {
				return;
			}
			NodePacket packet;
			boolean useConnection = false;
			try {
				while ((packet = sendQueue.poll()) != null) {
					instance.debug("Sending " + packet);

					Set<Node> nodes = new HashSet<Node>();
					for (Node node : packet.getNodes()) {
						nodes.add(node);
					}

					nodes.remove(null);

					if (nodes.remove(instance.node)) {
						// This packet has to be sent to this server too, so instead of sending it to the MySQL server, just send it to the dispatcher.
						dispatcher.dispatchQueue.add(packet);
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
				lock.unlock();
			}
		}
	}

	public class Maintenance extends Locked implements Runnable {
		@Override
		public void run() {
			if (!lock.tryLock()) {
				return;
			}
			try {
				connection.queries().updateLastActivity(instance.getNode().getId());
				connection.queries().purgeTransfers();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				connection.close();
				lock.unlock();
			}
		}
	}

	public class PacketsDispatcher extends Locked implements Runnable {
		protected LinkedBlockingQueue<Packet> dispatchQueue = new LinkedBlockingQueue<>();
		private final ReentrantLock lock2 = new ReentrantLock();

		@Override
		public void run() {
			if (!lock2.tryLock()) {
				return;
			}
			try {
				Packet packet = dispatchQueue.take();
				lock.lock();
				do {
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
				} while ((packet = dispatchQueue.poll()) != null);
			} catch (InterruptedException e) {

			} finally {
				lock.unlock();
				lock2.unlock();
			}
		}
	}

	static class Locked {
		public final ReentrantLock lock = new ReentrantLock();
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
