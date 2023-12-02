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
package me.dordsor21.advswearblock.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.dordsor21.advswearblock.AdvSwearBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SQL {

    private static final Logger LOGGER = LogManager.getLogger("AdvSwearBlock/" + SQL.class.getSimpleName());

    private final String tableName;
    private final AdvSwearBlock plugin;
    private HikariDataSource dataSource;

    public SQL(AdvSwearBlock plugin) {
        this.plugin = plugin;
        tableName = plugin.getConfig().getString("SQL.tablePrefix", "") + "advSwearBlock";
    }

    public void initialise() {
        FileConfiguration conf = plugin.getConfig();
        HikariConfig hikConf = new HikariConfig();
        hikConf.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s", conf.getString("SQL.hostname"), conf.getString("SQL.port"),
            conf.getString("SQL.database")));
        hikConf.setUsername(conf.getString("SQL.username"));
        hikConf.setPassword(conf.getString("SQL.password"));
        dataSource = new HikariDataSource(hikConf);
        tableExists();
    }

    public void closeConnection() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void tableExists() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stm = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + tableName
                + " (id INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT, uuid VARCHAR(32) NOT NULL, name VARCHAR(16) NOT NULL,"
                + " ignoreNo INT(11) DEFAULT 0, ignorees VARCHAR(255), isBlocking BOOLEAN DEFAULT ?, canBeIgnored BOOLEAN DEFAULT FALSE)");
            stm.setBoolean(1, plugin.getConfig().getBoolean("defaultStatus"));
            stm.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error creating table `{}` if not exists", tableName, e);
        }
    }

    private void createIgnoreree(UUID uuid, int no) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + tableName + " (uuid, ignoreNo) VALUES (?,?)");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            stmt.setInt(2, no);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error creating ignoree", e);
        }
    }

    public boolean isIgnoreree(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            boolean ret = res.next();
            stmt.close();
            return ret;
        } catch (Exception e) {
            LOGGER.error("Error checking ignoree status", e);
            return false;
        }
    }

    private String getIgnorereeID(UUID uuid) {
        if (isIgnoreree(uuid)) {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + tableName + " WHERE uuid=?");
                stmt.setString(1, plugin.uuids.niceUUID(uuid));
                ResultSet res = stmt.executeQuery();
                if (res.next()) {
                    String ret = String.valueOf(res.getInt("id"));
                    stmt.close();
                    return ret;
                } else {
                    stmt.close();
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("Error getting ignoree ID", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public String getNamefromID(String id) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM " + tableName + " WHERE id=?");
            stmt.setString(1, id);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                String ret = res.getString("name");
                stmt.close();
                return ret;
            } else {
                stmt.close();
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting name from ID", e);
            return null;
        }
    }

    private boolean alreadyIgnored(UUID ignorer, String iID) {
        String s = getIgnorees(ignorer);
        if (s.equals(",")) {
            return false;
        }
        try {
            List<String> ignorees = new ArrayList<>(Arrays.asList(s.substring(1, s.length() - 1).split(",")));
            return ignorees.contains(iID);
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    private void incrIgnoredNo(String iID) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt =
                conn.prepareStatement("UPDATE " + tableName + " SET ignoreNo = ignoreNo + 1 WHERE id=?;");
            stmt.setInt(1, Integer.parseInt(iID));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error incrementing ignored count", e);
        }
    }

    private void lwrIgnoredNo(String iID) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt =
                conn.prepareStatement("UPDATE " + tableName + " SET ignoreNo = ignoreNo - 1 WHERE id=?;");
            stmt.setInt(1, Integer.parseInt(iID));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error lowering ignored count", e);
        }
    }

    public void ignorePlayer(UUID ignorer, UUID ignoree) {
        String ignorees = getIgnorees(ignorer);
        String iID = getIgnorereeID(ignoree);
        if (iID == null) {
            createIgnoreree(ignoree, 1);
            iID = getIgnorereeID(ignoree);
        } else if (alreadyIgnored(ignorer, iID)) {
            return;
        } else {
            incrIgnoredNo(iID);
        }
        try (Connection conn = dataSource.getConnection()) {
            ignorees = ignorees + iID + ",";
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, ignorees);
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error ignoring player", e);
        }
    }

    public void unIgnorePlayer(UUID ignorer, UUID ignoree) {
        String ignorees = getIgnorees(ignorer);
        String iID = getIgnorereeID(ignoree);
        if (!alreadyIgnored(ignoree, iID)) {
            try (Connection conn = dataSource.getConnection()) {
                if (!ignorees.contains("," + iID + ",")) {
                    return;
                }
                ignorees = ignorees.replace("," + iID + ",", ",");
                lwrIgnoredNo(iID);
                PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
                stmt.setString(1, ignorees);
                stmt.setString(2, plugin.uuids.niceUUID(ignorer));
                stmt.executeUpdate();
                stmt.close();
            } catch (Exception e) {
                LOGGER.error("Error unignoring player", e);
            }
        }
    }

    public void ignorePlayers(UUID ignorer, List<UUID> ignorees) {
        if (!isIgnoreree(ignorer)) {
            createIgnoreree(ignorer, 0);
        }
        StringBuilder cIgnorees = new StringBuilder(getIgnorees(ignorer));
        List<String> lIgnorees = new ArrayList<>();
        if (!cIgnorees.isEmpty() && !cIgnorees.toString().equals(",") && !cIgnorees.toString().equalsIgnoreCase("null")) {
            try {
                String[] test = cIgnorees.substring(1, cIgnorees.length() - 1).split(",");
                Collections.addAll(lIgnorees, test);
            } catch (StringIndexOutOfBoundsException ignored) {
            }
        } else {
            cIgnorees = new StringBuilder(",");
        }
        try (Connection conn = dataSource.getConnection()) {
            for (UUID id : ignorees) {
                String iID = getIgnorereeID(id);
                if (iID == null || iID.isEmpty() || iID.equals("null")) {
                    createIgnoreree(id, 0);
                    iID = getIgnorereeID(id);
                }
                if (lIgnorees.contains(iID)) {
                    continue;
                }
                cIgnorees.append(iID).append(",");
                incrIgnoredNo(iID);
            }
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, cIgnorees.toString());
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error ignoring players", e);
        }
    }

    public void unIgnorePlayers(UUID ignorer, List<UUID> ignorees) {
        String cIgnorees = getIgnorees(ignorer);
        List<String> lIgnorees = new ArrayList<>();
        if (!cIgnorees.equals(",")) {
            lIgnorees = Arrays.asList(cIgnorees.substring(1, cIgnorees.length() - 1).split(","));
        }
        try (Connection conn = dataSource.getConnection()) {
            for (UUID id : ignorees) {
                String iID = getIgnorereeID(id);
                if (!lIgnorees.contains(iID)) {
                    continue;
                }
                cIgnorees = cIgnorees.replace("," + iID + ",", ",");
                lwrIgnoredNo(iID);
            }
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, cIgnorees);
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error unignoring players", e);
        }
    }

    public boolean isIgnoring(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt =
                conn.prepareStatement("SELECT id FROM  " + tableName + " WHERE uuid=? AND ignorees IS NOT NULL");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            boolean ret = res.next();
            stmt.close();
            return ret;
        } catch (Exception e) {
            LOGGER.error("Error checking if player is ignoring", e);
            return false;
        }
    }

    public String getIgnorees(UUID ignorer) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT ignorees FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(ignorer));
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                String ret = res.getString("ignorees");
                stmt.close();
                return ret;
            } else {
                stmt.close();
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting ignorees", e);
            return null;
        }
    }

    public List<String> noIgnoreList() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM " + tableName + " WHERE canBeIgnored = 0");
            ResultSet res = stmt.executeQuery();
            List<String> ret = new ArrayList<>();
            while (res.next())
                ret.add(res.getString("name").toLowerCase());
            stmt.close();
            res.close();
            return ret;
        } catch (Exception e) {
            LOGGER.error("Error getting no-ignore list", e);
            return null;
        }
    }

    public void setCannotIgnore(UUID uuid, boolean bool) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET canBeIgnored=? WHERE uuid=?");
            stmt.setBoolean(1, bool);
            stmt.setString(2, plugin.uuids.niceUUID(uuid));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error setting cannot ignore status", e);
        }
    }

    public boolean swearBlock(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT isBlocking FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            if (!res.next()) {
                return false;
            }
            return res.getBoolean("swearBlock");
        } catch (Exception e) {
            LOGGER.error("Error getting swear-blocking status", e);
            return true;
        }
    }

    public void setSwearBlock(UUID uuid, boolean sb) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET isBlocking=? WHERE uuid=?");
            stmt.setBoolean(1, sb);
            stmt.setString(2, plugin.uuids.niceUUID(uuid));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            LOGGER.error("Error updating swear-block status", e);
        }
    }
}
