package me.dordsor21.AdvSwearBlock.cmd;

import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Ignore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IgnoreCmd implements CommandExecutor {

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
                                    List<UUID> uuids = new ArrayList<>();//list of ignorees' uuids to ignore
                                    List<String> ignorees = new ArrayList<>();//list of player names to store in the cache to allow for quick detection of an ignored player
                                    if (ignore.isIgnorer(puuid))//check if the player is already ignore anyone, and getting that list
                                        ignorees = ignore.getIgnored(puuid);
                                    for (int i = 1; i < args.length; i++) {//iterate through the arguments
                                        String pl = args[i];
                                        if (pl.equalsIgnoreCase(sender.getName()))//make sure they're not ignoring themselves
                                            continue;
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
                                    player.sendMessage(plugin.messages.get("ignoredPlayersSuccess").replace("{{players}}", completed));
                                    if (!failed.toString().equals("")) {
                                        player.sendMessage(plugin.messages.get("ignoredPlayersFailure").replace("{{players}}", failed));
                                    }
                                    if (!alreadyIgnored.toString().equals("")) {
                                        player.sendMessage(plugin.messages.get("ignoredPlayersAlready").replace("{{players}}", alreadyIgnored));
                                    }
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
                                    if (ignorees.contains(args[1].toLowerCase())) {
                                        player.sendMessage(plugin.messages.get("ignoredPlayerAlready").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    UUID uuid = plugin.uuids.getUUIDFromName(args[1]);
                                    if (ignore.isIgnorer(puuid))
                                        ignorees = ignore.getIgnored(puuid);
                                    if (uuid == null) {
                                        player.sendMessage(plugin.messages.get("ignoredPlayerFailure").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    plugin.sql.ignorePlayer(player.getUniqueId(), uuid);
                                    player.sendMessage(plugin.messages.get("ignoredPlayerSuccess").replace("{{player}}", args[1]));
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
                                    player.sendMessage(plugin.messages.get("unignoredPlayersSuccess").replace("{{players}}", completed));
                                    if (!notIgnored.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("unignoredPlayersAlready").replace("{{players}}", notIgnored));
                                    if (!failed.toString().equals(""))
                                        player.sendMessage(plugin.messages.get("unignoredPlayersFailed").replace("{{players}}", failed));
                                    ignore.editIgnorer(puuid, ignorees);
                                } else {
                                    if (!ignorees.contains(args[1])) {
                                        player.sendMessage(plugin.messages.get("unignoredPlayerAlready").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    UUID uuid = plugin.uuids.getUUIDFromName(args[1]);
                                    if (uuid == null) {
                                        player.sendMessage(plugin.messages.get("unignoredPlayerFailed").replace("{{player}}", args[1]));
                                        return;
                                    }
                                    plugin.sql.unIgnorePlayer(player.getUniqueId(), uuid);
                                    player.sendMessage(plugin.messages.get("unignoredPlayerSuccess").replace("{{player}}", args[1]));
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

}
