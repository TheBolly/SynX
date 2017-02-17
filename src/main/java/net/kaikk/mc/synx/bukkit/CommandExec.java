package net.kaikk.mc.synx.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.kaikk.mc.synx.SynX;
import net.kaikk.mc.synx.packets.Node;

public class CommandExec implements CommandExecutor {
	protected SynXBukkit instance;

	protected CommandExec(SynXBukkit instance) {
		this.instance = instance;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("synx")) {
			if (!sender.hasPermission(instance.getName()+".manage")) {
				return false;
			}

			if (args.length==0) {
				sender.sendMessage(ChatColor.RED+"Usage: /"+label+" [reload|nodes|tags]");
				return false;
			}

			switch(args[0].toLowerCase()) {
			case "reload": {
				Bukkit.getPluginManager().disablePlugin(instance);
				Bukkit.getPluginManager().enablePlugin(instance);
				sender.sendMessage("["+instance.getName()+"] Plugin reloaded.");
				return true;
			}
			case "nodes": {
				if (args.length==2 && args[1].toLowerCase().equals("details")) {
					sender.sendMessage(ChatColor.RED+"[SynX] Detailed nodes list:");
					for (Node node : SynX.instance().getNodes().values()) {
						sender.sendMessage(ChatColor.AQUA+""+node.getId()+":"+node.getName()+" [tags: "+String.join(", ", node.getTags())+"]");
					}
				} else {
					sender.sendMessage(ChatColor.RED+"[SynX] Nodes list: "+ChatColor.AQUA+String.join(", ", SynX.instance().getNodes().keySet()));
				}
				return true;
			}
			case "tags": {
				sender.sendMessage(ChatColor.RED+"[SynX] Tags list: "+ChatColor.AQUA+String.join(", ", SynX.instance().getTags()));
				return true;
			}
			default:
				sender.sendMessage(ChatColor.RED+"Wrong parameter "+args[0]);
				break;
			}
		}
		return false;
	}
}
