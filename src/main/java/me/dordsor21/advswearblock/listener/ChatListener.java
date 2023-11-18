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
package me.dordsor21.advswearblock.listener;

import me.dordsor21.advswearblock.AdvSwearBlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ChatListener implements Listener {

    private final AdvSwearBlock plugin;
    private final Long cooldown;
    private final double spaceLimit;

    private final boolean spaces;
    private final boolean cd;

    public ChatListener(AdvSwearBlock plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        cd = plugin.getConfig().getBoolean("cooldown.enabled", true);
        cooldown = plugin.getConfig().getLong("cooldown.length", 1500) / 50;
        spaces = plugin.getConfig().getBoolean("spaces.enabled", true);
        spaceLimit = plugin.getConfig().getDouble("spaces.limit", 0.45);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChatEvent(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (cd && !p.hasPermission("asb.bypass.cooldown")) {
            if (p.hasMetadata("cooldown")) {
                p.sendMessage(
                    plugin.messages.get("cooldown").replace("{{cooldown}}", String.valueOf(cooldown.doubleValue() / 20)));
                e.setCancelled(true);
                return;
            } else {
                p.setMetadata("cooldown", new FixedMetadataValue(plugin, true));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> p.removeMetadata("cooldown", plugin), cooldown);
            }
        }
        if (spaces && !p.hasPermission("asb.bypass.spacecheck")) {
            String msg = e.getMessage();
            if (msg.contains(" ")
                && ((double) msg.replaceAll("[^ ]", "").length() / (double) msg.replaceAll("[ ]", "").length())
                >= spaceLimit) {
                p.sendMessage(plugin.messages.get("spaceLimit"));
                e.setCancelled(true);
            }
        }
    }
}
