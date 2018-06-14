/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.dordsor21.AdvSwearBlock.cmd.IgnoreCmd;
import me.dordsor21.AdvSwearBlock.cmd.MainCmd;
import me.dordsor21.AdvSwearBlock.cmd.SwearWordCmd;
import me.dordsor21.AdvSwearBlock.listener.ChatListener;
import me.dordsor21.AdvSwearBlock.listener.JoinLeaveListener;
import me.dordsor21.AdvSwearBlock.listener.PlayerChatPacketListener;
import me.dordsor21.AdvSwearBlock.listener.PlayerSignPacketListener;
import me.dordsor21.AdvSwearBlock.util.Ignore;
import me.dordsor21.AdvSwearBlock.util.SQL;
import me.dordsor21.AdvSwearBlock.util.SwearList;
import me.dordsor21.AdvSwearBlock.util.UUIDs;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;
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
    private Boolean signs;
    public Map<String, String> messages;
    private PlayerChatPacketListener playerChatPacketListener;
    private PlayerSignPacketListener playerSignPacketListener;
    private ChatListener chatListener;
    private JoinLeaveListener joinLeaveListener;
    private ProtocolManager pM;

    @Override
    public void onEnable() {

        plugin = this;

        saveDefaultConfig();

        loadConfigValues();
        messages = new HashMap<>();

        for (String message : getConfig().getConfigurationSection("messages").getValues(false).keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));

        if (ignoring && !persistence) {
            getLogger().severe(prefix + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }

        uuids = new UUIDs(this);

        if (ignoring) {
            ignore = new Ignore(this);
            getCommand("ignore").setExecutor(new IgnoreCmd(this, ignore));
        }

        getCommand("swearblock").setExecutor(new SwearWordCmd(this));
        getCommand("asb").setExecutor(new MainCmd(this));

        pM = ProtocolLibrary.getProtocolManager();

        playerChatPacketListener = new PlayerChatPacketListener(this, pM);
        if (signs) {
            playerSignPacketListener = new PlayerSignPacketListener(this, pM);
        }
        chatListener = new ChatListener(this);

        if (persistence) {
            sql = new SQL(this);
        }

        joinLeaveListener = new JoinLeaveListener(this);

        swearList = new SwearList(this);
    }

    private void loadConfigValues() {
        prefix = getConfig().getString("prefix");
        persistence = getConfig().getBoolean("persistence");
        ignoring = getConfig().getBoolean("ignoring.enabled");
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        signs = getConfig().getBoolean("swearing.signs", true);
    }

    public void reloadNoSwearList() {
        reloadConfig();
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
    }

    public void reloadSwearList() {
        reloadConfig();
        pM.removePacketListeners(this);
        HandlerList.unregisterAll(playerChatPacketListener);
        HandlerList.unregisterAll(playerSignPacketListener);
        HandlerList.unregisterAll(chatListener);
        HandlerList.unregisterAll(joinLeaveListener);
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        signs = getConfig().getBoolean("swearing.signs", true);
        persistence = getConfig().getBoolean("persistence");
        playerChatPacketListener = new PlayerChatPacketListener(this, pM);
        if (signs)
            playerSignPacketListener = new PlayerSignPacketListener(this, pM);
        chatListener = new ChatListener(this);
        joinLeaveListener = new JoinLeaveListener(this);
        swearList = new SwearList(this);
    }

    public void reloadIgnore() {
        reloadConfig();
        persistence = getConfig().getBoolean("persistence");
        ignoring = getConfig().getBoolean("ignoring.enabled");
        if (ignoring && persistence) {
            ignore = new Ignore(this);
            getCommand("ignore").setExecutor(new IgnoreCmd(this, ignore));
        } else {
            getLogger().severe(prefix + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }
    }

    public void reloadMessages() {
        reloadConfig();
        messages = new HashMap<>();
        prefix = getConfig().getString("prefix");
        for (String message : getConfig().getConfigurationSection("messages").getValues(false).keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));
    }

}
