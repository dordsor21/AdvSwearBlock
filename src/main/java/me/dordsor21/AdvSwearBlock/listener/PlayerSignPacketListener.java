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
package me.dordsor21.AdvSwearBlock.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import me.dordsor21.AdvSwearBlock.AdvSwearBlock;
import me.dordsor21.AdvSwearBlock.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerSignPacketListener implements Listener {

    private static final Logger LOGGER =
        LogManager.getLogger("AdvSwearBlock/" + PlayerSignPacketListener.class.getSimpleName());

    private static final Field tileEntityTag;

    static {
        Field tileEntityTagField;
        try {
            Class<?> clientboundLevelChunkPacketDataA =
                Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$a");
            tileEntityTagField = clientboundLevelChunkPacketDataA.getDeclaredField("d");
            tileEntityTagField.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            LOGGER.error("Error obtaining field for sign packet editing", e);
            tileEntityTagField = null;
        }
        tileEntityTag = tileEntityTagField;
    }

    public PlayerSignPacketListener(AdvSwearBlock pl, ProtocolManager pM) {

        Bukkit.getPluginManager().registerEvents(this, pl);
        double multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ?
            pl.getConfig().getDouble("swearing.swearWordMultiplier") :
            1;
        int incr = pl.getConfig().getInt("swearing.noMultiplierIncrement");

        pM.addPacketListener(new PacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK,
            PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
                    Player p = e.getPlayer();
                    List<MetadataValue> metaValues = p.getMetadata("swearBlock");
                    boolean swearBlock;
                    if (metaValues.isEmpty()) {
                        swearBlock = (pl.persistence && pl.sql.swearBlock(p.getUniqueId())) || plugin.getConfig()
                            .getBoolean("swearing.defaultStatus");
                    } else {
                        boolean thisPlugin = false;
                        for (MetadataValue metadataValue : metaValues)
                            thisPlugin = thisPlugin ? thisPlugin : metadataValue.getOwningPlugin() == pl;
                        if (!thisPlugin) {
                            swearBlock = (pl.persistence && pl.sql.swearBlock(p.getUniqueId())) || plugin.getConfig()
                                .getBoolean("swearing.defaultStatus");
                        } else {
                            swearBlock = p.hasMetadata("swearBlock");
                        }
                    }
                    if (!swearBlock) {
                        return;
                    }

                    var structures1 = e.getPacket().getStructures();
                    var structures2 = structures1.read(0).getStructures();
                    var modifier = structures2.read(2).getModifier();
                    if (modifier.size() == 0) {
                        return;
                    }

                    Class<?> clazz = modifier.read(0).getClass();
                    Object[] beArr = (Object[]) modifier.read(0);

                    var converter = BukkitConverters.getNbtConverter();
                    List<Object> list2 = new ArrayList<>();
                    boolean packetEdited = false;
                    for (Object obj : beArr) {
                        if (obj == null) {
                            continue;
                        }
                        NbtCompound nbt;
                        try {
                            nbt = (NbtCompound) converter.getSpecific(tileEntityTag.get(obj));
                            if (nbt == null) {
                                continue;
                            }
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }

                        for (String key : nbt.getKeys()) {
                            if (key.contains("Text")) {
                                if (!nbt.getString(key).equals("{\"text\":\"\"}")) {
                                    boolean actuallyEdited = false;
                                    String json = Json.fromReadJson(nbt.getString(key));
                                    String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                    List<String> mList = pl.swearList.getList().get("multiplier");
                                    List<String> nomList = pl.swearList.getList().get("nomultiplier");
                                    List<String> oList = pl.swearList.getList().get("onlymatch");
                                    List<String> both = new ArrayList<>();
                                    both.addAll(mList);
                                    both.addAll(nomList);
                                    String regexedMsg = text;
                                    for (String word : both) {
                                        StringBuilder regex =
                                            new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0))
                                                .append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                                        for (int i = 1; i < word.length(); i++)
                                            regex.append("\\s*((").append(word.charAt(i)).append("|&[a-fk-o\\d]))+");
                                        Matcher matcher = Pattern.compile(regex.toString()).matcher(regexedMsg);
                                        while (matcher.find() && matcher.group().length() > word.length()) {
                                            int length = matcher.group().length();
                                            if (pl.ignoreSwear.stream().anyMatch(
                                                Arrays.asList(matcher.group().toLowerCase().split(" "))::contains)) {
                                                regexedMsg = regexedMsg.replace(matcher.group(),
                                                    new String(new char[length]).replace('\0', '*'));
                                                actuallyEdited = true;
                                            }
                                        }
                                    }
                                    String[] words = regexedMsg.split(" ");
                                    StringBuilder c = new StringBuilder("{\"text\":\"");
                                    for (String w : words) {//iterate through all the words in the packet's message
                                        String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""))[0];
                                        if (Bukkit.getPlayer(temp) == null) {
                                            try {
                                                String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                                if (pl.ignoreSwear.contains(testTemp)) {
                                                    continue;
                                                }

                                                List<String> badmul = new ArrayList<>();
                                                List<String> badt = new ArrayList<>();
                                                List<String> bado = new ArrayList<>();
                                                mList.forEach(s -> {
                                                    if (testTemp.contains(s)) {
                                                        badmul.add(s);
                                                    }
                                                });
                                                nomList.forEach(s -> {
                                                    if (testTemp.contains(s)) {
                                                        badt.add(s);
                                                    }
                                                });
                                                oList.forEach(s -> {
                                                    if (testTemp.equalsIgnoreCase(s)) {
                                                        bado.add(s);
                                                    }
                                                });
                                                String bad1 = null;
                                                String bad2 = null;
                                                String bad3 = null;
                                                boolean multiple = false;
                                                if (!(badmul.size() > 1 || badt.size() > 1 || bado.size() > 1 || (
                                                    !badmul.isEmpty()
                                                        && StringUtils.countMatches(testTemp, badmul.get(0)) > 1) || (
                                                    !badt.isEmpty() && StringUtils.countMatches(testTemp, badt.get(0)) > 1))
                                                    || (!bado.isEmpty()
                                                    && StringUtils.countMatches(testTemp, bado.get(0)) > 1)) {
                                                    bad1 = !badmul.isEmpty() ? badmul.get(0) : null;
                                                    bad2 = !badt.isEmpty() ? badt.get(0) : null;
                                                    bad3 = !bado.isEmpty() ? bado.get(0) : null;
                                                } else {
                                                    multiple = true;
                                                }
                                                if (multiple || (bad1 != null && !bad1.isEmpty() && (testTemp.length()
                                                    <= multiplier * bad1.length())) || (bad2 != null
                                                    && testTemp.length() <= bad2.length() + incr) || bad3 != null) {
                                                    actuallyEdited = true;
                                                    continue;
                                                }
                                            } catch (NoSuchElementException ignored) {
                                            }
                                        }
                                        c.append(w).append(" ");
                                    }
                                    String s2 = c.toString();
                                    String s3 = c.toString();
                                    String s4;
                                    if (actuallyEdited) {//only actually resend/etc the packet if we've edited it.
                                        if (s3.endsWith(",")) {
                                            s3 = s3.substring(0, s3.length() - 1);
                                        }
                                        if (s3.endsWith(" ")) {
                                            s3 = s3.substring(0, s3.length() - 1);
                                        }
                                        String message = Json.colourCodeToJson(s3 + "\"}]", "&");
                                        if (message.startsWith("\"},")) {
                                            message = message.substring(3);
                                        }

                                        String sign = message.replace("§", "&").substring(0, message.length() - 1);

                                        s3 = sign;

                                        HashMap<String, Boolean> chatHasStyle = new HashMap<>();
                                        for (String style : new String[] {"bold", "italics", "underline"})
                                            chatHasStyle.put(style, sign.contains("\"" + style + "\":true"));

                                        String[] chatParts = sign.split(",~~,");
                                        int i = 0;
                                        for (String part : chatParts) {
                                            StringBuilder partBuilder =
                                                new StringBuilder(part.substring(0, part.length() - 1));
                                            for (String style : chatHasStyle.keySet())
                                                if (chatHasStyle.get(style) && !partBuilder.toString()
                                                    .contains("\"" + style + "\":")) {
                                                    partBuilder.append(",\"").append(style).append("\":false");
                                                }
                                            part = partBuilder.append("}").toString();
                                            sign = sign.replace(chatParts[i], part);
                                            i++;
                                        }
                                        sign = sign.replace(",~~,", ",");
                                        s4 = sign;
                                        try {
                                            nbt.put(key, "[" + sign + "]");
                                        } catch (Exception ex) {
                                            LOGGER.error("Error Editing Sign Packet. Please report this to GitLab");
                                            LOGGER.error("json: " + json);
                                            LOGGER.error("text: " + text);
                                            LOGGER.error("regexed: " + regexedMsg);
                                            LOGGER.error("s2: " + s2);
                                            LOGGER.error("s3: " + s3);
                                            LOGGER.error("s4: " + s4);
                                            LOGGER.error(ex);
                                        }
                                    }
                                }
                            }
                        }
                        list2.add(converter.getGeneric(nbt));
                    }
                    if (packetEdited) {
                        modifier.write(0, clazz.cast(list2.toArray()));
                    }
                }
                if (e.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                    NbtCompound nbt = (NbtCompound) e.getPacket().getNbtModifier().read(0);
                    Player p = e.getPlayer();
                    if (!p.hasMetadata("swearBlock")) {
                        return;
                    }
                    boolean signEdited = false;
                    for (String key : nbt.getKeys())
                        if (key.contains("Text")) {
                            if (!nbt.getString(key).equals("{\"text\":\"\"}")) {
                                boolean actuallyEdited = false;
                                String json = Json.fromReadJson(nbt.getString(key));
                                String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");
                                List<String> mList = pl.swearList.getList().get("multiplier");
                                List<String> nomList = pl.swearList.getList().get("nomultiplier");
                                List<String> oList = pl.swearList.getList().get("onlymatch");
                                List<String> both = new ArrayList<>();
                                both.addAll(mList);
                                both.addAll(nomList);
                                String regexedMsg = text;
                                for (String word : both) {
                                    StringBuilder regex =
                                        new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0))
                                            .append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                                    for (int i = 1; i < word.length(); i++)
                                        regex.append("\\s*((").append(word.charAt(i)).append("|&[a-fk-o\\d]))+");
                                    Matcher matcher = Pattern.compile(regex.toString()).matcher(regexedMsg);
                                    while (matcher.find() && matcher.group().length() > word.length()) {
                                        int length = matcher.group().length();
                                        if (pl.ignoreSwear.parallelStream()
                                            .anyMatch(Arrays.asList(matcher.group().toLowerCase().split(" "))::contains)) {
                                            regexedMsg = regexedMsg.replace(matcher.group(),
                                                new String(new char[length]).replace('\0', '*'));
                                            actuallyEdited = true;
                                        }
                                    }
                                }
                                String[] words = regexedMsg.split(" ");
                                StringBuilder c = new StringBuilder("{\"text\":\"");
                                for (String w : words) {//iterate through all the words in the packet's message
                                    String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""))[0];
                                    if (Bukkit.getPlayer(temp) == null) {
                                        try {
                                            String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                            if (pl.ignoreSwear.contains(testTemp)) {
                                                continue;
                                            }

                                            //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                            List<String> badmul = pl.swearList.getList().get("multiplier").parallelStream()
                                                .filter(testTemp::contains).toList();
                                            List<String> badt = pl.swearList.getList().get("nomultiplier").parallelStream()
                                                .filter(testTemp::contains).toList();
                                            List<String> bado =
                                                oList.parallelStream().filter(testTemp::equalsIgnoreCase).toList();
                                            String bad1 = null;
                                            String bad2 = null;
                                            String bad3 = null;
                                            boolean multiple = false;
                                            if (!(badmul.size() > 1 || badt.size() > 1 || bado.size() > 1 || (
                                                !badmul.isEmpty() && StringUtils.countMatches(testTemp, badmul.get(0)) > 1)
                                                || (!badt.isEmpty() && StringUtils.countMatches(testTemp, badt.get(0)) > 1))
                                                || (!bado.isEmpty()
                                                && StringUtils.countMatches(testTemp, bado.get(0)) > 1)) {
                                                bad1 = !badmul.isEmpty() ? badmul.get(0) : null;
                                                bad2 = !badt.isEmpty() ? badt.get(0) : null;
                                                bad3 = !bado.isEmpty() ? bado.get(0) : null;
                                            } else {
                                                multiple = true;
                                            }
                                            if (multiple || (bad1 != null && !bad1.isEmpty() && (testTemp.length()
                                                <= multiplier * bad1.length())) || (bad2 != null
                                                && testTemp.length() <= bad2.length() + incr) || bad3 != null) {
                                                c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*"))
                                                    .append(" ");
                                                actuallyEdited = true;
                                                continue;
                                            }
                                        } catch (NoSuchElementException ignored) {
                                        }
                                    }
                                    c.append(w).append(" ");
                                }
                                String s2 = c.toString();
                                String s3 = c.toString();
                                String s4;
                                if (actuallyEdited) {//only actually resend/etc the packet if we've edited it.
                                    if (s3.endsWith(",")) {
                                        s3 = s3.substring(0, s3.length() - 1);
                                    }
                                    if (s3.endsWith(" ")) {
                                        s3 = s3.substring(0, s3.length() - 1);
                                    }
                                    String message = Json.colourCodeToJson(s3 + "\"}]", "&");
                                    if (message.startsWith("\"},")) {
                                        message = message.substring(3);
                                    }

                                    String sign = message.replace("§", "&").substring(0, message.length() - 1);

                                    s3 = sign;

                                    HashMap<String, Boolean> chatHasStyle = new HashMap<>();
                                    for (String style : new String[] {"bold", "italics", "underline"})
                                        chatHasStyle.put(style, sign.contains("\"" + style + "\":true"));

                                    String[] chatParts = sign.split(",~~,");
                                    int i = 0;
                                    for (String part : chatParts) {
                                        StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
                                        for (String style : chatHasStyle.keySet())
                                            if (chatHasStyle.get(style) && !partBuilder.toString()
                                                .contains("\"" + style + "\":")) {
                                                partBuilder.append(",\"").append(style).append("\":false");
                                            }
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
                                        LOGGER.error("Error Editing Sign Packet. Please report this to GitLab");
                                        LOGGER.error("json: " + json);
                                        LOGGER.error("text: " + text);
                                        LOGGER.error("regexed: " + regexedMsg);
                                        LOGGER.error("s2: " + s2);
                                        LOGGER.error("s3: " + s3);
                                        LOGGER.error("s4: " + s4);
                                        LOGGER.error(ex);
                                    }
                                }
                            }
                        }
                    if (signEdited) {
                        e.getPacket().getNbtModifier().write(0, nbt);
                    }
                }
            }
        });
    }

}
