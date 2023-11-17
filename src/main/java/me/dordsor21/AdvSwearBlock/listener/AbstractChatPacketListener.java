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

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;
import me.dordsor21.AdvSwearBlock.AdvSwearBlock;
import me.dordsor21.AdvSwearBlock.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractChatPacketListener implements Listener {

    protected static final String[] styles = new String[] {"bold", "italics", "underline"};

    protected final AdvSwearBlock pl;
    protected final String[] kw;
    protected final List<String> mList;
    protected final List<String> nomList;
    protected final List<String> oList;
    protected final HashMap<String, Pattern> allPatterns;
    protected final double multiplier;
    protected final int incr;

    public AbstractChatPacketListener(AdvSwearBlock pl, ProtocolManager pM) {
        this.pl = pl;

        //If the packet contains one of these strings, it cannot be ignored
        kw = pl.getConfig().getStringList("ignoring.noIgnoringPacketIfContains").toArray(new String[0]);
        multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ?
            pl.getConfig().getDouble("swearing.swearWordMultiplier") :
            1;
        incr = pl.getConfig().getInt("swearing.noMultiplierIncrement");
        mList = pl.swearList.getList().get("multiplier");
        nomList = pl.swearList.getList().get("nomultiplier");
        oList = pl.swearList.getList().get("onlymatch");
        List<String> all = new ArrayList<>();
        all.addAll(mList);
        all.addAll(nomList);
        allPatterns = new HashMap<>();
        for (String word : all) {
            StringBuilder regex = new StringBuilder("((?<=[a-z\\d])|(^|(?<=\\s)))").append(word.charAt(0));
            for (int i = 1; i < word.length() - 1; i++) {
                regex.append("(\\s{0,1}|").append(word.charAt(i)).append(")+");
            }
            regex.append("(\\s{0,1}").append(word.charAt(word.length() - 1)).append(")");
            allPatterns.put(word, Pattern.compile(regex.toString()));
        }

        Bukkit.getPluginManager().registerEvents(this, pl);
        registerListener(pM);
    }

    abstract void registerListener(ProtocolManager pM);

    protected record ParseResult(String msg, String cCMsg, String s1, String chat, String chatt) {
    }

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
                for (String k : kw) {
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

        for (Map.Entry<String, Pattern> patternEntry : allPatterns.entrySet()) {
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

        //iterate through all the words in the packet's message
        for (String w : words) {
            String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""))[0];
            String[] strip = Json.stripCodes(w);
            String sw = strip[0];
            if (!p.hasMetadata("swearBlock") || pl.ignoreSwear.contains(temp.toLowerCase())
                || Bukkit.getPlayer(temp) != null) {
                c.append(w).append(" ");
                continue;
            }
            String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

            if (pl.ignoreSwear.contains(testTemp)) {
                c.append(w).append(" ");
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
            if (!(badmul.size() > 1 || badt.size() > 1 || bado.size() > 1 || (!badmul.isEmpty()
                && StringUtils.countMatches(testTemp, badmul.get(0)) > 1) || (!badt.isEmpty()
                && StringUtils.countMatches(testTemp, badt.get(0)) > 1)) || (!bado.isEmpty()
                && StringUtils.countMatches(testTemp, bado.get(0)) > 1)) {
                bad1 = !badmul.isEmpty() ? badmul.get(0) : null;
                bad2 = !badt.isEmpty() ? badt.get(0) : null;
                bad3 = !bado.isEmpty() ? bado.get(0) : null;
            } else {
                multiple = true;
            }
            if (multiple || (bad1 != null && !bad1.isEmpty() && (testTemp.length() <= multiplier * bad1.length())) || (
                bad2 != null && testTemp.length() <= bad2.length() + incr) || bad3 != null) {
                c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*")).append(" ");
                actuallyEdited = true;
                continue;
            }

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

        String s1 = c.toString();
        if (s1.endsWith(",")) {
            s1 = s1.substring(0, c.length() - 1);
        }
        if (s1.endsWith(" ")) {
            s1 = s1.substring(0, c.length() - 1);
        }
        String message = Json.colourCodeToJson(s1 + "\"}]", "&");
        if (message.startsWith("\"},")) {
            message = message.substring(3);
        }

        String chat = message.replace("§", "&").substring(0, message.length() - 1);
        String chatt = chat;

        HashMap<String, Boolean> chatHasStyle = new HashMap<>();
        for (String style : styles)
            chatHasStyle.put(style, chat.contains("\"" + style + "\":true"));
        String[] chatParts = chat.split(",~~,");
        int i = 0;
        for (String part : chatParts) {
            StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
            for (String style : chatHasStyle.keySet())
                if (chatHasStyle.get(style) && !partBuilder.toString().contains("\"" + style + "\":")) {
                    partBuilder.append(",\"").append(style).append("\":false");
                }
            part = partBuilder.append("}").toString();
            chat = chat.replace(chatParts[i], part);
            i++;
        }
        chat = chat.replace(",~~,", ",");
        return new ParseResult(msg, cCMsg, s1, chat, chatt);
    }

    protected void handleError(Logger logger, Exception ex, String aRMsg, ParseResult result) {
        logger.error("Error Editing Chat Packet. Please report this to GitLab");
        logger.error("Almost Raw " + aRMsg);
        logger.error("Colour Code " + result.cCMsg());
        logger.error("Regexed " + result.msg());
        logger.error("s1 " + result.s1());
        logger.error("chatt " + result.chatt());
        logger.error("Final " + result.chat());
        logger.error(ex);
    }
}
