package net.kaikk.mc.synx.sponge;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import net.kaikk.mc.synx.Config;
import net.kaikk.mc.synx.DataExchanger;
import net.kaikk.mc.synx.ISynX;
import net.kaikk.mc.synx.SynX;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id=PluginInfo.id, name = PluginInfo.name, version = PluginInfo.version, description = PluginInfo.description)
public class SynXSponge implements ISynX {
    private SynX synx;
    private Map<DataExchanger,Task[]> tasks = new ConcurrentHashMap<>(1);

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Listener
    public void onServerStart(GamePreInitializationEvent event) throws Exception {
        this.enable();
    }

    @Listener
    public void onServerStart(GameInitializationEvent event) throws Exception {
        CommandSpec commandSpec = CommandSpec.builder()
                .description(Text.of("SynX Main Command"))
                .permission("synx.manage")
                .arguments(SpongeUtils.buildChoices("command", "reload", "nodes", "tags"))
                .executor(new MainCommand(this))
                .build();

        Sponge.getCommandManager().register(this, commandSpec, "synx");
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        this.disable();
    }

    @Listener
    public void onServerReload(GameReloadEvent event) throws Exception {
        this.reload();
    }

    void enable() throws Exception {
        this.synx = SynX.initialize(this);
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
    
    public ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
        return configManager;
    }

    @Override
    public void startExchanger(DataExchanger exchanger, int interval) {
        Task[] t = new Task[4];
        t[0] = Sponge.getScheduler().createTaskBuilder().execute(exchanger.getMaintenance()).async().interval(1L, TimeUnit.HOURS).submit(this);
        t[1] = Sponge.getScheduler().createTaskBuilder().execute(exchanger.getDispatcher()).async().interval(interval, TimeUnit.MILLISECONDS).submit(this);
        t[2] = Sponge.getScheduler().createTaskBuilder().execute(exchanger.getSender()).async().interval(interval, TimeUnit.MILLISECONDS).submit(this);
        Sponge.getScheduler().createTaskBuilder().execute(() -> {
            t[3] = Sponge.getScheduler().createTaskBuilder().execute(exchanger.getReceiver()).async().interval(interval, TimeUnit.MILLISECONDS).submit(this);
            tasks.put(exchanger, t);
        }).delayTicks(1L).submit(this);
    }

    @Override
    public void stopExchanger(DataExchanger exchanger) {
        Task[] t = tasks.remove(exchanger);
        if (t != null) {
            for (Task task : t) {
                task.cancel();
            }
        }
    }
}
