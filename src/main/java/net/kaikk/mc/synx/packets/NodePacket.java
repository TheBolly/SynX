package net.kaikk.mc.synx.packets;

public final class NodePacket extends Packet {
	private final Node[] nodes;
	
	public NodePacket(Node from, String channel, byte[] data, long timeOfDeath, Node... nodes) {
		super(from, channel, data, timeOfDeath);
		this.nodes = nodes;
	}

	public Node[] getNodes() {
		return nodes;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Node n : nodes) {
			sb.append(n);
			sb.append(',');
		}
		int l = sb.length();
		if (l>0) {
			sb.setLength(l-1);
		}
		
		return "NodePacket [from=" + this.getFrom() + ", channel=" + this.getChannel() + ", data.length=" + this.getData().length + ", nodes=["+sb+"]]";
	}
}
