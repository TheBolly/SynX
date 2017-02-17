package net.kaikk.mc.synx.packets;

import java.io.Serializable;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.kaikk.mc.synx.SynXUtils;

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
	
	public Object getObject() {
		return SynXUtils.convertFromBytes(data);
	}
	
	public <T extends Serializable> T getObject(Class<T> clazz) {
		return SynXUtils.convertFromBytes(clazz, data);
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
		return "Packet [from=" + from + ", channel=" + channel + ", data.length=" + data.length+"]";
	}
}
