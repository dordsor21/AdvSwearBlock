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

import java.util.Arrays;

public class MainCmd implements CommandExecutor {

    private final AdvSwearBlock plugin;

    public MainCmd(AdvSwearBlock plugin) {
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
                    if (!Arrays.asList(new String[] {"m", "nom", "o", "multiplier", "nomultiplier", "onlymatch"})
                        .contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    plugin.swearList.add(sender, Arrays.copyOfRange(args, 2, args.length), args[1]);

                } else {
                    sender.sendMessage(plugin.messages.get("asbAddUsage"));
                }
                break;
            case "remove":
                if (args.length > 2) {
                    if (!Arrays.asList(new String[] {"m", "nom", "multiplier", "nomultiplier"})
                        .contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    plugin.swearList.remove(sender, Arrays.copyOfRange(args, 2, args.length), args[1]);
                } else {
                    sender.sendMessage(plugin.messages.get("asbRemoveUsage"));
                }
                break;
            case "list":
                if (args.length > 1) {
                    if (!Arrays.asList(new String[] {"m", "nom", "o", "multiplier", "nomultiplier", "onlymatch"})
                        .contains(args[1].toLowerCase())) {
                        sender.sendMessage(plugin.messages.get("asbListUsage"));
                        break;
                    }
                    if (args.length > 2) {
                        if (args[1].matches("^\\d+$")) {
                            plugin.swearList.list(sender, Integer.parseInt(args[2]), args[1]);
                        } else {
                            sender.sendMessage(plugin.messages.get("notInteger"));
                        }
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

    private void reload(final CommandSender sender, String[] args) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (args.length == 1) {
                plugin.reloadConfigValues();
                plugin.reloadPersistance();
                plugin.reloadSwearList();
                plugin.reloadNoSwearList();
                plugin.reloadMessages();
                plugin.reloadIgnore();
                sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", "all"));
                return;
            }
            String component = args[1];
            switch (component.toLowerCase()) {
                case "swearlist":
                    plugin.reloadPersistance();
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
                case "ignore":
                    plugin.reloadPersistance();
                    plugin.reloadIgnore();
                    sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                    break;
                case "all":
                    plugin.reloadPersistance();
                    plugin.reloadSwearList();
                    plugin.reloadNoSwearList();
                    plugin.reloadMessages();
                    plugin.reloadIgnore();
                    sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", component));
                    break;
                default:
                    plugin.reloadPersistance();
                    plugin.reloadSwearList();
                    plugin.reloadNoSwearList();
                    plugin.reloadMessages();
                    plugin.reloadIgnore();
                    sender.sendMessage(plugin.messages.get("asbReloaded").replace("{{component}}", "all"));
            }
        });
    }
}
