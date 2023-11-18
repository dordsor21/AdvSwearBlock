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

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;
import me.dordsor21.advswearblock.AdvSwearBlock;
import me.dordsor21.advswearblock.util.Json;
import me.dordsor21.advswearblock.util.SwearBlocker;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractChatPacketListener implements Listener {

    protected final AdvSwearBlock pl;

    public AbstractChatPacketListener(AdvSwearBlock pl, ProtocolManager pM) {
        this.pl = pl;
        Bukkit.getPluginManager().registerEvents(this, pl);
        registerListener(pM);
    }

    abstract void registerListener(ProtocolManager pM);

    @Nullable
    protected ParseResult getResult(PacketEvent e, String msg, UUID puuid, boolean actuallyEdited, Player p) {
        //if a player puts &e in chat, it won't make it a colour when converting back to Json
        String cCMsg = Json.jsonToColourCode(msg.replace("&", "§§"), "&f");
        msg = cCMsg;
        String m = Json.stripCodes(msg)[0];
        String mLower = m.toLowerCase();

        //test if packet contains an ignored player's name (SUPER OP)
        if (pl.ignoring && pl.ignore.isIgnorer(puuid)) {
            for (String ignoree : pl.ignore.getIgnored(puuid)) {
                boolean b = false;
                if (!mLower.contains(ignoree)) {
                    continue;
                }
                for (String k : pl.swearBlocker.kw) {
                    b = mLower.startsWith(k);
                    if (b) {
                        break;
                    }
                }
                if (!b) {
                    e.setCancelled(true);
                    return null;
                }
            }
        }

        for (Map.Entry<String, Pattern> patternEntry : pl.swearBlocker.allPatterns.entrySet()) {
            Matcher matcher = patternEntry.getValue().matcher(msg);
            while (matcher.find() && matcher.group().length() > patternEntry.getKey().length()) {
                int length = matcher.group().length();
                boolean yes = false;
                for (String noswear : pl.ignoreSwearArray) {
                    for (String s : matcher.group().toLowerCase().split(" ")) {
                        yes = !noswear.contains(s);
                        if (yes) {
                            break;
                        }
                    }
                    if (yes) {
                        break;
                    }
                }
                if (yes) {
                    msg = msg.replaceAll(matcher.group(), StringUtils.repeat("*", length));
                    actuallyEdited = true;
                }
            }
        }

        String[] words = msg.split(" ");
        StringBuilder c = new StringBuilder("{\"text\":\"");

        boolean isBlocking = p.hasMetadata("swearBlock");

        //iterate through all the words in the packet's message
        for (String w : words) {
            String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""))[0];
            if (!isBlocking || pl.ignoreSwear.contains(temp.toLowerCase()) || Bukkit.getPlayer(temp) != null) {
                c.append(w).append(" ");
                continue;
            }
            SwearBlocker.Result result = pl.swearBlocker.removeSwears(w, temp, c);
            if (result != SwearBlocker.Result.FALLTHROUGH) {
                actuallyEdited |= result == SwearBlocker.Result.EDITED;
                continue;
            }

            String[] strip = Json.stripCodes(w);
            String sw = strip[0];
            //Tests for URL so we don't break URLs. Two pieces of regex to catch everything.
            //Tests for URL so we don't break URLs. Two pieces of regex to catch everything.
            String colour1 = strip.length == 3 ? strip[1] : "";
            String colour2 = strip.length == 3 ? strip[2] : "";
            if (sw.matches("^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]"
                + "{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$") || sw.matches(
                "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)")) {
                String http = "";
                if (!w.contains("http")) {
                    http = "https://";
                }
                c.append("\"},~~,{\"clickEvent\":{\"action\":\"open_url\",\"value\":\"").append(http).append(sw)
                    .append("\"},").append(Json.singleJson(colour1)).append("\"text\":\"").append(sw)
                    .append("\"},~~,{\"text\":\"").append(" ").append(colour2);
                continue;
            }
            c.append(w).append(" ");
        }

        //only actually resend/etc the chat packet if we've edited it.
        if (!actuallyEdited) {
            return null;
        }

        SwearBlocker.FinaliseResult result = SwearBlocker.finalise(c);
        String chat = result.result().replace(",~~,", ",");
        return new ParseResult(msg, cCMsg, result.subbed(), result.mid(), chat);
    }

    protected void handleError(Logger logger, Exception ex, String aRMsg, ParseResult result) {
        logger.error("Error Editing Chat Packet. Please report this to GitLab");
        logger.error("Almost Raw " + aRMsg);
        logger.error("Colour Code " + result.cCMsg());
        logger.error("Regexed " + result.msg());
        logger.error("subbed " + result.subbed());
        logger.error("mid " + result.finaliseMid());
        logger.error("Final " + result.finalResult());
        logger.error(ex);
    }


    protected record ParseResult(String msg, String cCMsg, String subbed, String finaliseMid, String finalResult) {
    }
}
