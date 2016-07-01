package net.kaikk.mc.synx.bukkit;

import javax.sql.DataSource;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;

public class SynXBukkit extends JavaPlugin implements ISynX {
	private static SynX synx;
	
	@Override
	public void onEnable() {
		try {
			synx = SynX.initialize(this);
			
			this.getServer().getPluginManager().registerEvents(new EventListener(synx), this);
			this.getCommand(this.getName()).setExecutor(new CommandExec(this));
			
			new BukkitRunnable() {
				@Override
				public void run() {
					synx.startDataExchangerThread();
				}
			}.runTask(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onDisable() {
		synx.stopDataExchangerThread();
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
	public DataSource getDataSource(String hostname, String username, String password, String database) {
		try {
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			this.log("ERROR: Unable to load Java's MySQL database driver. Check to make sure you've installed it properly.");
			throw new RuntimeException(e);
		}
		
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setURL(hostname);
		dataSource.setUser(username);
		dataSource.setPassword(password);
		dataSource.setDatabaseName(database);
		return dataSource;
	}
}
