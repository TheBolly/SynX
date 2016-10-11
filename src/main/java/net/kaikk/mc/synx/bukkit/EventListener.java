package net.kaikk.mc.synx.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import net.kaikk.mc.synx.SynX;

class EventListener implements Listener {
	private SynX synx;
	
	EventListener(SynX synx) {
		this.synx = synx;
	}
	
	@EventHandler
	void onPluginDisable(PluginDisableEvent event) {
		synx.unregisterAll(event.getPlugin());
	}
}
