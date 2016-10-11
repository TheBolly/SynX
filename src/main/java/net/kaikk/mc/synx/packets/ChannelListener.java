package net.kaikk.mc.synx.packets;

public interface ChannelListener {
	/** 
	 * This method will be called every time some data is received on this channel.<br>
	 * This method is called <b>asynchronously from the minecraft server thread</b>. Avoid accessing the minecraft server thread without synchronization.<br>
	 * This method will block any other received packet until it returns. Avoid blocking operations.
	 * @param packet The packet received from another server
	 * */

	abstract void onPacketReceived(Packet packet);
}
