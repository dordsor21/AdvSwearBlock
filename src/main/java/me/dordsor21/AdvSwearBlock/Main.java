package me.dordsor21.AdvSwearBlock;

import me.dordsor21.AdvSwearBlock.cmd.IgnoreCmd;
import me.dordsor21.AdvSwearBlock.cmd.MainCmd;
import me.dordsor21.AdvSwearBlock.cmd.SwearWordCmd;
import me.dordsor21.AdvSwearBlock.listener.ChatListener;
import me.dordsor21.AdvSwearBlock.listener.JoinLeaveListener;
import me.dordsor21.AdvSwearBlock.listener.PlayerChatPacketListener;
import me.dordsor21.AdvSwearBlock.util.Ignore;
import me.dordsor21.AdvSwearBlock.util.SQL;
import me.dordsor21.AdvSwearBlock.util.SwearList;
import me.dordsor21.AdvSwearBlock.util.UUIDs;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin {

    public static Main plugin;
    public List<String> ignoreSwear = new ArrayList<>();
    private String prefix;
    public SwearList swearList;
    public Ignore ignore;
    public SQL sql;
    public UUIDs uuids;
    public Boolean persistence;
    public Boolean ignoring;
    public Map<String, String> messages;

    @Override
    public void onEnable() {

        plugin = this;

        saveDefaultConfig();

        prefix = getConfig().getString("prefix");
        persistence = getConfig().getBoolean("persistence");
        ignoring = getConfig().getBoolean("ignoring.enabled");
        messages = new HashMap<>();
        for (String message : getConfig().getConfigurationSection("messages").getValues(false).keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));

        if (ignoring && !persistence) {
            getLogger().severe(prefix + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }

        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        uuids = new UUIDs(this);

        if (ignoring) {
            ignore = new Ignore();
            getCommand("ignore").setExecutor(new IgnoreCmd(this, ignore));
        }


        getCommand("swearblock").setExecutor(new SwearWordCmd(this));
        getCommand("asb").setExecutor(new MainCmd(this));

        new PlayerChatPacketListener(this);
        new ChatListener(this);

        if (persistence) {
            sql = new SQL(this);
            new JoinLeaveListener(this);
        }

        swearList = new SwearList(this);
    }

    public void reloadNoSwearList() {
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
    }

    public void reloadSwearList() {
        swearList = new SwearList(this);
    }

    public void reloadMessages() {
        messages = new HashMap<>();
        for (String message : getConfig().getConfigurationSection("messages").getValues(false).keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));
    }

}
