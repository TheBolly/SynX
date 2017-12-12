package net.kaikk.mc.synx.bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.DataExchanger;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;

public class SynXBukkit extends JavaPlugin implements ISynX {
    protected static SynX synx;
    protected Map<DataExchanger,BukkitTask[]> tasks = new ConcurrentHashMap<>(1);

    @Override
    public void onEnable() {
        try {
            synx = SynX.initialize(this);

            this.getServer().getPluginManager().registerEvents(new EventListener(synx), this);
            this.getCommand("synx").setExecutor(new CommandExec(this));
            
            new BukkitRunnable() {
				@Override
				public void run() {
					synx.start();
				}
			}.runTaskLater(this, 1L);
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
        return new ConfigBukkit(this);
    }
}
