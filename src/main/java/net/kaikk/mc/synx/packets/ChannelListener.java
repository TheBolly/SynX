package net.kaikk.mc.synx.packets;

public interface ChannelListener {
	/** 
	 * This method will be called every time some data is received on this channel.<br>
	 * This method is called <b>asynchronously</b>. Avoid accessing Bukkit/Sponge API without prior synchronization (see Bukkit/Sponge Scheduler).<br>
	 * This method will block any other received packet until it returns, so make sure your implementation of onPacketReceived doesn't block the thread. If you have to do blocking operations, then start a new thread (see Bukkit/Bungee/Sponge Scheduler)
	 * @param packet The packet received from another server
	 * */

	abstract void onPacketReceived(final Packet packet);
}
