package net.kaikk.mc.synx.sponge;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.synx.Config;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ConfigSponge extends Config {
	
	
	ConfigSponge(ConfigurationLoader<CommentedConfigurationNode> configManager) throws Exception {
		ConfigurationNode rootNode = configManager.load();
		
		ConfigurationNode mySql = rootNode.getNode("MySQL");
		this.dbHostname = mySql.getNode("Hostname").getString("localhost");
		this.dbUsername = mySql.getNode("Username").getString("username");
		this.dbPassword = mySql.getNode("Password").getString("password");
		this.dbDatabase = mySql.getNode("Database").getString("database");
		
		this.nodeName = rootNode.getNode("NodeName").getString("undefined");
		this.waitTime = rootNode.getNode("DataExchangerThrottleTime").getInt(500);
		this.defaultTTL = rootNode.getNode("DefaultTTL").getLong(86400000);
		
		this.debug = rootNode.getNode("Debug").getBoolean(false);
		
		this.tags = rootNode.getNode("Tags").getList(TypeToken.of(String.class), new ArrayList<String>(Arrays.asList(new String[]{"default"})));
	}
}
