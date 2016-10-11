package net.kaikk.mc.synx.bungee;

import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;
import net.md_5.bungee.api.plugin.Plugin;

public class SynXBungee extends Plugin implements ISynX {
	private static SynX synx;
	
	@Override
	public void onEnable() {
		try {
			synx = SynX.initialize(this);
			
			new Thread() {
				public void run() {
					try {
						getLogger().info("SynX will wait 5 seconds before starting the data exchange thread...");
						sleep(5000);
						synx.startDataExchangerThread();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				};
			}.start();
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
		return new ConfigBungee(this);
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
		dataSource.setURL("jdbc:mysql://"+hostname+"/"+database);
		dataSource.setUser(username);
		dataSource.setPassword(password);
		dataSource.setDatabaseName(database);
		return dataSource;
	}
}
