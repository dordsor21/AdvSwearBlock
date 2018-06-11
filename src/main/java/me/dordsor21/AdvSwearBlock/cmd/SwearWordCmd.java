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
import org.bukkit.metadata.FixedMetadataValue;

public class SwearWordCmd implements CommandExecutor {
    //lots of parsing player metadata to toggle/turn on/off the swear-block

    private Main plugin;

    public SwearWordCmd(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String strng, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length < 1 && p.hasPermission("asb.swear.toggle")) {
                if (p.hasMetadata("swearBlock")) {
                    p.removeMetadata("swearBlock", plugin);
                    p.sendMessage(plugin.messages.get("swearBlockOff"));
                } else {
                    p.setMetadata("swearBlock", new FixedMetadataValue(plugin, true));
                    p.sendMessage(plugin.messages.get("swearBlockOn"));
                }
            } else if (p.hasPermission("asb.swear")) {
                if (args[0].equalsIgnoreCase("on")) {
                    if (!p.hasMetadata("swearBlock")) {
                        p.setMetadata("swearBlock", new FixedMetadataValue(plugin, true));
                    }
                    p.sendMessage(plugin.messages.get("swearBlockOn"));
                } else if (args[0].equalsIgnoreCase("off")) {
                    if (p.hasMetadata("swearBlock")) {
                        p.removeMetadata("swearBlock", plugin);
                    }
                    p.sendMessage(plugin.messages.get("swearBlockOff"));
                } else {
                    p.sendMessage(plugin.messages.get("swearBlockUsage"));
                }
            } else {
                p.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.swear.toggle"));
            }
        } else
            sender.sendMessage(plugin.messages.get("notPlayer"));
        return true;
    }

}
