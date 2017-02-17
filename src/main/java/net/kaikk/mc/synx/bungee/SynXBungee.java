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
	
	@Override
	public void startExchanger(DataExchanger exchanger, int interval) {
		ScheduledTask[] t = new ScheduledTask[4];
		t[0] = this.getProxy().getScheduler().schedule(this, exchanger.getMaintenance(), 0L, 1L, TimeUnit.HOURS);
		t[1] = this.getProxy().getScheduler().schedule(this, exchanger.getDispatcher(), 0L, interval, TimeUnit.MILLISECONDS);
		t[2] = this.getProxy().getScheduler().schedule(this, exchanger.getSender(), 0L, interval, TimeUnit.MILLISECONDS);
		t[3] = this.getProxy().getScheduler().schedule(this, exchanger.getReceiver(), 5000L, interval, TimeUnit.MILLISECONDS);
		tasks.put(exchanger, t);
	}

	@Override
	public void stopExchanger(DataExchanger exchanger) {
		ScheduledTask[] t = tasks.remove(exchanger);
        if (t != null) {
            for (ScheduledTask task : t) {
            	task.cancel();
            }
        }
	}
}

