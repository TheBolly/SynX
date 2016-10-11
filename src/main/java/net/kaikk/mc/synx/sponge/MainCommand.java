package net.kaikk.mc.synx.sponge;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.kaikk.mc.synx.SynX;

public class MainCommand implements CommandExecutor {
	private SynXSponge instance;
	
	MainCommand(SynXSponge instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
		String param = args.<String>getOne("command").get();
		switch(param.toLowerCase()) {
		case "reload": {
			try {
				instance.reload();
				sender.sendMessage(Text.of("[SynX] Plugin reloaded."));
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage(Text.of(TextColors.RED, "[SynX] An error occurred while reloading the plugin! Error: "+e.getMessage()));
			}
			break;
		}
		case "nodes": {
			sender.sendMessage(Text.of(TextColors.RED, "[SynX] Nodes list: ", TextColors.AQUA, String.join(", ", SynX.instance().getNodes().keySet())));
			return CommandResult.empty();
		}
		case "tags": {
			sender.sendMessage(Text.of(TextColors.RED, "[SynX] Tags list: ", TextColors.AQUA, String.join(", ", SynX.instance().getTags())));
			return CommandResult.empty();
		}
		}
		return CommandResult.empty();
	}
}
