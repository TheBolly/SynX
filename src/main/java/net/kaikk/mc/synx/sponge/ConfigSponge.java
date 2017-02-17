package net.kaikk.mc.synx.sponge;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.service.sql.SqlService;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.synx.Config;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class ConfigSponge extends Config {
	protected ConfigSponge(SynXSponge instance) throws Exception {
		//load defaults
		Asset asset = Sponge.getAssetManager().getAsset(instance, "config.yml").get();
		YAMLConfigurationLoader defaultsLoader = YAMLConfigurationLoader.builder().setURL(asset.getUrl()).build();
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
		this.interval = rootNode.getNode("DataExchangerThrottleTime").getInt();
		if (this.interval<50) {
			this.interval=50;
		}
		this.defaultTTL = rootNode.getNode("DefaultTTL").getLong();

		this.debug = rootNode.getNode("Debug").getBoolean();

		this.tags = rootNode.getNode("Tags").getList(TypeToken.of(String.class));
	}

	@Override
	public DataSource getDataSource() throws SQLException {
		return Sponge.getServiceManager().provide(SqlService.class).get().getDataSource("jdbc:mysql://"+dbUsername+(dbPassword.isEmpty() ? "" : ":"+dbPassword)+"@"+dbHostname+"/"+dbDatabase);
	}
}
