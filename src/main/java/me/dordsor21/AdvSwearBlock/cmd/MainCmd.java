/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.cmd;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class MainCmd implements CommandExecutor {

    private Main plugin;

    public MainCmd(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String strng, String[] args) {
        if (!sender.hasPermission("asb.admin")) {
            sender.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.admin"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.messages.get("asbReloadUsage"));
            sender.sendMessage(plugin.messages.get("asbSwearUsage"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
            case "r":
                reload(sender, args);
                break;
            case "add":
                if (args.length > 2) {
                    if (!Arrays.asList(new String[]{"m", "nom", "multiplier", "nomultiplier"}).contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    plugin.swearList.add(sender, Arrays.copyOfRange(args, 2, args.length), args[1]);

                } else
                    sender.sendMessage(plugin.messages.get("asbAddUsage"));
                break;
            case "remove":
                if (args.length > 2) {
                    if (!Arrays.asList(new String[]{"m", "nom", "multiplier", "nomultiplier"}).contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    plugin.swearList.remove(sender, Arrays.copyOfRange(args, 2, args.length), args[1]);
                } else
                    sender.sendMessage(plugin.messages.get("asbRemoveUsage"));
                break;
            case "list":
                if (args.length > 1) {
                    if (!Arrays.asList(new String[]{"m", "nom", "multiplier", "nomultiplier"}).contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    if (args.length > 2) {
                        if (args[1].matches("^\\d+$"))
                            plugin.swearList.list(sender, Integer.valueOf(args[2]), args[1]);
                        else
                            sender.sendMessage(plugin.messages.get("notInteger"));
                        break;
                    }
                    plugin.swearList.list(sender, 1, args[1]);
                    break;
                }
                sender.sendMessage(plugin.messages.get("asbListUsage"));
                break;
            case "refresh":
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.hasPermission("asb.noignore") && !plugin.ignore.cannotIgnore.contains(p.getName().toLowerCase())) {
                        plugin.ignore.cannotIgnore.add(p.getName().toLowerCase());
                        plugin.sql.setCannotIgnore(p.getUniqueId(), true);
                        continue;
                    }
                    if (!p.hasPermission("asb.noignore") && plugin.ignore.cannotIgnore.contains(p.getName().toLowerCase())) {
                        plugin.ignore.cannotIgnore.remove(p.getName().toLowerCase());
                        plugin.sql.setCannotIgnore(p.getUniqueId(), false);
                    }
                }
                break;
            default:
                sender.sendMessage(plugin.messages.get("asbReloadUsage"));
                sender.sendMessage(plugin.messages.get("asbSwearUsage"));
        }
        return true;
    }

    private void reload(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.reloadSwearList();
            plugin.reloadNoSwearList();
            plugin.reloadMessages();
            sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", "all"));
            return;
        }
        String component = args[1];
        switch (component.toLowerCase()) {
            case "swearlist":
                plugin.reloadSwearList();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            case "noswearlist":
                plugin.reloadNoSwearList();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                break;
            case "messages":
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
                plugin.reloadSwearList();
                plugin.reloadNoSwearList();
                plugin.reloadMessages();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", "all"));
        }
    }
}
