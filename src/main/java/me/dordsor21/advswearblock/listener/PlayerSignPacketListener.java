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
package me.dordsor21.advswearblock.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import me.dordsor21.advswearblock.AdvSwearBlock;
import me.dordsor21.advswearblock.util.Json;
import me.dordsor21.advswearblock.util.SwearBlocker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerSignPacketListener implements Listener {

    private static final Logger LOGGER =
        LogManager.getLogger("AdvSwearBlock/" + PlayerSignPacketListener.class.getSimpleName());

    private static final Field TILE_ENTITY_TAG;

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
        TILE_ENTITY_TAG = tileEntityTagField;
    }

    private final AdvSwearBlock pl;

    public PlayerSignPacketListener(AdvSwearBlock pl, ProtocolManager pM) {
        this.pl = pl;

        Bukkit.getPluginManager().registerEvents(this, pl);

        pM.addPacketListener(new PacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK,
            PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent e) {
                Player p = e.getPlayer();
                if (!p.hasMetadata("swearBlock")) {
                    return;
                }
                if (e.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
                    var structures1 = e.getPacket().getStructures();
                    var structures2 = structures1.read(0).getStructures();
                    var modifier = structures2.read(2).getModifier();
                    if (modifier.size() == 0) {
                        return;
                    }

                    Object[] beArr = (Object[]) modifier.read(0);

                    var converter = BukkitConverters.getNbtConverter();
                    for (Object obj : beArr) {
                        if (obj == null) {
                            continue;
                        }
                        NbtCompound compound;
                        try {
                            compound = (NbtCompound) converter.getSpecific(TILE_ENTITY_TAG.get(obj));
                            if (compound == null) {
                                continue;
                            }
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                        if (editSign(compound)) {
                            try {
                                TILE_ENTITY_TAG.set(obj, converter.getGeneric(compound));
                            } catch (IllegalAccessException ex) {
                                if (!pl.failSilent) {
                                    LOGGER.error("Error editing sign", ex);
                                }
                            }
                        }
                    }
                } else if (e.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                    var modifier = e.getPacket().getNbtModifier();
                    NbtCompound nbt = (NbtCompound) modifier.read(0);
                    if (editSign(nbt)) {
                        modifier.write(0, nbt);
                    }
                }
            }
        });
    }

    private boolean editSign(NbtCompound compound) {
        boolean edited = false;
        for (String parentKey : compound.getKeys()) {
            if (!"front_text".equals(parentKey) && !"back_text".equals(parentKey)) {
                continue;
            }
            List<NbtBase<Object>> strings = compound.getCompound(parentKey).getList("messages").getValue();
            for (NbtBase<Object> line : strings) {
                if (line.getValue().equals("{\"text\":\"\"}")) {
                    continue;
                }
                boolean actuallyEdited = false;
                String json = (String) line.getValue();
                if (json.startsWith("{\"extra\":[")) {
                    String[] split = json.split("],\"text\":\"");
                    String start = split[1].replaceAll("\"}$", "");
                    json = split[0].replace("{\"extra\":[{", "[{\"text\":\"" + start + "\"},{") + "]";
                }
                String text = Json.jsonToColourCode(json.replace("&", "§§"), "&0");

                String regexedMsg = text;
                for (String word : pl.swearBlocker.allPatterns.keySet()) {
                    StringBuilder regex = new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0))
                        .append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                    for (int j = 1; j < word.length(); j++) {
                        regex.append("\\s*((").append(word.charAt(j)).append("|&[a-fk-o\\d]))+");
                    }
                    Matcher matcher = Pattern.compile(regex.toString()).matcher(regexedMsg);
                    while (matcher.find() && matcher.group().length() > word.length()) {
                        int length = matcher.group().length();
                        if (pl.ignoreSwear.stream()
                            .anyMatch(Arrays.asList(matcher.group().toLowerCase().split(" "))::contains)) {
                            regexedMsg =
                                regexedMsg.replace(matcher.group(), new String(new char[length]).replace('\0', '*'));
                            actuallyEdited = true;
                        }
                    }
                }
                String[] words = regexedMsg.split(" ");
                StringBuilder c = new StringBuilder("{\"text\":\"");
                for (String w : words) {
                    String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""))[0];
                    if (Bukkit.getPlayer(temp) != null) {
                        c.append(w).append(" ");
                        continue;
                    }
                    try {
                        SwearBlocker.Result result = pl.swearBlocker.removeSwears(w, temp, c);
                        if (result == SwearBlocker.Result.EDITED) {
                            actuallyEdited = true;
                        } else if (result == SwearBlocker.Result.FALLTHROUGH) {
                            c.append(w).append(" ");
                        }
                    } catch (NoSuchElementException ignored) {
                    }
                }
                //only actually resend/etc the packet if we've edited it.
                if (!actuallyEdited) {
                    continue;
                }

                SwearBlocker.FinaliseResult result = SwearBlocker.finalise(c);
                String sign =
                    result.result().replace(",~~,", ",").replace("{\"text\":\"\"}", "").replaceAll("(^,+)|(,+$)", "");
                String finalStr = "{\"extra\":[" + sign + "],\"text\":\"\"}";
                try {
                    line.setValue(finalStr);
                    edited = true;
                } catch (Exception ex) {
                    if (!pl.failSilent) {
                        LOGGER.error("Error Editing Sign Packet. Please report this to GitLab");
                        LOGGER.error("json: " + json);
                        LOGGER.error("text: " + text);
                        LOGGER.error("regexed: " + regexedMsg);
                        LOGGER.error("subbed " + result.subbed());
                        LOGGER.error("mid " + result.mid());
                        LOGGER.error("final: " + sign);
                        LOGGER.error(ex);
                    }
                }
            }
        }
        return edited;
    }

}
