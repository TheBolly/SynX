package net.kaikk.mc.synx.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import net.kaikk.mc.synx.SynX;

public class EventListener implements Listener {
	protected SynX synx;

	protected EventListener(SynX synx) {
		this.synx = synx;
	}

	@EventHandler
	protected void onPluginDisable(PluginDisableEvent event) {
		synx.unregisterAll(event.getPlugin());
	}
}
