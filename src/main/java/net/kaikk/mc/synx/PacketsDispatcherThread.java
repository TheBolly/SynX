package net.kaikk.mc.synx;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.kaikk.mc.synx.packets.ChannelListener;
import net.kaikk.mc.synx.packets.Packet;

class PacketsDispatcherThread extends Thread {
	BlockingQueue<Packet> dispatchQueue = new LinkedBlockingQueue<Packet>();
	private SynX instance;
	
	PacketsDispatcherThread(SynX instance) {
		super("SynXReceivedPacketsDispatcherThread");
		this.instance = instance;
	}
	
	@Override
	public void run() {
		instance.debug("ReceivedPacketsDispatcherThread: started");
		Packet packet;
		for(;;) {
			try {
				packet = dispatchQueue.take();
				
				Map<?, ChannelListener> map = instance.registeredListeners.get(packet.getChannel());
				if (map==null) {
					return;
				}
				
				for (Entry<?, ChannelListener> entry : map.entrySet()) {
					try {
						instance.debug("Dispatching ", packet, " to ", entry.getKey());
						entry.getValue().onPacketReceived(packet);
					} catch (Throwable e) {
						instance.log("An exception has been thrown by "+entry.getKey()+" while executing onPacketReceived.");
						e.printStackTrace();
					}
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		instance.debug("ReceivedPacketsDispatcherThread: stopped");
	}
}
