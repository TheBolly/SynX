package net.kaikk.mc.synx.bukkit;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.Utils;

class ConfigBukkit extends Config {
	ConfigBukkit(SynXBukkit instance) {
		instance.saveDefaultConfig();
		instance.reloadConfig();
		
		this.nodeName=instance.getConfig().getString("NodeName", "");
		
		if (this.nodeName.isEmpty() || this.nodeName.equals("undefined")) {
			throw new IllegalArgumentException("Undefined node name. Please check your config file.");
		}
		
		if (this.nodeName.length()>16) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is longer than 16 characters. Please check your config file.");
		}

		if (!Utils.isAlphanumeric(this.nodeName)) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is not alphanumeric. Please check your config file.");
		}
		
		if (this.nodeName.equals("all") || this.nodeName.equals("UNKNOWN_NODE")) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is a reserved word for the node name.");
		}
		
		this.dbHostname=instance.getConfig().getString("MySQL.Hostname");
		this.dbUsername=instance.getConfig().getString("MySQL.Username");
		this.dbPassword=instance.getConfig().getString("MySQL.Password");
		this.dbDatabase=instance.getConfig().getString("MySQL.Database");
		
		this.waitTime=instance.getConfig().getInt("DataExchangerThrottleTime", 200);
		if (this.waitTime<200) {
			this.waitTime=200;
		}
		
		this.defaultTTL=instance.getConfig().getInt("DefaultTTL", 86400000);
		
		this.tags=instance.getConfig().getStringList("Tags");
		
		this.debug=instance.getConfig().getBoolean("Debug");
	}
}
