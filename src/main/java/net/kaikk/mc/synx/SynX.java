package net.kaikk.mc.synx;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.kaikk.mc.synx.packets.ChannelListener;
import net.kaikk.mc.synx.packets.Node;
import net.kaikk.mc.synx.packets.NodePacket;
import net.kaikk.mc.synx.packets.Packet;

public class SynX extends JavaPlugin implements ChannelListener {
	Node node;
	Map<String,Node> nodes = new ConcurrentHashMap<String,Node>();
	Map<Integer,Node> nodesById = new ConcurrentHashMap<Integer,Node>();
	Map<String,Set<Node>> tags = new ConcurrentHashMap<String,Set<Node>>();
	Map<String,Map<Plugin,ChannelListener>> registeredListeners = new ConcurrentHashMap<String, Map<Plugin,ChannelListener>>();
	String registeredChannelsSQLList = "";
	
	private static SynX instance;
	private Config config;
	private DataStore ds;
	private DataExchangerThread dataExchangerThread;
	
	@Override
	public void onEnable() {
		instance=this;
		this.nodes.clear();
		this.nodesById.clear();
		this.tags.clear();
		this.registeredListeners.clear();
		
		config = new Config(instance);
		
		try {
			ds = new DataStore(instance);
			
			this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
			this.getCommand(this.getName()).setExecutor(new CommandExec(this));
			
			dataExchangerThread = new DataExchangerThread(instance);
			
			instance.register(instance, "SynX", instance);
			
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeInt(1);
			
			out.writeInt(node.getId());
			out.writeUTF(node.getName());
			out.writeInt(node.getTags().length);
			for (String s : node.getTags()) {
				out.writeUTF(s);
			}
			
			instance.broadcast("SynX", out.toByteArray(), System.currentTimeMillis()+60000L);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					dataExchangerThread.start();
				}
			}.runTask(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onDisable() {
		if (this.dataExchangerThread!=null) {
			this.dataExchangerThread.interrupt();
		}
		
		instance = null;
	}
	
	public static SynX instance() {
		return instance;
	}

	DataStore dataStore() {
		return ds;
	}
	
	Config config() {
		return config;
	}

	/** 
	 * Sends data to one or more nodes.<br>
	 * Suggestion: use ByteStreams.newDataOutput() for generating data to be sent.
	 * Data can't be more than 32726 bytes long.
	 * */
	public void send(String channel, byte[] data, Node... destination) {
		this.send(channel, data, this.defaultTimeOfDeath(), destination);
	}
	
	/** 
	 * Sends data to one or more nodes <br>
	 * Suggestion: use ByteStreams.newDataOutput() for generating data to be sent.
	 * Data can't be more than 32726 bytes long.
	 * */
	public void send(String channel, byte[] data, long timeOfDeath, Node... destination) {
		if (destination==null || destination.length==0) {
			new IllegalArgumentException("Invalid destination.");
		}
		
		if (data.length>32726) {
			new IllegalArgumentException("Data can't be longer than 32726 bytes.");
		}
		
		this.dataExchangerThread.sendPacket(new NodePacket(this.node, channel, data, timeOfDeath, destination));
	}
	
	/** 
	 * Sends data to all known nodes. <br>
	 * Suggestion: use ByteStreams.newDataOutput() for generating data to be sent.
	 * Data can't be more than 32726 bytes long.
	 * */
	public void broadcast(String channel, byte[] data) {
		this.broadcast(channel, data, this.defaultTimeOfDeath());
	}
	
	/** 
	 * Sends data to all known nodes. <br>
	 * Suggestion: use ByteStreams.newDataOutput() for generating data to be sent.
	 * Data can't be more than 32726 bytes long.
	 * */
	public void broadcast(String channel, byte[] data, long timeOfDeath) {
		if (data.length>32726) {
			new IllegalArgumentException("Data can't be longer than 32726 bytes.");
		}
		this.dataExchangerThread.sendPacket(new NodePacket(this.node, channel, data, timeOfDeath, this.nodes.values().toArray(new Node[this.nodes.size()])));
	}
	
	public Set<Node> getNodesByTag(String... tags) {
		Set<Node> selectedNodes = new HashSet<Node>();
		for (String tag : tags) {
			Set<Node> tagNodes = this.tags.get(tag);
			if (tagNodes!=null) {
				selectedNodes.addAll(tagNodes);
			}
		}
		
		return selectedNodes;
	}

	/** Register a channel listener for a plugin. */
	public void register(Plugin instance, String channel, ChannelListener channelListener) {
		if (channel==null) {
			throw new NullPointerException("Channel can't be null!");
		}
		if (channel.isEmpty()) {
			throw new IllegalArgumentException("Channel can't be empty!");
		}
		if (!Utils.isAlphanumeric(channel)) {
			throw new IllegalArgumentException("Channel must be alphanumeric!");
		}
		
		Map<Plugin, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) {
			map = new ConcurrentHashMap<Plugin, ChannelListener>();
			this.registeredListeners.put(channel, map);
		}
		
		map.put(instance, channelListener);
		this.generateRegisteredChannelsSQLList();
	}
	
	/** Unregister the specified channel listener for the specified plugin instance.
	 * @return true if the registered channel has been registered, false otherwise.
	 * */
	public boolean unregister(Plugin instance, String channel) {
		Map<Plugin, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) { // the channel is not registered
			return false;
		}
		if (map.remove(instance) != null) {
			this.generateRegisteredChannelsSQLList();
			return true;
		}
		return false;
	}
	
	public void unregisterAll(Plugin instance) {
		for (Map<Plugin, ChannelListener> map : this.registeredListeners.values()) {
			map.remove(instance);
		}
		this.generateRegisteredChannelsSQLList();
	}
	
	public ChannelListener getChannelListener(Plugin instance, String channel) {
		Map<Plugin, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) {
			return null;
		}
		
		return map.get(instance);
	}

	public Node getNode() {
		return this.node;
	}

	public Map<String, Node> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	public Set<String> getTags() {
		return Collections.unmodifiableSet(this.tags.keySet());
	}
	
	public Set<Node> getTagNodes(String tag) {
		return Collections.unmodifiableSet(this.tags.get(tag));
	}
	
	public long defaultTimeOfDeath() {
		return System.currentTimeMillis()+this.config.defaultTTL;
	}
	
	private void generateRegisteredChannelsSQLList() {
		StringBuilder sb = new StringBuilder();
		for (String s : this.registeredListeners.keySet()) {
			sb.append('"');
			sb.append(s);
			sb.append('"');
			sb.append(',');
		}
		int l = sb.length();
		if (l>0) {
			sb.setLength(l-1);
		}
		this.registeredChannelsSQLList = sb.toString(); 
	}
	
	@Override
	public void onPacketReceived(Packet packet) {
		instance.debug("Received packet on the SynX channel from ", packet.getFrom().getId(), ":", packet.getFrom().getName());
		
		ByteArrayDataInput in = packet.getDataInputStream();
		int code = in.readInt();
		switch(code) {
			case 1:
				if (this.node.getId()==packet.getFrom().getId()) {
					break;
				}
				
				int id = in.readInt();
				String name = in.readUTF();
				int l = in.readInt();
				String[] tags = new String[l];
				for (int i=0; i<l; i++) {
					tags[i] = in.readUTF();
				}
				
				instance.addNode(new Node(id, name, tags));
				instance.debug("Added/updated node "+id+":"+name);
				break;
			default:
				instance.debug("Unknown data code "+code);
		}
	}
	
	void addNode(Node node) {
		instance.nodes.put(node.getName(), node);
		instance.nodesById.put(node.getId(), node);
		
		for (String tag : node.getTags()) {
			Set<Node> nodes = instance.tags.get(tag);
			if (nodes==null) {
				nodes = Collections.synchronizedSet(new HashSet<Node>());
				instance.tags.put(tag, nodes);
			}
			nodes.add(node);
		}
	}
	
	void debug(Object... message) {
		if (this.config().debug) {
			StringBuilder sb = new StringBuilder();
			for (Object obj : message) {
				sb.append(obj);
			}
			this.getLogger().info(sb.toString());
		}
	}
}
