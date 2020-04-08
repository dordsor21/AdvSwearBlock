/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.listener;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ChatListener implements Listener {

    private Main plugin;
    private Long cooldown;
    private double spaceLimit;

    private boolean spaces;
    private boolean cd;

    public ChatListener(Main plugin) {
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
                p.sendMessage(plugin.messages.get("cooldown")
                    .replace("{{cooldown}}", String.valueOf(cooldown.doubleValue() / 20)));
                e.setCancelled(true);
                return;
            } else {
                p.setMetadata("cooldown", new FixedMetadataValue(plugin, true));
                plugin.getServer().getScheduler()
                    .runTaskLater(plugin, () -> p.removeMetadata("cooldown", plugin), cooldown);
            }
        }
        if (spaces && !p.hasPermission("asb.bypass.spacecheck")) {
            String msg = e.getMessage();
            if (msg.contains(" ") &&
                ((double) msg.replaceAll("[^ ]", "").length() / (double) msg.replaceAll("[ ]", "")
                    .length()) >= spaceLimit) {
                p.sendMessage(plugin.messages.get("spaceLimit"));
                e.setCancelled(true);
            }
        }
    }
}
