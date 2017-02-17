package net.kaikk.mc.synx.bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
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

    @Override
    public void startExchanger(DataExchanger exchanger, int interval) {
        BukkitTask[] t = new BukkitTask[4];
        t[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, exchanger.getMaintenance(), 0, 3600*20);
        t[1] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, exchanger.getDispatcher(), 0, interval/50);
        t[2] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, exchanger.getSender(), 0, interval/50);
        t[3] = Bukkit.getScheduler().runTaskTimerAsynchronously(this, exchanger.getReceiver(), 1, interval/50);
        this.tasks.put(exchanger, t);
    }

    @Override
    public void stopExchanger(DataExchanger exchanger) {
        BukkitTask[] t = tasks.remove(exchanger);
        if (t != null) {
            for (BukkitTask task : t) {
            	task.cancel();
            }
        }
    }
}
