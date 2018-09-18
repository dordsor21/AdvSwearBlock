/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.cmd;

import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Ignore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class IgnoreCmd implements CommandExecutor, TabCompleter {

    private Main plugin;
    private Ignore ignore;

    public IgnoreCmd(Main plugin, Ignore ignore) {
        this.plugin = plugin;
        this.ignore = ignore;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String strng, final String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length < 1)
                player.sendMessage(plugin.messages.get("ignoreUsage"));
            else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    UUID puuid = player.getUniqueId();
                    switch (args[0]) {
                        case "add":
                        case "ignore":
                            if (player.hasPermission("asb.ignore")) {
                                if (args.length == 1) {
                                    player.sendMessage(plugin.messages.get("ignoreUsage"));
                                    return;
                                }
                                if (args.length > 2) {
                                    StringBuilder failed = new StringBuilder();//list of people that were failed to be ignored
                                    StringBuilder completed = new StringBuilder();//list of people that were successfully ignored
                                    StringBuilder alreadyIgnored = new StringBuilder();//list of people that were already ignored
                                    StringBuilder cannotIgnore = new StringBuilder();
                                    List<UUID> uuids = new ArrayList<>();//list of ignorees' uuids to ignore
                                    List<String> ignorees = new ArrayList<>();//list of player names to store in the cache to allow for quick detection of an ignored player
                                    if (ignore.isIgnorer(puuid))//check if the player is already ignore anyone, and getting that list
                                        ignorees = ignore.getIgnored(puuid);
                                    for (int i = 1; i < args.length; i++) {//iterate through the arguments
                                        String pl = args[i];
                                        if (pl.equalsIgnoreCase(sender.getName()))//make sure they're not ignoring themselves
                                            continue;
                                        if (plugin.ignore.cannotIgnore.contains(pl.toLowerCase())) {
                                            cannotIgnore.append(pl).append(" ");
                                            continue;
                                        }
                                        if (ignorees.contains(pl.toLowerCase())) {//make sure the person to be ignored is not already ignored
                                            alreadyIgnored.append(pl).append(" ");
                                            continue;
                                        }
                                        UUID uuid = plugin.uuids.getUUIDFromName(pl);//Use uuidCache/mojang UUID API
                                        if (uuid == null) {//if the UUID is still null, they can't be a player
                                            failed.append(pl).append(" ");
                                            continue;
                                        }
                                        completed.append(pl).append(" ");
                                        uuids.add(uuid);
                                        ignorees.add(pl.toLowerCase());
                                    }
                                    plugin.sql.ignorePlayers(player.getUniqueId(), uuids);
                                    player.sendMessage(plugin.messages.get("ignorePlayersSuccess").replace("{{players}}", completed));
                                    if (!failed.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("ignorePlayersFailure").replace("{{players}}", failed));
                                    if (!cannotIgnore.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("cannotIgnorePlayers").replace("{{players}}", cannotIgnore));
                                    if (!alreadyIgnored.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("ignorePlayersAlready").replace("{{players}}", alreadyIgnored));
                                    if (ignore.isIgnorer(puuid))
                                        ignore.editIgnorer(puuid, ignorees);
                                    else
                                        ignore.addIgnorer(puuid, ignorees);
                                } else {//Simpler and easier method for just one player to be ignored.
                                    List<String> ignorees = new ArrayList<>();
                                    if (args[1].equalsIgnoreCase(player.getName())) {
                                        player.sendMessage(plugin.messages.get("ignoreSelf"));
                                        return;
                                    }
                                    if (plugin.ignore.cannotIgnore.contains(args[1].toLowerCase())) {
                                        player.sendMessage(plugin.messages.get("cannotIgnorePlayer").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    if (ignorees.contains(args[1].toLowerCase())) {
                                        player.sendMessage(plugin.messages.get("ignorePlayerAlready").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    UUID uuid = plugin.uuids.getUUIDFromName(args[1]);
                                    if (ignore.isIgnorer(puuid))
                                        ignorees = ignore.getIgnored(puuid);
                                    if (uuid == null) {
                                        player.sendMessage(plugin.messages.get("ignorePlayerFailure").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    plugin.sql.ignorePlayer(player.getUniqueId(), uuid);
                                    player.sendMessage(plugin.messages.get("ignorePlayerSuccess").replace("{{player}}", args[1]));
                                    ignorees.add(args[1].toLowerCase());
                                    if (ignore.isIgnorer(puuid))
                                        ignore.editIgnorer(puuid, ignorees);
                                    else
                                        ignore.addIgnorer(puuid, ignorees);
                                }
                            } else
                                player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.ignore"));
                            break;
                        case "remove":
                        case "unignore":
                            if (player.hasPermission("asb.unignore")) {
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
                                        if (pl.equalsIgnoreCase(player.getName()))
                                            continue;
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
                                    if (!notIgnored.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("unignorePlayersAlready").replace("{{players}}", notIgnored));
                                    if (!failed.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("unignorePlayersFailed").replace("{{players}}", failed));
                                    ignore.editIgnorer(puuid, ignorees);
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
                                    ignore.editIgnorer(puuid, ignorees);
                                }
                            } else
                                player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.unignore"));
                            break;
                        case "list":
                            if (player.hasPermission("asb.list")) {
                                StringBuilder list = new StringBuilder();
                                if (plugin.ignore.isIgnorer(puuid)) {
                                    for (String pl : plugin.ignore.getIgnored(puuid))
                                        list.append(pl).append("  ");
                                    if (list.toString().equals("")) {
                                        player.sendMessage(plugin.messages.get("ignoringNoone"));
                                        return;
                                    } else {
                                        player.sendMessage(plugin.messages.get("listIgnoredPlayers").replace("{{players}}", list));
                                    }
                                } else
                                    player.sendMessage(plugin.messages.get("ignoringNoone"));
                            } else
                                player.sendMessage(plugin.messages.get("noPermission").replace("{{permission}}", "asb.list"));
                            break;
                        default:
                            player.sendMessage(plugin.messages.get("ignoreListUsage"));
                            break;
                    }

                });
            }
        } else
            sender.sendMessage(plugin.messages.get("Must be a player."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String strng, final String[] args) {
        if (!(sender instanceof Player))
            return null;
        if (args.length == 1)
            if (args[0].equals(""))
                return Arrays.asList("add", "remove", "list");
            else
                for (String s : new String[]{"add", "remove", "list"})
                    if (s.startsWith(args[0].toLowerCase()))
                        return Collections.singletonList(s);
        if (args.length == 2)
            if (args[0].equalsIgnoreCase("add"))
                if (args[1].equals("")) {
                    List<String> ret = new ArrayList<>();
                    plugin.getServer().getOnlinePlayers().forEach(p -> ret.add(p.getName()));
                    return ret;
                } else {
                    List<String> ret = new ArrayList<>();
                    for (Player p : plugin.getServer().getOnlinePlayers())
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                            ret.add(p.getName());
                    return ret;
                }
            else if (args[0].equalsIgnoreCase("remove"))
                if (args[1].equals(""))
                    return plugin.ignore.getIgnored(((Player) sender).getUniqueId());
                else {
                    List<String> ret = new ArrayList<>();
                    for (String s : plugin.ignore.getIgnored(((Player) sender).getUniqueId()))
                        if (s.toLowerCase().startsWith(args[1].toLowerCase()))
                            ret.add(s);
                    return ret;
                }
        return null;
    }

}
