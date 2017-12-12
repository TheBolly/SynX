package net.kaikk.mc.synx.bungee;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.DataExchanger;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class SynXBungee extends Plugin implements ISynX {
	protected static SynX synx;
	protected Map<DataExchanger,ScheduledTask[]> tasks = new ConcurrentHashMap<>(1);

	@Override
	public void onEnable() {
		try {
			synx = SynX.initialize(this);
			this.getProxy().getScheduler().schedule(this, () -> {
				synx.start();
			}, 5L, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDisable() {
		synx.deinitialize();
		synx = null;
	}

	@Override
	public void log(String message) {
		this.getLogger().info(message);
	}

	@Override
	public Config loadConfig() throws Exception {
		return new ConfigBungee(this);
	}
}

