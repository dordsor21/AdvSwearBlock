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
import me.dordsor21.AdvSwearBlock.listener.SystemChatPacketListener;
import me.dordsor21.AdvSwearBlock.util.Ignore;
import me.dordsor21.AdvSwearBlock.util.SQL;
import me.dordsor21.AdvSwearBlock.util.SwearList;
import me.dordsor21.AdvSwearBlock.util.UUIDs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvSwearBlock extends JavaPlugin {

    private static final Logger LOGGER = LogManager.getLogger("AdvSwearBlock/" + AdvSwearBlock.class.getSimpleName());

    public static AdvSwearBlock plugin;
    public List<String> ignoreSwear;
    public String[] ignoreSwearArray;
    public SwearList swearList;
    public Ignore ignore;
    public SQL sql;
    public UUIDs uuids;
    public boolean persistence;
    public boolean ignoring;
    public Map<String, String> messages;
    public boolean failSilent;
    private String prefix;
    private boolean signs;
    private PlayerChatPacketListener playerChatPacketListener;
    private SystemChatPacketListener systemChatPacketListener;
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
            messages.put(message,
                ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));

        if (persistence) {
            sql = new SQL(this);
            try {
                sql.initialise();
            } catch (Exception e) {
                LOGGER.error("Error initialising SQL, disabling persistence, e");
                persistence = false;
                getConfig().set("persistence", false);
            }
        }

        if (ignoring && !persistence) {
            LOGGER.error(prefix + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
            getConfig().set("ignoring.enabled", false);
        }

        uuids = new UUIDs();

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
        systemChatPacketListener = new SystemChatPacketListener(this, pM);
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

    public void reloadConfigValues() {
        prefix = getConfig().getString("prefix", "&2[AdvSB]&r ");
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
        HandlerList.unregisterAll(systemChatPacketListener);
        if (playerSignPacketListener != null) {
            HandlerList.unregisterAll(playerSignPacketListener);
        }
        HandlerList.unregisterAll(chatListener);
        HandlerList.unregisterAll(joinLeaveListener);

        ignoreSwear = getConfig().getStringList("swearing.not-blocked");
        ignoreSwearArray = getConfig().getStringList("ignoreWords.swear").toArray(new String[0]);
        signs = getConfig().getBoolean("swearing.signs", true);

        playerChatPacketListener = new PlayerChatPacketListener(this, pM);
        systemChatPacketListener = new SystemChatPacketListener(this, pM);
        if (signs) {
            playerSignPacketListener = new PlayerSignPacketListener(this, pM);
        }
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
            LOGGER.error(prefix + " You cannot have ignoring enabled without persistence (MySQL)! Turning ignoring off!");
            ignoring = false;
        }
    }

    public void reloadMessages() {
        reloadConfig();
        messages = new HashMap<>();
        prefix = getConfig().getString("prefix");
        for (String message : getConfig().getConfigurationSection("messages").getValues(false).keySet())
            messages.put(message,
                ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + message)));
    }

    public void reloadPersistance() {
        reloadConfig();
        if (persistence) {
            sql.closeConnection();
        }
        persistence = getConfig().getBoolean("persistence");
        ignoring = getConfig().getBoolean("ignoring.enabled");
        if (persistence) {
            sql = new SQL(this);
            sql.initialise();
        }
    }

}
