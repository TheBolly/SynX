package net.kaikk.mc.synx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.kaikk.mc.synx.packets.Node;

public class NodeArrayBuilder {
	private Set<Node> nodes;
	
	private NodeArrayBuilder() {
		this.nodes = new HashSet<Node>();
	}
	
	public static NodeArrayBuilder builder() {
		return new NodeArrayBuilder();
	}
	
	public NodeArrayBuilder all() {
		this.nodes.addAll(SynX.instance().getNodes().values());
		return this;
	}
	
	public NodeArrayBuilder add(String... nodesId) {
		if (nodesId.length==1) {
			this.add(SynX.instance().getNode(nodesId[0]));
		} else {
			List<Node> l = new ArrayList<Node>();
			for (String nodeId : nodesId) {
				l.add(SynX.instance().getNode(nodeId));
			}
			this.add(l);
		}
		return this;
	}
	
	public NodeArrayBuilder addTags(String... tags) {
		for (String tag : tags) {
			Set<Node> nodes = SynX.instance().getTagNodes(tag);
			if (nodes != null) {
				this.add(nodes);
			}
		}
		return this;
	}
	
	public NodeArrayBuilder add(Node... nodes) {
		for (Node node : nodes) {
			this.nodes.add(node);
		}
		return this;
	}
	
	public NodeArrayBuilder add(Collection<Node> nodes) {
		this.nodes.addAll(nodes);
		return this;
	}
	
	public NodeArrayBuilder remove(Node... nodes) {
		for (Node node : nodes) {
			this.nodes.remove(node);
		}
		return this;
	}
	
	public NodeArrayBuilder remove(Collection<Node> nodes) {
		this.nodes.removeAll(nodes);
		return this;
	}
	
	public NodeArrayBuilder remove(String... nodesId) {
		if (nodesId.length==1) {
			this.remove(SynX.instance().getNode(nodesId[0]));
		} else {
			List<Node> l = new ArrayList<Node>();
			for (String nodeId : nodesId) {
				l.add(SynX.instance().getNode(nodeId));
			}
			this.remove(l);
		}
		return this;
	}
	
	public NodeArrayBuilder removeTags(String... tags) {
		for (String tag : tags) {
			Set<Node> nodes = SynX.instance().getTagNodes(tag);
			if (nodes != null) {
				this.remove(nodes);
			}
		}
		return this;
	}
	
	public NodeArrayBuilder removeCurrentNode() {
		this.nodes.remove(SynX.instance().getNode());
		return this;
	}
	
	public NodeArrayBuilder addCurrentNode() {
		this.nodes.add(SynX.instance().getNode());
		return this;
	}
	
	public Node[] build() {
		return nodes.toArray(new Node[this.nodes.size()]);
	}
}
