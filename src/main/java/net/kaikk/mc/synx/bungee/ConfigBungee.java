package net.kaikk.mc.synx.bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.SynXUtils;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class ConfigBungee extends Config {
	protected ConfigBungee(SynXBungee instance) throws IOException {
		instance.getDataFolder().mkdirs();
		File configFile = new File(instance.getDataFolder(), "config.yml");
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

		if (!SynXUtils.isAlphanumeric(this.nodeName)) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is not alphanumeric. Please check your config file.");
		}

		if (this.nodeName.equals("all") || this.nodeName.equals("UNKNOWN_NODE")) {
			throw new IllegalArgumentException("Defined node name ("+this.nodeName+") is a reserved word for the node name.");
		}

		this.dbHostname=config.getString("MySQL.Hostname");
		this.dbUsername=config.getString("MySQL.Username");
		this.dbPassword=config.getString("MySQL.Password");
		this.dbDatabase=config.getString("MySQL.Database");

		this.interval=config.getInt("DataExchangerThrottleTime", 200);
		if (this.interval<50) {
			this.interval=50;
		}

		this.defaultTTL=config.getInt("DefaultTTL", 86400000);

		this.tags=config.getStringList("Tags");

		this.debug=config.getBoolean("Debug");
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
