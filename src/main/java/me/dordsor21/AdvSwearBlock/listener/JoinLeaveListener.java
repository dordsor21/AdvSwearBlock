package me.dordsor21.AdvSwearBlock.listener;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JoinLeaveListener implements Listener {

    private Main plugin;

    private boolean firstSwear;

    public JoinLeaveListener(Main plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        firstSwear = plugin.getConfig().getBoolean("sendMessageOnFirstSwear");
    }

    @EventHandler()
    public void onPlayerLeave(final PlayerQuitEvent e) {
        Player pl = e.getPlayer();
        plugin.ignore.removeIgnorer(pl.getUniqueId());
        plugin.sql.setSwearBlock(pl.getUniqueId(), pl.hasMetadata("swearBlock"));
    }

    @EventHandler()
    public void onPlayerJoin(final PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player p = e.getPlayer();
            UUID uuid = p.getUniqueId();
            if (plugin.ignoring && plugin.sql.isIgnoreree(uuid) && plugin.sql.isIgnoring(uuid)) {//loads any ignored players into cache.
                String[] ignorees = plugin.sql.getIgnorees(uuid).split(",");
                try {
                    List<String> toIgnore = new ArrayList<>();
                    for (String ignoree : ignorees) {
                        if (ignoree == null || ignoree.equals("") || ignoree.isEmpty())
                            continue;
                        String name = plugin.sql.getNamefromID(ignoree);
                        try {
                            toIgnore.add(name);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    plugin.ignore.addIgnorer(uuid, toIgnore);
                } catch (StringIndexOutOfBoundsException ignored) {
                }
            }
            if ((plugin.persistence && plugin.sql.swearBlock(p.getUniqueId())) || plugin.getConfig().getBoolean("defaultStatus")) {//turns swearblock on (persistant cross-network n stuff)
                p.setMetadata("swearBlock", new FixedMetadataValue(plugin, true));
                if (firstSwear)
                    p.setMetadata("firstSwear", new FixedMetadataValue(plugin, true));
            }
        });
    }

}
