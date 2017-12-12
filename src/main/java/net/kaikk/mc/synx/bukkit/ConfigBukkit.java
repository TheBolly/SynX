package net.kaikk.mc.synx.bukkit;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.kaikk.mc.kaiscommons.bukkit.CommonBukkitUtils;
import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.SynXUtils;

public class ConfigBukkit extends Config {
	protected ConfigBukkit(SynXBukkit instance) {
		CommonBukkitUtils.copyAsset(instance, "config.yml");
		instance.reloadConfig();

		this.nodeName=instance.getConfig().getString("NodeName", "");

		if (this.nodeName.isEmpty() || this.nodeName.equals("undefined")) {
			throw new IllegalArgumentException("Undefined node name. Please check your config file.");
		}

		if (this.nodeName.length()>16) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is longer than 16 characters. Please check your config file.");
		}

		if (!SynXUtils.isAlphanumeric(this.nodeName)) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is not alphanumeric. Please check your config file.");
		}

		if (this.nodeName.equals("all") || this.nodeName.equals("UNKNOWN_NODE")) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is a reserved word for the node name.");
		}

		this.dbHostname=instance.getConfig().getString("MySQL.Hostname");
		this.dbUsername=instance.getConfig().getString("MySQL.Username");
		this.dbPassword=instance.getConfig().getString("MySQL.Password");
		this.dbDatabase=instance.getConfig().getString("MySQL.Database");

		this.interval=instance.getConfig().getInt("DataExchangerThrottleTime", 200);
		if (this.interval<50) {
			this.interval=50;
		}

		this.defaultTTL=instance.getConfig().getInt("DefaultTTL", 86400000);

		this.tags=instance.getConfig().getStringList("Tags");

		this.debug=instance.getConfig().getBoolean("Debug");
	}

	@Override
	public DataSource getDataSource() throws SQLException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			throw new RuntimeException("ERROR: Unable to load Java's MySQL database driver. Check to make sure you've installed it properly.");
		}

		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setURL("jdbc:mysql://"+dbHostname+"/"+dbDatabase);
		dataSource.setUser(dbUsername);
		dataSource.setPassword(dbPassword);
		dataSource.setDatabaseName(dbDatabase);
		return dataSource;
	}
}
