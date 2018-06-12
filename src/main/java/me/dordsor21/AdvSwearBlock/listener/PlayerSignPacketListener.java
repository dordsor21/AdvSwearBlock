/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Json;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlayerSignPacketListener implements Listener {
    public PlayerSignPacketListener(Main pl, ProtocolManager pM) {

        Bukkit.getPluginManager().registerEvents(this, pl);
        double multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ? pl.getConfig().getDouble("swearing.swearWordMultiplier") : 1;
        int incr = pl.getConfig().getInt("swearing.noMultiplierIncrement");

        pM.addPacketListener(new PacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
                    Player p = e.getPlayer();
                    List<MetadataValue> metaValues = p.getMetadata("swearBlock");
                    boolean swearBlock;
                    if (metaValues.size() == 0)
                        swearBlock = (pl.persistence && pl.sql.swearBlock(p.getUniqueId())) || plugin.getConfig().getBoolean("swearing.defaultStatus");
                    else {
                        boolean thisPlugin = false;
                        for (MetadataValue metadataValue : metaValues)
                            thisPlugin = thisPlugin ? thisPlugin : metadataValue.getOwningPlugin() == pl;
                        if (!thisPlugin)
                            swearBlock = (pl.persistence && pl.sql.swearBlock(p.getUniqueId())) || plugin.getConfig().getBoolean("swearing.defaultStatus");
                        else
                            swearBlock = p.hasMetadata("swearBlock");
                    }
                    if (!swearBlock)
                        return;
                    List<NbtBase<?>> list = e.getPacket().getListNbtModifier().read(0);
                    Boolean packetEdited = false;
                    List<NbtBase<?>> list2 = new ArrayList<>();
                    for (NbtBase nbtB : list) {
                        NbtCompound nbt = (NbtCompound) nbtB;
                        for (String key : nbt.getKeys()) {
                            if (key.contains("Text")) {
                                if (!nbt.getString(key).equals("{\"text\":\"\"}")) {
                                    boolean actuallyEdited = false;
                                    String json = Json.fromReadJson(nbt.getString(key));
                                    String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                    List<String> mList = pl.swearList.getList().get("multiplier");
                                    List<String> nomList = pl.swearList.getList().get("nomultiplier");
                                    List<String> both = new ArrayList<>();
                                    both.addAll(mList);
                                    both.addAll(nomList);
                                    String regexedMsg = text;
                                    for (String word : both) {
                                        StringBuilder regex = new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0)).append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                                        for (int i = 1; i < word.length(); i++)
                                            regex.append("\\s*((").append(word.charAt(i)).append("|&[a-fk-o\\d]))+");
                                        Matcher matcher = Pattern.compile(regex.toString()).matcher(regexedMsg);
                                        while (matcher.find() && matcher.group().length() > word.length()) {
                                            regexedMsg = regexedMsg.replace(matcher.group(), new String(new char[matcher.group().replace(" ", "").length()]).replace('\0', '*'));
                                            actuallyEdited = true;
                                        }
                                    }
                                    String[] words = regexedMsg.split(" ");
                                    StringBuilder c = new StringBuilder("{\"text\":\"");
                                    for (String w : words) {//iterate through all the words in the packet's message
                                        String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                                        if (Bukkit.getPlayer(temp) == null) {
                                            try {
                                                String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                                if (pl.ignoreSwear.contains(testTemp))
                                                    continue;

                                                //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                                List<String> badmul = pl.swearList.getList().get("multiplier").parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                                List<String> badt = pl.swearList.getList().get("nomultiplier").parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                                String bad1 = null;
                                                String bad2 = null;
                                                boolean multiple = false;
                                                if (!(badmul.size() > 1 || badt.size() > 1
                                                        || (badmul.size() > 0 && StringUtils.countMatches(testTemp, badmul.get(0)) > 1)
                                                        || (badt.size() > 0 && StringUtils.countMatches(testTemp, badt.get(0)) > 1))) {
                                                    bad1 = badmul.size() > 0 ? badmul.get(0) : null;
                                                    bad2 = badt.size() > 0 ? badt.get(0) : null;
                                                } else {
                                                    multiple = true;
                                                }
                                                if (multiple || (bad1 != null && !bad1.equals("") && !bad1.isEmpty() && (testTemp.length() <= multiplier * bad1.length()))
                                                        || (bad2 != null && testTemp.length() <= bad2.length() + incr)) {
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
                                            nbt.put(key, "[" + sign + "]");
                                        } catch (Exception ex) {
                                            pl.getLogger().severe("Error Editing Sign Packet. Please report this to GitLab");
                                            pl.getLogger().severe("json: " + json);
                                            pl.getLogger().severe("text: " + text);
                                            pl.getLogger().severe("regexed: " + regexedMsg);
                                            pl.getLogger().severe("s2: " + s2);
                                            pl.getLogger().severe("s3: " + s3);
                                            pl.getLogger().severe("s4: " + s4);
                                            ex.printStackTrace();
                                        }
                                    }
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
                                boolean actuallyEdited = false;
                                String json = Json.fromReadJson(nbt.getString(key));
                                String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                List<String> mList = pl.swearList.getList().get("multiplier");
                                List<String> nomList = pl.swearList.getList().get("nomultiplier");
                                List<String> both = new ArrayList<>();
                                both.addAll(mList);
                                both.addAll(nomList);
                                String regexedMsg = text;
                                for (String word : both) {
                                    StringBuilder regex = new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0)).append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                                    for (int i = 1; i < word.length(); i++)
                                        regex.append("\\s*((").append(word.charAt(i)).append("|&[a-fk-o\\d]))+");
                                    Matcher matcher = Pattern.compile(regex.toString()).matcher(regexedMsg);
                                    while (matcher.find() && matcher.group().length() > word.length()) {
                                        regexedMsg = regexedMsg.replace(matcher.group(), new String(new char[matcher.group().replace(" ", "").length()]).replace('\0', '*'));
                                        actuallyEdited = true;
                                    }
                                }
                                String[] words = regexedMsg.split(" ");
                                StringBuilder c = new StringBuilder("{\"text\":\"");
                                for (String w : words) {//iterate through all the words in the packet's message
                                    String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                                    if (Bukkit.getPlayer(temp) == null) {
                                        try {
                                            String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                            if (pl.ignoreSwear.contains(testTemp))
                                                continue;

                                            //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                            List<String> badmul = pl.swearList.getList().get("multiplier").parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                            List<String> badt = pl.swearList.getList().get("nomultiplier").parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                            String bad1 = null;
                                            String bad2 = null;
                                            boolean multiple = false;
                                            if (!(badmul.size() > 1 || badt.size() > 1
                                                    || (badmul.size() > 0 && StringUtils.countMatches(testTemp, badmul.get(0)) > 1)
                                                    || (badt.size() > 0 && StringUtils.countMatches(testTemp, badt.get(0)) > 1))) {
                                                bad1 = badmul.size() > 0 ? badmul.get(0) : null;
                                                bad2 = badt.size() > 0 ? badt.get(0) : null;
                                            } else {
                                                multiple = true;
                                            }
                                            if (multiple || (bad1 != null && !bad1.equals("") && !bad1.isEmpty() && (testTemp.length() <= multiplier * bad1.length()))
                                                    || (bad2 != null && testTemp.length() <= bad2.length() + incr)) {
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
                                    } catch (Exception ex) {
                                        pl.getLogger().severe("Error Editing Sign Packet. Please report this to GitLab");
                                        pl.getLogger().severe("json: " + json);
                                        pl.getLogger().severe("text: " + text);
                                        pl.getLogger().severe("regexed: " + regexedMsg);
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
