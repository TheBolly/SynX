package net.kaikk.mc.synx.bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.Utils;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

class ConfigBungee extends Config {
	ConfigBungee(SynXBungee instance) throws IOException {
		File configFile = new File(instance.getDataFolder(), "config.yml");
		configFile.mkdirs();
		if (!configFile.exists()) {
            try (InputStream in = instance.getResourceAsStream("assets/synx/config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		
		Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		
		this.nodeName=config.getString("NodeName", "");
		
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
		
		this.dbHostname=config.getString("MySQL.Hostname");
		this.dbUsername=config.getString("MySQL.Username");
		this.dbPassword=config.getString("MySQL.Password");
		this.dbDatabase=config.getString("MySQL.Database");
		
		this.waitTime=config.getInt("DataExchangerThrottleTime", 200);
		if (this.waitTime<200) {
			this.waitTime=200;
		}
		
		this.defaultTTL=config.getInt("DefaultTTL", 86400000);
		
		this.tags=config.getStringList("Tags");
		
		this.debug=config.getBoolean("Debug");
	}
}
