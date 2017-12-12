package net.kaikk.mc.synx;

import java.io.Serializable;
import java.util.Collection;
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
	protected Node node;
	protected Map<String,Node> nodes = new ConcurrentHashMap<String,Node>();
	protected Map<Integer,Node> nodesById = new ConcurrentHashMap<Integer,Node>();
	protected Map<String,Set<Node>> tags = new ConcurrentHashMap<String,Set<Node>>();

	protected Map<String,Map<Object,ChannelListener>> registeredListeners = new ConcurrentHashMap<String, Map<Object,ChannelListener>>();
	protected String registeredChannelsSQLList = "";

	protected static SynX instance;
	protected ISynX implementation;
	protected Config config;
	protected DataExchanger dataExchanger;

	protected Map<String,Node> nodesUnmodifiable = Collections.unmodifiableMap(nodes);
	protected Map<String,Set<Node>> tagsUnmodifiable = Collections.unmodifiableMap(tags);

	protected SynX() {}

	public static SynX instance() {
		return instance;
	}

	public Config config() {
		return config;
	}

	/**
	 * Sends data to one or more nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, byte[] data, Node... destination) {
		this.send(channel, data, this.defaultTimeOfDeath(), destination);
	}
	
	/**
	 * Sends data to one or more nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, byte[] data, Collection<Node> destination) {
		this.send(channel, data, destination.toArray(new Node[destination.size()]));
	}

	/**
	 * Sends data to one or more nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, Serializable object, Node... destination) {
		this.send(channel, SynXUtils.convertToBytes(object), destination);
	}
	
	/**
	 * Sends data to one or more nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, Serializable object, Collection<Node> destination) {
		this.send(channel, object, destination.toArray(new Node[destination.size()]));
	}
	

	/**
	 * Sends data to one or more nodes
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, byte[] data, long timeOfDeath, Node... destination) {
		if (destination==null || destination.length==0) {
			new IllegalArgumentException("Invalid destination.");
		}

		if (data.length>16777215) {
			new IllegalArgumentException("Data can't be longer than 16MiB.");
		}

		this.dataExchanger.sendPacket(new NodePacket(this.node, channel, data, timeOfDeath, destination));
	}
	
	/**
	 * Sends data to one or more nodes
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, byte[] data, long timeOfDeath, Collection<Node> destination) {
		this.send(channel, data, timeOfDeath, destination.toArray(new Node[destination.size()]));
	}

	/**
	 * Sends data to one or more nodes
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, Serializable object, long timeOfDeath, Node... destination) {
		this.send(channel, SynXUtils.convertToBytes(object), timeOfDeath, destination);
	}
	
	/**
	 * Sends data to one or more nodes
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 * @param destination nodes that will receive the data
	 */
	public void send(String channel, Serializable object, long timeOfDeath, Collection<Node> destination) {
		this.send(channel, object, timeOfDeath, destination.toArray(new Node[destination.size()]));
	}

	/**
	 * Sends data to all known nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 */
	public void broadcast(String channel, byte[] data) {
		this.broadcast(channel, data, this.defaultTimeOfDeath());
	}

	/**
	 * Sends data to all known nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 */
	public void broadcast(String channel, Serializable object) {
		this.broadcast(channel, SynXUtils.convertToBytes(object));
	}

	/**
	 * Sends data to all known nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param data data to be sent
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 */
	public void broadcast(String channel, byte[] data, long timeOfDeath) {
		if (data.length>16777215) {
			new IllegalArgumentException("Data can't be longer than 16MiB.");
		}
		this.dataExchanger.sendPacket(new NodePacket(this.node, channel, data, timeOfDeath, this.nodes.values().toArray(new Node[this.nodes.size()])));
	}

	/**
	 * Sends data to all known nodes.
	 * Data can't be more than 16MiB long.
	 * @param channel the channel
	 * @param object the object to be sent to other servers
	 * @param timeOfDeath after the specified time in milliseconds since the epoch this packet data will be removed
	 */
	public void broadcast(String channel, Serializable object, long timeOfDeath) {
		this.broadcast(channel, SynXUtils.convertToBytes(object), timeOfDeath);
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

	/**
	 * @return this instance node
	 */
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

	/**
	 * Register a channel listener for a plugin.
	 * @param pluginInstance the plugin instance
	 * @param channel the channel, max 8 characters
	 * @param channelListener a class implementing ChannelListener
	 * @throws NullPointerException if the channel is null
	 * @throws IllegalArgumentException if the channel is empty, not alphanumeric, or longer than 8 characters.
	 */
	public void register(Object pluginInstance, String channel, ChannelListener channelListener) {
		if (channel==null) {
			throw new NullPointerException("Channel can't be null!");
		}
		if (channel.isEmpty()) {
			throw new IllegalArgumentException("Channel can't be empty!");
		}
		if (!SynXUtils.isAlphanumeric(channel)) {
			throw new IllegalArgumentException("Channel must be alphanumeric!");
		}
		if (channel.length()>16) {
			throw new IllegalArgumentException("Channel can't be longer than 16 characters!");
		}

		Map<Object, ChannelListener> map = this.registeredListeners.get(channel);
		if (map==null) {
			map = new ConcurrentHashMap<Object, ChannelListener>();
			this.registeredListeners.put(channel, map);
		}

		map.put(pluginInstance, channelListener);
		this.generateRegisteredChannelsSQLList();
	}

	/**
	 * Unregister the specified channel listener for the specified plugin instance.
	 * @param pluginInstance the plugin instance
	 * @param channel the channel
	 * @return true if the registered channel was registered, false otherwise.
	 */
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

	/**
	 * Unregister all registered listeners for the specified plugin
	 * @param pluginInstance the plugin instance
	 */
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

	protected void generateRegisteredChannelsSQLList() {
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
	 * Initialize SynX implementation. Unless you know what to do with this, ignore it.
	 * @param implementation the SynX implementation
	 * @return the SynX instance
	 * @throws Exception if something messed up
	 */
	public static SynX initialize(ISynX implementation) throws Exception {
		SynX inst = new SynX();
		inst.implementation = implementation;
		inst.nodes.clear();
		inst.nodesById.clear();
		inst.tags.clear();

		inst.config = implementation.loadConfig();

		inst.dataExchanger = new DataExchanger(inst);
		inst.dataExchanger.init();
		
		instance = inst;
		
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
		
		return inst;
	}
	
	public void start() {
		this.dataExchanger.start();
	}

	public void shutdown() {
		this.dataExchanger.shutdown();
	}
	
	/**
	 * Called by the SynX implementation to deinitialize SynX, usually on server stop or implementation disabling.
	 */
	public void deinitialize() {
		this.shutdown();
		this.implementation = null;
		instance = null;
	}

	@Override
	public String toString() {
		return "SynX";
	}

	public ISynX getImplementation() {
		return implementation;
	}
}
