/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.util;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import me.dordsor21.AdvSwearBlock.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQL {

    private final static HashMap<String, String> columns;

    private String tableName;

    static {
        columns = new HashMap<>();
        columns.put("id", "INT(11)AUTO_INCREMENT|PRIMARY|NOTNULL");
        columns.put("uuid", "VARCHAR(32)NOTNULL");
        columns.put("name", "VARCHAR(16)NOTNULL");
        columns.put("ignoreNo", "INT(11)!DEFAULT=0!");
        columns.put("ignorees", "VARCHAR(255)");
        columns.put("canBeIgnored", "BOOLEAN!DEFAULT=FALSE!");
    }

    private Connection conn;
    private Main plugin;

    public SQL(Main plugin) {
        this.plugin = plugin;
        tableName = plugin.getConfig().getString("SQL.tablePrefix", "") + "advSwearBlock";

        columns.put("isBlocking", "BOOLEAN!DEFAULT=" + plugin.getConfig().getBoolean("defaultStatus", true) + "!");

        tableExists();
        checkColumns();

        MysqlDataSource source = new MysqlDataSource();
        source.setServerName(plugin.getConfig().getString("SQL.hostname"));
        source.setDatabaseName(plugin.getConfig().getString("SQL.database"));
        source.setUser(plugin.getConfig().getString("SQL.username"));
        source.setPassword(plugin.getConfig().getString("SQL.password"));
        source.setPort(plugin.getConfig().getInt("SQL.port"));
        source.setAutoReconnect(plugin.getConfig().getBoolean("SQL.autoreconnect", true));
        source.setUseSSL(plugin.getConfig().getBoolean("SQL.useSSL", false));

        try {
            conn = source.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating MySQL connection");
            e.printStackTrace();
        }
    }

    private void tableExists() {
        try {
            PreparedStatement stm = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + tableName + " (id INT(11) NOT NULL PRIMARY KEYAUTO_INCREMENT, uuid VARCHAR(32) NOT NULL, name VARCHAR(16) NOT NULL," +
                    " ignoreNo INT(11) DEFAULT 0, ignorees VARCHAR(255), isBlocking BOOLEAN DEFAULT ?, canBeIgnored BOOLEAN DEFAULT FALSE)");
            stm.setBoolean(1, plugin.getConfig().getBoolean("defaultStatus"));
            stm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkColumns() {
        try {
            for (String col : columns.keySet()) {
                PreparedStatement stmt1 = conn.prepareStatement("SELECT column_name FROM INFORMATION_SCHEMA.columns "
                        + "WHERE table_name = 'ignoring' AND column_name = '" + col + "';");
                ResultSet result = stmt1.executeQuery();
                if (!result.next()) {
                    String type = columns.get(col);
                    String def = "";
                    String auto_incr = "";
                    String nul = "";
                    String key = "";
                    if (type.contains("NOTNULL")) {
                        nul = " NOT NULL ";
                        type = type.replace("NOTNULL", "");
                    }
                    if (type.contains("AUTO_INCREMENT")) {
                        auto_incr = " AUTO_INCREMENT ";
                        type = type.replace("AUTO_INCREMENT", "");
                    }
                    if (type.contains("|")) {
                        key = " " + type.split("//|")[1] + " KEY ";
                        type = type.split("//|")[0] + type.split("//|")[2];
                    }
                    if (type.contains("!")) {
                        def = " DEFAULT " + type.split("!")[1].split("=")[1];
                        try {
                            type = type.split("!")[0] + type.split("!")[2];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            type = type.split("!")[0].replace("!", "");
                        }
                    }
                    PreparedStatement stmt = conn.prepareStatement("ALTER TABLE " + tableName + " ADD COLUMN (" + col + " " + type + def + nul + auto_incr + key + ");");
                    stmt.executeUpdate();
                    plugin.getLogger().info("Column " + col + " created");
                    stmt.close();
                }
                stmt1.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    UUID uuidFromCache(String name) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT uuid from " + tableName + " where LOWER(name)=?");
            stmt.setString(1, name.toLowerCase());
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return plugin.uuids.getUUID(res.getString("uuid"));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String nameFromCache(UUID uuid) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getString("name");
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createIgnoreree(UUID uuid, int no) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + tableName + " (uuid, ignoreNo) VALUES (?,?)");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            stmt.setInt(2, no);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isIgnoreree(UUID uuid) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            boolean ret = res.next();
            stmt.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getIgnorereeID(UUID uuid) {
        if (isIgnoreree(uuid)) {
            try {
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
                e.printStackTrace();
                return null;
            }
        } else
            return null;
    }

    public String getNamefromID(String id) {
        try {
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
            e.printStackTrace();
            return null;
        }
    }

    private boolean alreadyIgnored(UUID ignorer, String iID) {
        String s = getIgnorees(ignorer);
        if (s.equals(","))
            return false;
        try {
            List<String> ignorees = new ArrayList<>(Arrays.asList(s.substring(1, s.length() - 1).split(",")));
            return ignorees.contains(iID);
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    private void incrIgnoredNo(String iID) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignoreNo = ignoreNo + 1 WHERE id=?;");
            stmt.setInt(1, Integer.valueOf(iID));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lwrIgnoredNo(String iID) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignoreNo = ignoreNo - 1 WHERE id=?;");
            stmt.setInt(1, Integer.valueOf(iID));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception ignored) {

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
        try {
            ignorees = ignorees + iID + ",";
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, ignorees);
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unIgnorePlayer(UUID ignorer, UUID ignoree) {
        String ignorees = getIgnorees(ignorer);
        String iID = getIgnorereeID(ignoree);
        if (!alreadyIgnored(ignoree, iID)) {
            try {
                if (!ignorees.contains("," + iID + ","))
                    return;
                ignorees = ignorees.replace("," + iID + ",", ",");
                lwrIgnoredNo(iID);
                PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
                stmt.setString(1, ignorees);
                stmt.setString(2, plugin.uuids.niceUUID(ignorer));
                stmt.executeUpdate();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void ignorePlayers(UUID ignorer, List<UUID> ignorees) {
        if (!isIgnoreree(ignorer))
            createIgnoreree(ignorer, 0);
        StringBuilder cIgnorees = new StringBuilder(getIgnorees(ignorer));
        List<String> lIgnorees = new ArrayList<>();
        if (cIgnorees.length() > 0 && !cIgnorees.toString().equals(",") && !cIgnorees.toString().equalsIgnoreCase("null")) {
            try {
                String[] test = cIgnorees.substring(1, cIgnorees.length() - 1).split(",");
                Collections.addAll(lIgnorees, test);
            } catch (StringIndexOutOfBoundsException ignored) {
            }
        } else {
            cIgnorees = new StringBuilder(",");
        }
        try {
            for (UUID id : ignorees) {
                String iID = getIgnorereeID(id);
                if (iID == null || iID.equals("") || iID.isEmpty() || iID.equals("null")) {
                    createIgnoreree(id, 0);
                    iID = getIgnorereeID(id);
                }
                if (lIgnorees.contains(iID))
                    continue;
                cIgnorees.append(iID).append(",");
                incrIgnoredNo(iID);
            }
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, cIgnorees.toString());
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unIgnorePlayers(UUID ignorer, List<UUID> ignorees) {
        String cIgnorees = getIgnorees(ignorer);
        List<String> lIgnorees = new ArrayList<>();
        if (!cIgnorees.equals(","))
            lIgnorees = Arrays.asList(cIgnorees.substring(1, cIgnorees.length() - 1).split(","));
        try {
            for (UUID id : ignorees) {
                String iID = getIgnorereeID(id);
                if (!lIgnorees.contains(iID))
                    continue;
                cIgnorees = cIgnorees.replace("," + iID + ",", ",");
                lwrIgnoredNo(iID);
            }
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET ignorees=? WHERE uuid=?");
            stmt.setString(1, cIgnorees);
            stmt.setString(2, plugin.uuids.niceUUID(ignorer));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UUID getIgnorantUUID(String id) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM " + tableName + " WHERE id=?");
            stmt.setString(1, id);
            ResultSet res = stmt.executeQuery();
            String ret = "";
            if (res.next())
                ret = res.getString("uuid");
            stmt.close();
            return plugin.uuids.getUUID(ret);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isIgnoring(UUID uuid) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM  " + tableName + " WHERE uuid=? AND ignorees IS NOT NULL");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            boolean ret = res.next();
            stmt.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getIgnorees(UUID ignorer) {
        try {
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
            e.printStackTrace();
            return null;
        }
    }

    public List<String> noIgnoreList() {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM " + tableName + " WHERE canBeIgnored = 0");
            ResultSet res = stmt.executeQuery();
            List<String> ret = new ArrayList<>();
            while (res.next())
                ret.add(res.getString("name").toLowerCase());
            stmt.close();
            res.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setCannotIgnore(UUID uuid, boolean bool) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET canBeIgnored=? WHERE uuid=?");
            stmt.setBoolean(1, bool);
            stmt.setString(2, plugin.uuids.niceUUID(uuid));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean swearBlock(UUID uuid) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT isBlocking FROM " + tableName + " WHERE uuid=?");
            stmt.setString(1, plugin.uuids.niceUUID(uuid));
            ResultSet res = stmt.executeQuery();
            if (!res.next())
                return false;
            return res.getBoolean("swearBlock");
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setSwearBlock(UUID uuid, boolean sb) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET isBlocking=? WHERE uuid=?");
            stmt.setBoolean(1, sb);
            stmt.setString(2, plugin.uuids.niceUUID(uuid));
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
