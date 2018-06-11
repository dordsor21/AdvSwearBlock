package me.dordsor21.AdvSwearBlock.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Json;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class PlayerSignPacketListener implements Listener {

    public PlayerSignPacketListener(Main pl) {
        ProtocolManager pM = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, pl);
        double multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ? pl.getConfig().getDouble("swearing.swearWordMultiplier") : 1;
        pM.addPacketListener(new PacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
                    Player p = e.getPlayer();
                    if (!p.hasMetadata("swearBlock"))
                        return;
                    List<NbtBase<?>> list = e.getPacket().getListNbtModifier().read(0);
                    Boolean packetEdited = false;
                    List<NbtBase<?>> list2 = new ArrayList<>();
                    for (NbtBase nbtB : list) {
                        NbtCompound nbt = (NbtCompound) nbtB;
                        for (String key : nbt.getKeys())
                            if (key.contains("Text"))
                                if (!nbt.getString(key).equals("{\"text\":\"\"}")) {
                                    String json = Json.fromReadJson(nbt.getString(key));
                                    String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                    String[] words = text.split(" ");
                                    StringBuilder c = new StringBuilder("{\"text\":\"");
                                    boolean actuallyEdited = false;
                                    for (String w : words) {//iterate through all the words in the packet's message
                                        String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                                        if (!pl.ignoreSwear.contains(temp.toLowerCase()) && Bukkit.getPlayer(temp) == null) {
                                            try {
                                                String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                                //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                                List<String> bads = pl.swearList.getList().parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                                String bad = "";
                                                for (String potential : bads)//finds longest swear words of list from above.
                                                    if (potential.length() > bad.length())
                                                        bad = potential;
                                                if (!bad.equals("") && !bad.isEmpty() && (temp.length() < multiplier * bad.length())) {
                                                    c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*")).append(" ");
                                                    actuallyEdited = true;
                                                    continue;
                                                }
                                            } catch (NoSuchElementException ignored) {
                                            }
                                        }
                                        c.append(w).append(" ");
                                    }
                                    String s2 = c.toString();
                                    String s3;
                                    String s4;
                                    if (actuallyEdited) {//only actually resend/etc the packet if we've edited it.
                                        if (c.toString().endsWith(","))
                                            c.substring(0, c.length() - 1);
                                        if (c.toString().endsWith(" "))
                                            c.substring(0, c.length() - 1);
                                        String message = Json.colourCodeToJson(c + "\"}]");
                                        if (message.startsWith("\"},"))
                                            message = message.substring(3);

                                        String sign = message.replace("§", "&").substring(0, message.length() - 1);

                                        s3 = sign;

                                        HashMap<String, Boolean> chatHasStyle = new HashMap<>();
                                        for (String style : new String[]{"bold", "italics", "underline"})
                                            chatHasStyle.put(style, sign.contains("\"" + style + "\":true"));

                                        String chatParts[] = sign.split(",~~,");
                                        int i = 0;
                                        for (String part : chatParts) {
                                            StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
                                            for (String style : chatHasStyle.keySet())
                                                if (chatHasStyle.get(style) && !partBuilder.toString().contains("\"" + style + "\":"))
                                                    partBuilder.append(",\"").append(style).append("\":false");
                                            part = partBuilder.append("}").toString();
                                            sign = sign.replace(chatParts[i], part);
                                            i++;
                                        }
                                        sign = sign.replace(",~~,", ",");
                                        s4 = sign;
                                        try {
                                            packetEdited = true;
                                            nbt.put(key, "[" + sign + "]");
                                        } catch (Exception ex) {
                                            pl.getLogger().severe("Error Editing Sign Packet. Please report this to GitLab");
                                            pl.getLogger().severe("json: " + json);
                                            pl.getLogger().severe("text: " + text);
                                            pl.getLogger().severe("s2: " + s2);
                                            pl.getLogger().severe("s3: " + s3);
                                            pl.getLogger().severe("s4: " + s4);
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                        list2.add(nbt);
                    }
                    if (packetEdited)
                        e.getPacket().getListNbtModifier().write(0, list2);
                }
                if (e.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                    NbtCompound nbt = (NbtCompound) e.getPacket().getNbtModifier().read(0);
                    Player p = e.getPlayer();
                    if (!p.hasMetadata("swearBlock"))
                        return;
                    boolean signEdited = false;
                    for (String key : nbt.getKeys())
                        if (key.contains("Text"))
                            if (!nbt.getString(key).equals("{\"text\":\"\"}")) {
                                String json = Json.fromReadJson(nbt.getString(key));
                                String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                String[] words = text.split(" ");
                                StringBuilder c = new StringBuilder("{\"text\":\"");
                                boolean actuallyEdited = false;
                                for (String w : words) {//iterate through all the words in the packet's message
                                    String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                                    if (!pl.ignoreSwear.contains(temp.toLowerCase()) && Bukkit.getPlayer(temp) == null) {
                                        try {
                                            String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                            //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                            List<String> bads = pl.swearList.getList().parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                            String bad = "";
                                            for (String potential : bads)//finds longest swear words of list from above.
                                                if (potential.length() > bad.length())
                                                    bad = potential;
                                            if (!bad.equals("") && !bad.isEmpty() && (temp.length() < multiplier * bad.length())) {
                                                c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*")).append(" ");
                                                actuallyEdited = true;
                                                continue;
                                            }
                                        } catch (NoSuchElementException ignored) {
                                        }
                                    }
                                    c.append(w).append(" ");
                                }
                                String s2 = c.toString();
                                String s3;
                                String s4;
                                if (actuallyEdited) {//only actually resend/etc the packet if we've edited it.
                                    if (c.toString().endsWith(","))
                                        c.substring(0, c.length() - 1);
                                    if (c.toString().endsWith(" "))
                                        c.substring(0, c.length() - 1);
                                    String message = Json.colourCodeToJson(c + "\"}]");
                                    if (message.startsWith("\"},"))
                                        message = message.substring(3);

                                    String sign = message.replace("§", "&").substring(0, message.length() - 1);

                                    s3 = sign;

                                    HashMap<String, Boolean> chatHasStyle = new HashMap<>();
                                    for (String style : new String[]{"bold", "italics", "underline"})
                                        chatHasStyle.put(style, sign.contains("\"" + style + "\":true"));

                                    String chatParts[] = sign.split(",~~,");
                                    int i = 0;
                                    for (String part : chatParts) {
                                        StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
                                        for (String style : chatHasStyle.keySet())
                                            if (chatHasStyle.get(style) && !partBuilder.toString().contains("\"" + style + "\":"))
                                                partBuilder.append(",\"").append(style).append("\":false");
                                        part = partBuilder.append("}").toString();
                                        sign = sign.replace(chatParts[i], part);
                                        i++;
                                    }
                                    sign = sign.replace(",~~,", ",");
                                    s4 = sign;
                                    try {
                                        signEdited = true;
                                        nbt.put(key, "[" + sign + "]");
                                        pl.getLogger().severe("json: " + json);
                                        pl.getLogger().severe("text: " + text);
                                        pl.getLogger().severe("s2: " + s2);
                                        pl.getLogger().severe("s3: " + s3);
                                        pl.getLogger().severe("s4: " + s4);
                                    } catch (Exception ex) {
                                        pl.getLogger().severe("Error Editing Sign Packet. Please report this to GitLab");
                                        pl.getLogger().severe("json: " + json);
                                        pl.getLogger().severe("text: " + text);
                                        pl.getLogger().severe("s2: " + s2);
                                        pl.getLogger().severe("s3: " + s3);
                                        pl.getLogger().severe("s4: " + s4);
                                        ex.printStackTrace();
                                    }
                                }
                            }
                    if (signEdited)
                        e.getPacket().getNbtModifier().write(0, nbt);
                }
            }
        });
    }
}
