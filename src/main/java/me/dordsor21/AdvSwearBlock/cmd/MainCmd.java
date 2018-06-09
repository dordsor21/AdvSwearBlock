package me.dordsor21.AdvSwearBlock.cmd;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MainCmd implements CommandExecutor {

    private Main plugin;

    public MainCmd(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String strng, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.messages.get("asbUsage"));
            return true;
        }
        if (sender.hasPermission("asb.admin"))
            reload(sender, args[1]);
        else
            sender.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.admin"));
        return true;
    }

    private void reload(CommandSender sender, String component) {
        switch (component.toLowerCase()) {
            case "swearlist":
                plugin.reloadSwearList();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            case "noswearlist":
                plugin.reloadNoSwearList();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            case"messages":
                plugin.reloadMessages();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            case "all":
                plugin.reloadSwearList();
                plugin.reloadNoSwearList();
                plugin.reloadMessages();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            default:
                sender.sendMessage(plugin.messages.get("asbUsage"));
        }
    }
}
