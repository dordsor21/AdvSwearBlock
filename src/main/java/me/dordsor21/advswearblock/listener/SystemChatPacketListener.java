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
import io.papermc.lib.PaperLib;
import me.dordsor21.advswearblock.AdvSwearBlock;
import me.dordsor21.advswearblock.util.Json;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SystemChatPacketListener extends AbstractChatPacketListener {

    private static final Logger LOGGER =
        LogManager.getLogger("AdvSwearBlock/" + SystemChatPacketListener.class.getSimpleName());

    public SystemChatPacketListener(AdvSwearBlock pl, ProtocolManager pM) {
        super(pl, pM);
    }

    @Override
    public void registerListener(ProtocolManager pM) {
        pM.addPacketListener(new SBPacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT));
    }

    private class SBPacketAdapter extends PacketAdapter {

        public SBPacketAdapter(AdvSwearBlock pl, ListenerPriority highest, PacketType packet) {
            super(pl, highest, packet);
        }

        @Override
        public void onPacketSending(PacketEvent e) {
            if (e.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT) {
                return;
            }
            Player p = e.getPlayer();
            UUID puuid = p.getUniqueId();
            if (!p.hasMetadata("swearBlock") && !(pl.ignoring && pl.ignore.isIgnorer(puuid))) {
                return;
            }

            //false unless the chat packet has actually been edited. improves performance and reduces bugs
            boolean actuallyEdited = false;

            var modifier = e.getPacket().getModifier();
            String componentJson = null;
            String strJson = null;
            boolean adventure = false;
            for (int i = 0; i < modifier.size(); i++) {
                Object obj = modifier.read(i);
                if (obj == null) {
                    continue;
                }
                if (PaperLib.isPaper() && obj instanceof Component component) {
                    adventure = true;
                    componentJson = GsonComponentSerializer.gson().serialize(component);
                } else if (obj instanceof String str) {
                    strJson = str;
                }
            }
            // Prefer component
            String json = componentJson != null ? componentJson : strJson;
            if (json == null) {
                if (!pl.failSilent) {
                    LOGGER.error("Could not identify chat component from SystemChatPacket");
                    LOGGER.error(
                        " It is possible this is due to using vanilla chat messaging (currently unsupported due to message signing issues)");
                    LOGGER.error(
                        " Please install a chat-altering plugin such as essentials, or a chat plugin that applies prefixes, etc.");
                    LOGGER.error(
                        " Packet value types: " + modifier.getFields().stream().map(f -> f.getField().getType().getName())
                            .toList());
                }
                return;
            }

            //parse the packet to be nice and readable.
            String aRMsg = Json.fromReadJson(json);

            //#circumstances in which we don't want to edit packets
            if (aRMsg.contains(",{\"color\":\"gold\",\"text\":\"\"}]") || aRMsg.contains("\"action\":\"run_command\",")
                || aRMsg.contains("\"action\":\"suggest_command\",") || aRMsg.contains(",\"hoverEvent\":{\"action\":\"")
                || aRMsg.equals("{\"text\":\"\"}")) {
                return;
            }

            ParseResult result = getResult(e, aRMsg, puuid, actuallyEdited, p);
            if (result == null) {
                return;
            }

            try {
                String finalStr = "[" + result.finalResult() + ",{\"text\":\"\",\"color\":\"gold\"}]";
                if (adventure) {
                    e.getPacket().getModifier().write(0, GsonComponentSerializer.gson().deserialize(finalStr));
                } else {
                    e.getPacket().getModifier().write(0, finalStr);
                }
            } catch (Exception ex) {
                if (pl.failSilent) {
                    return;
                }
                handleError(LOGGER, ex, aRMsg, result);
            }
        }
    }
}
