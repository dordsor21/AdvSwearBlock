/*
 * AdvSwearBlock is designed to streamline and simplify your mountain building experience.
 * Copyright (C) dordsor21 team and contributores
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.dordsor21.AdvSwearBlock.cmd;

import me.dordsor21.AdvSwearBlock.AdvSwearBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class SwearWordCmd implements CommandExecutor {
    //lots of parsing player metadata to toggle/turn on/off the swear-block

    private final AdvSwearBlock plugin;

    public SwearWordCmd(AdvSwearBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String strng, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.messages.get("notPlayer"));
            return true;
        }
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
        return true;
    }

}
