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
package me.dordsor21.AdvSwearBlock.listener;

import me.dordsor21.AdvSwearBlock.AdvSwearBlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JoinLeaveListener implements Listener {

    private final AdvSwearBlock plugin;

    private final boolean firstSwear;

    public JoinLeaveListener(AdvSwearBlock plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        firstSwear = plugin.getConfig().getBoolean("sendMessageOnFirstSwear");
    }

    @EventHandler()
    public void onPlayerLeave(final PlayerQuitEvent e) {
        Player pl = e.getPlayer();
        if (plugin.ignoring) {
            plugin.ignore.removeIgnorer(pl.getUniqueId());
        }
        if (plugin.persistence) {
            plugin.sql.setSwearBlock(pl.getUniqueId(), pl.hasMetadata("swearBlock"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player p = e.getPlayer();
            UUID uuid = p.getUniqueId();
            if (plugin.ignoring) {
                if (plugin.sql.isIgnoreree(uuid) && plugin.sql.isIgnoring(uuid)) {//loads any ignored players into cache.
                    String[] ignorees = plugin.sql.getIgnorees(uuid).split(",");
                    try {
                        List<String> toIgnore = new ArrayList<>();
                        for (String ignoree : ignorees) {
                            if (ignoree == null || ignoree.isEmpty()) {
                                continue;
                            }
                            String name = plugin.sql.getNamefromID(ignoree);
                            if (plugin.ignore.cannotIgnore.contains(name.toLowerCase())) {
                                plugin.sql.unIgnorePlayer(uuid, plugin.uuids.getUUIDFromName(name));
                                continue;
                            }
                            toIgnore.add(name);
                        }
                        plugin.ignore.setIgnorer(uuid, toIgnore);
                    } catch (StringIndexOutOfBoundsException ignored) {
                    }
                }
                if (p.hasPermission("asb.noignore") && !plugin.ignore.cannotIgnore.contains(p.getName().toLowerCase())) {
                    plugin.ignore.cannotIgnore.add(p.getName().toLowerCase());
                    plugin.sql.setCannotIgnore(p.getUniqueId(), true);
                }
                if (!p.hasPermission("asb.noignore") && plugin.ignore.cannotIgnore.contains(p.getName().toLowerCase())) {
                    plugin.ignore.cannotIgnore.remove(p.getName().toLowerCase());
                    plugin.sql.setCannotIgnore(p.getUniqueId(), false);
                }
            }
            if ((plugin.persistence && plugin.sql.swearBlock(p.getUniqueId())) || plugin.getConfig()
                .getBoolean("swearing.defaultStatus")) {//turns swearblock on (persistant cross-network n stuff)
                p.setMetadata("swearBlock", new FixedMetadataValue(plugin, true));
                if (firstSwear) {
                    p.setMetadata("firstSwear", new FixedMetadataValue(plugin, true));
                }
            } else {
                p.setMetadata("swearBlock", new FixedMetadataValue(plugin, false));
            }
        });
    }

}
