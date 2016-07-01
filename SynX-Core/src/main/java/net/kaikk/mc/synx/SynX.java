package net.kaikk.mc.synx;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.kaikk.mc.synx.packets.ChannelListener;
import net.kaikk.mc.synx.packets.Node;
import net.kaikk.mc.synx.packets.NodePacket;
import net.kaikk.mc.synx.packets.Packet;

public class SynX implements ChannelListener {
	Node node;
	Map<String,Node> nodes = new ConcurrentHashMap<String,Node>();
	Map<Integer,Node> nodesById = new ConcurrentHashMap<Integer,Node>();
	Map<String,Set<Node>> tags = new ConcurrentHashMap<String,Set<Node>>();
	
	Map<String,Map<Object,ChannelListener>> registeredListeners = new ConcurrentHashMap<String, Map<Object,ChannelListener>>();
	String registeredChannelsSQLList = "";
	
	private static SynX instance;
	private ISynX implementation;
	private Config config;
	private DataExchangerThread dataExchangerThread;
	
	private Map<String,Node> nodesUnmodifiable = Collections.unmodifiableMap(nodes);
	private Map<String,Set<Node>> tagsUnmodifiable = Collections.unmodifiableMap(tags);

	private SynX() {}
	
	public static SynX instance() {
		return instance;
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

	/** Get this server's node */
	public Node getNode() {
		return this.node;
	}
	
	public Node getNode(String name) {
		return this.nodes.get(name);
	}
	
	public Node getNode(int id) {
		return this.nodesById.get(id);
	}

	public Map<String, Node> getNodes() {
		return this.nodesUnmodifiable;
	}

	public Set<String> getTags() {
		return this.tagsUnmodifiable.keySet();
	}
	
	public Set<Node> getTagNodes(String tag) {
		return this.tagsUnmodifiable.get(tag);
	}
	
	public long defaultTimeOfDeath() {
		return System.currentTimeMillis()+this.config.defaultTTL;
	}
	
	public void addNode(Node node) {
		this.nodes.put(node.getName(), node);
		this.nodesById.put(node.getId(), node);
		
		for (String tag : node.getTags()) {
			Set<Node> nodes = this.tags.get(tag);
			if (nodes==null) {
				nodes = Collections.synchronizedSet(new HashSet<Node>());
				this.tags.put(tag, nodes);
			}
			nodes.add(node);
		}
	}
	
	/** Register a channel listener for a plugin. */
	public void register(Object pluginInstance, String channel, ChannelListener channelListener) {
		if (channel==null) {
			throw new NullPointerException("Channel can't be null!");
		}
		if (channel.isEmpty()) {
			throw new IllegalArgumentException("Channel can't be empty!");
		}
		if (!Utils.isAlphanumeric(channel)) {
			throw new IllegalArgumentException("Channel must be alphanumeric!");
		}
		
		Map<Object, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) {
			map = new ConcurrentHashMap<Object, ChannelListener>();
			this.registeredListeners.put(channel, map);
		}
		
		map.put(pluginInstance, channelListener);
		this.generateRegisteredChannelsSQLList();
	}
	
	/** Unregister the specified channel listener for the specified plugin instance.
	 * @return true if the registered channel has been registered, false otherwise.
	 * */
	public boolean unregister(Object pluginInstance, String channel) {
		Map<Object, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) { // the channel is not registered
			return false;
		}
		if (map.remove(pluginInstance) != null) {
			this.generateRegisteredChannelsSQLList();
			return true;
		}
		return false;
	}
	
	public void unregisterAll(Object pluginInstance) {
		for (Map<Object, ChannelListener> map : this.registeredListeners.values()) {
			map.remove(pluginInstance);
		}
		this.generateRegisteredChannelsSQLList();
	}
	
	public ChannelListener getChannelListener(Object pluginInstance, String channel) {
		Map<Object, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) {
			return null;
		}
		
		return map.get(pluginInstance);
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
		this.debug("Received packet on the SynX channel from ", packet.getFrom().getId(), ":", packet.getFrom().getName());
		
		ByteArrayDataInput in = packet.getDataInputStream();
		int code = in.readInt();
		switch(code) {
			case 1:
				if (this.getNode().getId()==packet.getFrom().getId()) {
					break;
				}
				
				int id = in.readInt();
				String name = in.readUTF();
				int l = in.readInt();
				String[] tags = new String[l];
				for (int i=0; i<l; i++) {
					tags[i] = in.readUTF();
				}
				
				this.addNode(new Node(id, name, tags));
				this.debug("Added/updated node "+id+":"+name);
				break;
			default:
				this.debug("Unknown data code "+code);
		}
	}
	
	public void debug(Object... message) {
		if (this.config().debug) {
			StringBuilder sb = new StringBuilder();
			for (Object obj : message) {
				sb.append(obj);
			}
			if (this.implementation!=null) {
				this.implementation.log(sb.toString());
			} else {
				System.out.println(sb.toString());
			}
		}
	}
	
	public void log(String message) {
		this.implementation.log(message);
	}
	
	/** 
	 * Initialize SynX implementation.
	 * @throws Exception 
	 * */
	public static SynX initialize(ISynX implementation) throws Exception {
		SynX inst = new SynX();
		inst.implementation = implementation;
		inst.nodes.clear();
		inst.nodesById.clear();
		inst.tags.clear();
		
		inst.config = implementation.loadConfig();
		
		inst.dataExchangerThread = new DataExchangerThread(inst);
		
		inst.registeredListeners.clear();
		inst.register(inst, "SynX", inst);
		
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeInt(1);
		
		out.writeInt(inst.node.getId());
		out.writeUTF(inst.node.getName());
		out.writeInt(inst.node.getTags().length);
		for (String s : inst.node.getTags()) {
			out.writeUTF(s);
		}
		
		inst.broadcast("SynX", out.toByteArray(), System.currentTimeMillis()+60000L);
		
		instance = inst;
		return inst;
	}
	
	/** 
	 * Called by the SynX implementation after initialization, usually on the first server tick.
	 * */
	public void startDataExchangerThread() {
		dataExchangerThread.start();
	}
	
	/** 
	 * Called by the SynX implementation to deinitialize SynX, usually on server stop or implementation disabling.
	 * */
	public void deinitialize() {
		this.stopDataExchangerThread();
		this.implementation = null;
		instance = null;
	}
	
	/** 
	 * Called by the SynX implementation to stop the DataExchangerThread. This is also called by the deinitialize method.
	 * */
	public void stopDataExchangerThread() {
		if (this.dataExchangerThread!=null) {
			this.dataExchangerThread.interrupt();
		}
	}

	@Override
	public String toString() {
		return "SynX";
	}

	public ISynX getImplementation() {
		return implementation;
	}
}
