package me.dordsor21.AdvSwearBlock.util;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.Bukkit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UUIDs {
    private final JSONParser jsonParser = new JSONParser();
    private Main plugin;

    public UUIDs(Main plugin) {
        this.plugin = plugin;
    }

    public UUID getUUIDFromName(String username) {
        Map<String, UUID> uuidMap = new HashMap<>();
        UUID uid = Bukkit.getPlayer(username).getUniqueId();
        if (uid == null)
            uid = Bukkit.getOfflinePlayer(username).getUniqueId();
        if (uid == null && plugin.persistence)
            uid = plugin.sql.uuidFromCache(username);
        if (uid == null && plugin.getConfig().getBoolean("UseMojangAPI"))
            try {
                HttpURLConnection connection = createConnection();
                String body = "[\"" + username + "\"]";
                writeBody(connection, body);
                JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                for (Object profile : array) {
                    JSONObject jsonProfile = (JSONObject) profile;
                    String id = (String) jsonProfile.get("id");
                    String name = (String) jsonProfile.get("name");
                    UUID uuid = getUUID(id);
                    uuidMap.put(name, uuid);
                }
                return uuidMap.get(username);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        return uid;
    }

    private void writeBody(HttpURLConnection connection, String body) throws Exception {
        OutputStream stream = connection.getOutputStream();
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }

    private HttpURLConnection createConnection() throws Exception {
        String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
        URL url = new URL(PROFILE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }

    String niceUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
