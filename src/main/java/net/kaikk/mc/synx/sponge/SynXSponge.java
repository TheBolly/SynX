package net.kaikk.mc.synx.sponge;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="synx", name="SynX", version="1.0", description = "SynX")
public class SynXSponge implements ISynX {
	private SynX synx;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	
	@Listener
	public void onServerStart(GameStartingServerEvent event) throws Exception {
		this.enable();
		CommandSpec commandSpec = CommandSpec.builder()
			    .description(Text.of("SynX Main Command"))
			    .permission("synx.manage")
			    .arguments(SpongeUtils.buildChoices("command", "reload", "nodes", "tags"))
			    .executor(new MainCommand(this))
			    .build();

		Sponge.getCommandManager().register(this, commandSpec, "synx");
	}

	@Listener
	public void onServerStop(GameStoppingEvent event) {
		this.disable();
	}
	
	@Listener
	public void onServerReload(GameReloadEvent event) throws Exception {
		this.reload();
	}
	
	void enable() throws Exception {
		this.synx = SynX.initialize(this);
		
		// start data exchanger at first tick
		Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
		    public void run() {
		    	synx.startDataExchangerThread();
		    }
		}).delayTicks(1).submit(this);
	}
	
	void disable() {
		this.synx.deinitialize();
	}
	
	void reload() throws Exception {
		disable();
		enable();
	}

	@Override
	public void log(String message) {
		System.out.println("[SynX] "+message);
	}

	@Override
	public Config loadConfig() throws Exception {
		return new ConfigSponge(this);
	}

	@Override
	public DataSource getDataSource(String hostname, String username, String password, String database) throws SQLException {
		return Sponge.getServiceManager().provide(SqlService.class).get().getDataSource("jdbc:mysql://"+username+(password.isEmpty() ? "" : ":"+password)+"@"+hostname+"/"+database);
	}
	
	public ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
		return configManager;
	}
}
