package net.kaikk.mc.synx;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import net.kaikk.mc.synx.packets.Node;
import net.kaikk.mc.synx.packets.NodePacket;
import net.kaikk.mc.synx.packets.Packet;

class DataExchangerThread extends Thread {
	private Queue<NodePacket> sendQueue = new LinkedBlockingQueue<NodePacket>();
	private SynX instance;
	private final PacketsDispatcherThread dispatcher;
	private long lastHourlyTask;
	private DataStore ds;
	
	DataExchangerThread(SynX instance) throws Exception {
		super("SynXDataExchangerThread");
		this.instance = instance;
		this.dispatcher = new PacketsDispatcherThread(instance);
		this.ds = new DataStore(instance);
	}
	
	@Override
	synchronized public void run() {
		instance.debug("DataExchangerThread: started");
		this.dispatcher.start();
		for(;;) {
			// receive packets
			this.receivePackets();
			
			// send pending packets, if any
			this.sendPendingPackets();
			
			if (!this.isInterrupted() && System.currentTimeMillis()-lastHourlyTask>3600000) {
				instance.debug("DataExchangerThread: running hourly task");
				try {
					lastHourlyTask = System.currentTimeMillis();
					Statement s = ds.statement();
					// cleanup
					s.executeUpdate("START TRANSACTION");
					s.executeUpdate("DELETE d, t FROM synx_data AS d LEFT OUTER JOIN synx_transfers AS t ON t.dataid = d.id WHERE d.tod < "+System.currentTimeMillis());
					s.executeUpdate("UPDATE synx_servers SET lastaction = "+System.currentTimeMillis()+" WHERE id ="+instance.node.getId());
					s.executeUpdate("COMMIT");
				} catch (SQLException e) {
					e.printStackTrace();
					try {
						ds.statement().executeUpdate("ROLLBACK");
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
			
			// sleep
			try {
				Thread.sleep(instance.config().waitTime);
			} catch (InterruptedException e) {
				// plugin is going to be stopped... send pending packets
				this.sendPendingPackets();
				break;
			}
		}
		instance.debug("DataExchangerThread: stopped");
		if (ds!=null) {
			ds.dbClose();
		}
	}
	
	@Override
	public void interrupt() {
		this.dispatcher.interrupt();
		super.interrupt();
	}
	
	public void sendPacket(NodePacket packet) {
		this.sendQueue.add(packet);
	}
	
	protected void sendPendingPackets() {
		NodePacket packet;
		while((packet = this.sendQueue.poll())!=null) {
			instance.debug("Sending "+packet);
			
			Set<Node> nodes = new HashSet<Node>();
			for (Node node : packet.getNodes()) {
				nodes.add(node);
			}
			
			nodes.remove(null);
			
			if (nodes.remove(instance.node)) {
				// This packet has to be sent to this server too, so instead of sending it to the MySQL server, just send it to the dispatcher.
				this.dispatcher.dispatchQueue.add(packet);
			}
			
			if (nodes.isEmpty()) {
				continue;
			}
			
			try {
				ds.statement().executeUpdate("START TRANSACTION");
				
				PreparedStatement ps = ds.prepareStatement("INSERT INTO synx_data (req, channel, tod, dat) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, packet.getFrom().getId());
				ps.setString(2, packet.getChannel());
				ps.setLong(3, packet.getTimeOfDeath());
				ps.setBytes(4, packet.getData());
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				rs.next();
				int id = rs.getInt(1);
				
				StringBuilder sb = new StringBuilder();
				for (Node node : nodes) {
					sb.append("("+node.getId()+","+id+"),");
				}
			
				sb.setLength(sb.length()-1);
				ds.statement().executeUpdate("INSERT INTO synx_transfers (dest, dataid) VALUES "+sb);
				ds.statement().executeUpdate("COMMIT");
			} catch (SQLException e) {
				// TODO handle connection issues, so the packet is sent one more time
				e.printStackTrace();
				try {
					ds.statement().executeUpdate("ROLLBACK");
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	protected void receivePackets() {
		if (instance.registeredChannelsSQLList.isEmpty()) {
			return;
		}
		try {
			ds.statement().executeUpdate("START TRANSACTION");
			ResultSet rs = ds.statement().executeQuery("SELECT t.id, d.req, d.channel, d.dat, d.tod FROM synx_transfers AS t, synx_data AS d WHERE t.dest = "+instance.node.getId()+" AND t.dataid = d.id AND d.channel IN ("+instance.registeredChannelsSQLList+")");
			
			StringBuilder sb = new StringBuilder();
			while (rs.next()) {
				sb.append(rs.getLong(1));
				sb.append(',');
				Node remoteNode = instance.nodesById.get(rs.getInt(2));
				Packet packet = new Packet(remoteNode!=null ? remoteNode : new Node(rs.getInt(2), "UNKNOWN_NODE", new String[]{}), rs.getString(3), rs.getBytes(4), rs.getLong(5));
				this.dispatcher.dispatchQueue.add(packet);
			}
			
			int l = sb.length();
			if (l>0) {
				sb.setLength(l-1);
				ds.statement().executeUpdate("DELETE FROM synx_transfers WHERE id IN ("+sb+")");
				ds.statement().executeUpdate("COMMIT");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				ds.statement().executeUpdate("ROLLBACK");
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
}
