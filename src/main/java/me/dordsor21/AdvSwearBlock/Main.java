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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin {

    public static Main plugin;
    public List<String> ignoreSwear;
    public String[] ignoreSwearArray;
    public SwearList swearList;
    public Ignore ignore;
    public SQL sql;
    public UUIDs uuids;
    public Boolean persistence;
    public Boolean ignoring;
    public Map<String, String> messages;
    public boolean failSilent;
    private String prefix;
    private Boolean signs;
    private PlayerChatPacketListener playerChatPacketListener;
    private PlayerSignPacketListener playerSignPacketListener;
    private ChatListener chatListener;
    private JoinLeaveListener joinLeaveListener;
    private ProtocolManager pM;

    @Override public void onEnable() {

        plugin = this;

        saveDefaultConfig();

        loadConfigValues();
        messages = new HashMap<>();

        for (String message : getConfig().getConfigurationSection("messages").getValues(false)
            .keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&',
                prefix + getConfig().getString("messages." + message)));

        if (persistence) {
            sql = new SQL(this);
            persistence = sql.initialise();
        }

        if (ignoring && !persistence) {
            getLogger().severe(prefix
                + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }

        uuids = new UUIDs(this);

        if (ignoring) {
            ignore = new Ignore(this);
            getCommand("ignore").setExecutor(new IgnoreCmd(this, ignore));
            getCommand("ignore").setTabCompleter(new IgnoreCmd(this, ignore));
        }

        getCommand("swearblock").setExecutor(new SwearWordCmd(this));
        getCommand("asb").setExecutor(new MainCmd(this));

        pM = ProtocolLibrary.getProtocolManager();

        swearList = new SwearList(this);

        playerChatPacketListener = new PlayerChatPacketListener(this, pM);
        if (signs) {
            playerSignPacketListener = new PlayerSignPacketListener(this, pM);
        }
        chatListener = new ChatListener(this);

        joinLeaveListener = new JoinLeaveListener(this);
    }

    private void loadConfigValues() {
        prefix = getConfig().getString("prefix", "&2[AdvSB]&r ");
        persistence = getConfig().getBoolean("persistence", false);
        ignoring = getConfig().getBoolean("ignoring.enabled", true);
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        ignoreSwearArray = getConfig().getStringList("ignoreWords.swear").toArray(new String[0]);
        signs = getConfig().getBoolean("swearing.signs", true);
        failSilent = getConfig().getBoolean("failsilently", false);
    }

    public void reloadNoSwearList() {
        reloadConfig();
        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        ignoreSwearArray = getConfig().getStringList("ignoreWords.swear").toArray(new String[0]);
    }

    public void reloadSwearList() {
        reloadConfig();
        pM.removePacketListeners(this);
        HandlerList.unregisterAll(playerChatPacketListener);
        HandlerList.unregisterAll(playerSignPacketListener);
        HandlerList.unregisterAll(chatListener);
        HandlerList.unregisterAll(joinLeaveListener);

        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        ignoreSwearArray = getConfig().getStringList("ignoreWords.swear").toArray(new String[0]);
        signs = getConfig().getBoolean("swearing.signs", true);

        playerChatPacketListener = new PlayerChatPacketListener(this, pM);
        if (signs)
            playerSignPacketListener = new PlayerSignPacketListener(this, pM);
        chatListener = new ChatListener(this);
        joinLeaveListener = new JoinLeaveListener(this);
        swearList = new SwearList(this);
    }

    public void reloadIgnore() {
        reloadConfig();
        if (ignoring && persistence) {
            ignore = new Ignore(this);
            getCommand("ignore").setExecutor(new IgnoreCmd(this, ignore));
        } else if (ignoring) {
            getLogger().severe(prefix
                + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }
    }

    public void reloadMessages() {
        reloadConfig();
        messages = new HashMap<>();
        prefix = getConfig().getString("prefix");
        for (String message : getConfig().getConfigurationSection("messages").getValues(false)
            .keySet())
            messages.put(message, ChatColor.translateAlternateColorCodes('&',
                prefix + getConfig().getString("messages." + message)));
    }

    public void reloadPersistance() {
        reloadConfig();
        if (persistence)
            sql.closeConnection();
        persistence = getConfig().getBoolean("persistence");
        ignoring = getConfig().getBoolean("ignoring.enabled");
        if (persistence) {
            sql = new SQL(this);
            persistence = sql.initialise();
        }
    }

}
