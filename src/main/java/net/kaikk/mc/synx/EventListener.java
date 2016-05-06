package net.kaikk.mc.synx;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

class EventListener implements Listener {
	private SynX instance;
	
	EventListener(SynX instance) {
		this.instance = instance;
	}
	
	@EventHandler
	void onPluginDisable(PluginDisableEvent event) {
		instance.unregisterAll(event.getPlugin());
	}
}
