package net.kaikk.mc.synx.packets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class Packet {
	private final Node from;
	private final String channel;
	private final byte[] data;
	private final long timeOfDeath;
	
	public Packet(Node from, String channel, byte[] data, long timeOfDeath) {
		this.from = from;
		this.channel = channel;
		this.data = data;
		this.timeOfDeath = timeOfDeath;
	}

	public String getChannel() {
		return channel;
	}

	public byte[] getData() {
		return data;
	}
	
	public ByteArrayDataInput getDataInputStream() {
		return ByteStreams.newDataInput(this.data);
	}
	
	public Node getFrom() {
		return from;
	}

	public long getTimeOfDeath() {
		return timeOfDeath;
	}

	@Override
	public String toString() {
		return "Packet [from=" + from + ", channel=" + channel + ", data.length="+data.length+"]";
	}
}
