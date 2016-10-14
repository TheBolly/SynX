package net.kaikk.mc.synx.sponge;

import java.net.URL;

import org.spongepowered.api.Sponge;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.synx.Config;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

public class ConfigSponge extends Config {
	ConfigSponge(SynXSponge instance) throws Exception {
		//load defaults
		URL defaultsInJarURL = new URL("jar:file:/"+Sponge.getPluginManager().fromInstance(instance).get().getSource().get()+"!/config.conf");
		HoconConfigurationLoader defaultsLoader = HoconConfigurationLoader.builder().setURL(defaultsInJarURL).build();
		ConfigurationNode defaults = defaultsLoader.load();

		//load config & merge defaults
		ConfigurationNode rootNode = instance.getConfigManager().load();
		rootNode.mergeValuesFrom(defaults);
		instance.getConfigManager().save(rootNode);
		
		ConfigurationNode mySql = rootNode.getNode("MySQL");
		this.dbHostname = mySql.getNode("Hostname").getString();
		this.dbUsername = mySql.getNode("Username").getString();
		this.dbPassword = mySql.getNode("Password").getString();
		this.dbDatabase = mySql.getNode("Database").getString();
		
		this.nodeName = rootNode.getNode("NodeName").getString();
		this.waitTime = rootNode.getNode("DataExchangerThrottleTime").getInt();
		this.defaultTTL = rootNode.getNode("DefaultTTL").getLong();
		
		this.debug = rootNode.getNode("Debug").getBoolean();
		
		this.tags = rootNode.getNode("Tags").getList(TypeToken.of(String.class));
	}
}
