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
import me.dordsor21.AdvSwearBlock.util.Ignore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class IgnoreCmd implements CommandExecutor, TabCompleter {

    private final AdvSwearBlock plugin;
    private final Ignore ignore;

    public IgnoreCmd(AdvSwearBlock plugin, Ignore ignore) {
        this.plugin = plugin;
        this.ignore = ignore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String strng, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages.get("Must be a player."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.messages.get("ignoreUsage"));
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID puuid = player.getUniqueId();
            switch (args[0]) {
                case "add":
                case "ignore":
                    if (!player.hasPermission("asb.ignore")) {
                        player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.ignore"));
                        return;
                    }
                    if (args.length == 1) {
                        player.sendMessage(plugin.messages.get("ignoreUsage"));
                        return;
                    }
                    if (args.length > 2) {
                        StringBuilder failed = new StringBuilder(); //list of people that were failed to be ignored
                        StringBuilder completed = new StringBuilder(); //list of people that were successfully ignored
                        StringBuilder alreadyIgnored = new StringBuilder(); //list of people that were already ignored
                        StringBuilder cannotIgnore = new StringBuilder();
                        List<UUID> uuids = new ArrayList<>(); //list of ignorees' uuids to ignore
                        List<String> ignorees =
                            new ArrayList<>(); //list of player names to store in the cache to allow for quick detection of an ignored player
                        if (ignore.isIgnorer(puuid)) { //check if the player is already ignore anyone, and getting that list
                            ignorees = ignore.getIgnored(puuid);
                        }
                        for (int i = 1; i < args.length; i++) { //iterate through the arguments
                            String pl = args[i];
                            if (pl.equalsIgnoreCase(sender.getName())) {//make sure they're not ignoring themselves
                                continue;
                            }
                            if (plugin.ignore.cannotIgnore.contains(pl.toLowerCase())) {
                                cannotIgnore.append(pl).append(" ");
                                continue;
                            }
                            if (ignorees.contains(
                                pl.toLowerCase())) { //make sure the person to be ignored is not already ignored
                                alreadyIgnored.append(pl).append(" ");
                                continue;
                            }
                            UUID uuid = plugin.uuids.getUUIDFromName(pl); //Use uuidCache/mojang UUID API
                            if (uuid == null) { //if the UUID is still null, they can't be a player
                                failed.append(pl).append(" ");
                                continue;
                            }
                            completed.append(pl).append(" ");
                            uuids.add(uuid);
                            ignorees.add(pl.toLowerCase());
                        }
                        plugin.sql.ignorePlayers(player.getUniqueId(), uuids);
                        player.sendMessage(plugin.messages.get("ignorePlayersSuccess").replace("{{players}}", completed));
                        if (!failed.isEmpty()) {
                            player.sendMessage(plugin.messages.get("ignorePlayersFailure").replace("{{players}}", failed));
                        }
                        if (!cannotIgnore.isEmpty()) {
                            player.sendMessage(
                                plugin.messages.get("cannotIgnorePlayers").replace("{{players}}", cannotIgnore));
                        }
                        if (!alreadyIgnored.isEmpty()) {
                            player.sendMessage(
                                plugin.messages.get("ignorePlayersAlready").replace("{{players}}", alreadyIgnored));
                        }
                        if (ignore.isIgnorer(puuid)) {
                            ignore.setIgnorer(puuid, ignorees);
                        } else {
                            ignore.setIgnorer(puuid, ignorees);
                        }
                    } else { //Simpler and easier method for just one player to be ignored.
                        List<String> ignorees = new ArrayList<>();
                        if (args[1].equalsIgnoreCase(player.getName())) {
                            player.sendMessage(plugin.messages.get("ignoreSelf"));
                            return;
                        }
                        if (plugin.ignore.cannotIgnore.contains(args[1].toLowerCase())) {
                            player.sendMessage(plugin.messages.get("cannotIgnorePlayer").replace("{{player}}", args[1]));
                            return;
                        }
                        UUID uuid = plugin.uuids.getUUIDFromName(args[1]);
                        if (ignore.isIgnorer(puuid)) {
                            ignorees = ignore.getIgnored(puuid);
                        }
                        if (uuid == null) {
                            player.sendMessage(plugin.messages.get("ignorePlayerFailure").replace("{{player}}", args[1]));
                            return;
                        }
                        plugin.sql.ignorePlayer(player.getUniqueId(), uuid);
                        player.sendMessage(plugin.messages.get("ignorePlayerSuccess").replace("{{player}}", args[1]));
                        ignorees.add(args[1].toLowerCase());
                        if (ignore.isIgnorer(puuid)) {
                            ignore.setIgnorer(puuid, ignorees);
                        } else {
                            ignore.setIgnorer(puuid, ignorees);
                        }
                    }
                    break;
                case "remove":
                case "unignore":
                    if (!player.hasPermission("asb.unignore")) {
                        player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.unignore"));
                    }
                    if (!ignore.isIgnorer(puuid)) {//check if the player is actually ignoring anyone
                        player.sendMessage(plugin.messages.get("ignoringNoone"));
                        return;
                    }
                    List<String> ignorees = ignore.getIgnored(puuid);//get the list of ignored players

                    if (args.length == 1) {
                        player.sendMessage(plugin.messages.get("unignoreUsage"));
                        return;
                    }
                    if (args.length > 2) {
                        StringBuilder failed = new StringBuilder();
                        StringBuilder completed = new StringBuilder();
                        StringBuilder notIgnored = new StringBuilder();
                        List<UUID> uuids = new ArrayList<>();
                        for (int i = 1; i < args.length; i++) {
                            String pl = args[i];
                            if (pl.equalsIgnoreCase(player.getName())) {
                                continue;
                            }
                            if (!ignorees.contains(pl.toLowerCase())) {
                                notIgnored.append(pl).append(" ");
                                continue;
                            }
                            UUID uuid = plugin.uuids.getUUIDFromName(pl);//Use uuidCache/mojang UUID API
                            if (uuid == null) {//if the UUID is still null, they can't be a player
                                failed.append(pl).append(" ");
                                continue;
                            }
                            completed.append(pl).append(" ");
                            uuids.add(uuid);
                            ignorees.remove(pl.toLowerCase());
                        }
                        plugin.sql.unIgnorePlayers(player.getUniqueId(), uuids);
                        player.sendMessage(plugin.messages.get("unignorePlayersSuccess").replace("{{players}}", completed));
                        if (!notIgnored.isEmpty()) {
                            player.sendMessage(
                                plugin.messages.get("unignorePlayersAlready").replace("{{players}}", notIgnored));
                        }
                        if (!failed.isEmpty()) {
                            player.sendMessage(plugin.messages.get("unignorePlayersFailed").replace("{{players}}", failed));
                        }
                        ignore.setIgnorer(puuid, ignorees);
                    } else {
                        if (!ignorees.contains(args[1])) {
                            player.sendMessage(plugin.messages.get("unignorePlayerAlready").replace("{{player}}", args[1]));
                            return;
                        }
                        UUID uuid = plugin.uuids.getUUIDFromName(args[1]);
                        if (uuid == null) {
                            player.sendMessage(plugin.messages.get("unignorePlayerFailed").replace("{{player}}", args[1]));
                            return;
                        }
                        plugin.sql.unIgnorePlayer(player.getUniqueId(), uuid);
                        player.sendMessage(plugin.messages.get("unignorePlayerSuccess").replace("{{player}}", args[1]));
                        ignorees.remove(args[1].toLowerCase());
                        ignore.setIgnorer(puuid, ignorees);
                    }
                    break;
                case "list":
                    if (!player.hasPermission("asb.list")) {
                        player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.list"));
                    }
                    StringBuilder list = new StringBuilder();
                    if (plugin.ignore.isIgnorer(puuid)) {
                        for (String pl : plugin.ignore.getIgnored(puuid))
                            list.append(pl).append("  ");
                        if (list.isEmpty()) {
                            player.sendMessage(plugin.messages.get("ignoringNoone"));
                            return;
                        } else {
                            player.sendMessage(plugin.messages.get("listIgnoredPlayers").replace("{{players}}", list));
                        }
                    } else {
                        player.sendMessage(plugin.messages.get("ignoringNoone"));
                    }
                    break;
                default:
                    player.sendMessage(plugin.messages.get("ignoreListUsage"));
                    break;
            }

        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String strng, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }
        if (args.length == 1) {
            if (args[0].isEmpty()) {
                return Arrays.asList("add", "remove", "list");
            } else {
                for (String s : new String[] {"add", "remove", "list"})
                    if (s.startsWith(args[0].toLowerCase())) {
                        return Collections.singletonList(s);
                    }
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                List<String> ret = new ArrayList<>();
                if (args[1].isEmpty()) {
                    plugin.getServer().getOnlinePlayers().forEach(p -> ret.add(p.getName()));
                } else {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            ret.add(p.getName());
                        }
                    }
                }
                return ret;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args[1].isEmpty()) {
                    return plugin.ignore.getIgnored(((Player) sender).getUniqueId());
                } else {
                    List<String> ret = new ArrayList<>();
                    for (String s : plugin.ignore.getIgnored(((Player) sender).getUniqueId())) {
                        if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                            ret.add(s);
                        }
                    }
                    return ret;
                }
            }
        }
        return null;
    }

}
