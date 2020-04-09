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
import com.comphenix.protocol.wrappers.ComponentConverter;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Json;
import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlayerChatPacketListener implements Listener {

    private String[] styles = new String[] {"bold", "italics", "underline"};
    private Main pl;
    private String[] kw;
    private List<String> mList;
    private List<String> nomList;
    private List<String> oList;
    private List<String> all;
    private double multiplier;
    private int incr;
    private Stream<String> stream;

    public PlayerChatPacketListener(Main pl, ProtocolManager pM) {
        this.pl = pl;

        //If the packet contains one of these strings, it cannot be ignored
        kw = pl.getConfig().getStringList("ignoring.noIgnoringPacketIfContains")
            .toArray(new String[0]);
        multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ?
            pl.getConfig().getDouble("swearing.swearWordMultiplier") :
            1;
        incr = pl.getConfig().getInt("swearing.noMultiplierIncrement");
        mList = pl.swearList.getList().get("multiplier");
        nomList = pl.swearList.getList().get("nomultiplier");
        oList = pl.swearList.getList().get("onlymatch");
        all = new ArrayList<>();
        all.addAll(mList);
        all.addAll(nomList);

        Bukkit.getPluginManager().registerEvents(this, pl);
        pM.addPacketListener(
            new SBPacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT));
    }

    public void reload() {
        multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ?
            pl.getConfig().getDouble("swearing.swearWordMultiplier") :
            1;
        incr = pl.getConfig().getInt("swearing.noMultiplierIncrement");
        mList = pl.swearList.getList().get("multiplier");
        nomList = pl.swearList.getList().get("nomultiplier");
        oList = pl.swearList.getList().get("onlymatch");
        all = new ArrayList<>();
        all.addAll(mList);
        all.addAll(nomList);
        stream = pl.ignoreSwear.stream();
    }

    private class SBPacketAdapter extends PacketAdapter {

        public SBPacketAdapter(Main pl, ListenerPriority highest, PacketType chat) {
            super(pl, highest, chat);
        }

        @Override public void onPacketSending(PacketEvent e) {
            if (e.getPacketType() != PacketType.Play.Server.CHAT) {
                return;
            }
            Player p = e.getPlayer();
            UUID puuid = p.getUniqueId();
            if (!p.hasMetadata("swearBlock") && !(pl.ignoring && pl.ignore.isIgnorer(puuid))) {
                return;
            }

            //false unless the chat packet has actually been edited. improves performance and reduces bugs
            boolean actuallyEdited = false;
            boolean isWcc = true;
            WrappedChatComponent wcc = e.getPacket().getChatComponents().read(0);
            if (wcc == null) {
                wcc = ComponentConverter
                    .fromBaseComponent((BaseComponent[]) e.getPacket().getModifier().read(1));
                isWcc = false;
            }
            String raw = wcc.getJson();

            //parse the packet to be nice and readable.
            String aRMsg = Json.fromReadJson(raw);

            //debug purposes.
            String msg = aRMsg;

            //#circumstances in which we don't want to edit packets
            if (msg.contains(",{\"color\":\"gold\",\"text\":\"\"}]") || msg
                .contains("\"action\":\"run_command\",") || msg
                .contains("\"action\":\"suggest_command\",") || msg
                .contains(",\"hoverEvent\":{\"action\":\"") || msg.equals("{\"text\":\"\"}")) {
                return;
            }

            //if a player puts &e in chat, it won't make it a colour when converting back to Json
            String cCMsg = Json.jsonToColourCode(msg.replace("&", "§§"), "&f");
            msg = cCMsg;
            String m = Json.stripCodes(msg);

            //test if packet contains an ignored player's name (SUPER OP)
            if (pl.ignoring && pl.ignore.isIgnorer(puuid)) {
                for (String ignoree : pl.ignore.getIgnored(puuid)) {
                    if (m.toLowerCase().contains(ignoree.toLowerCase()) && Arrays.stream(kw)
                        .noneMatch(m.toLowerCase()::startsWith)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            for (String word : all) {
                StringBuilder regex =
                    new StringBuilder("((?<=&[a-fk-o\\d])|(^|(?<=\\s)))(").append(word.charAt(0))
                        .append("((&[a-fk-o\\d]))|").append(word.charAt(0)).append(")+");
                for (int i = 1; i < word.length(); i++)
                    regex.append("\\s*((").append(word.charAt(i)).append("|&[a-fk-o\\d]))+");
                Matcher matcher = Pattern.compile(regex.toString()).matcher(msg);
                while (matcher.find() && matcher.group().length() > word.length()) {
                    int length = matcher.group().length();
                    if (stream.anyMatch(
                        Arrays.asList(matcher.group().toLowerCase().split(" "))::contains)) {
                        msg = msg.replace(matcher.group(),
                            new String(new char[length]).replace('\0', '*'));
                        actuallyEdited = true;
                    }
                }
            }
            String[] words = msg.split(" ");
            StringBuilder c = new StringBuilder("{\"text\":\"");

            //iterate through all the words in the packet's message
            for (String w : words) {
                String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                if (p.hasMetadata("swearBlock") && !pl.ignoreSwear.contains(temp.toLowerCase())
                    && Bukkit.getPlayer(temp) == null) {
                    String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                    if (pl.ignoreSwear.contains(testTemp))
                        continue;
                    List<String> badmul = new ArrayList<>();
                    List<String> badt = new ArrayList<>();
                    List<String> bado = new ArrayList<>();
                    mList.forEach(s -> {
                        if (testTemp.contains(s))
                            badmul.add(s);
                    });
                    nomList.forEach(s -> {
                        if (testTemp.contains(s))
                            badt.add(s);
                    });
                    oList.forEach(s -> {
                        if (testTemp.equalsIgnoreCase(s))
                            bado.add(s);
                    });
                    String bad1 = null;
                    String bad2 = null;
                    String bad3 = null;
                    boolean multiple = false;
                    if (!(badmul.size() > 1 || badt.size() > 1 || bado.size() > 1 || (
                        badmul.size() > 0 && StringUtils.countMatches(testTemp, badmul.get(0)) > 1)
                        || (badt.size() > 0 && StringUtils.countMatches(testTemp, badt.get(0)) > 1))
                        || (bado.size() > 0
                        && StringUtils.countMatches(testTemp, bado.get(0)) > 1)) {
                        bad1 = badmul.size() > 0 ? badmul.get(0) : null;
                        bad2 = badt.size() > 0 ? badt.get(0) : null;
                        bad3 = bado.size() > 0 ? bado.get(0) : null;
                    } else {
                        multiple = true;
                    }
                    if (multiple || (bad1 != null && !bad1.equals("") && !bad1.isEmpty() && (
                        testTemp.length() <= multiplier * bad1.length())) || (bad2 != null
                        && testTemp.length() <= bad2.length() + incr) || bad3 != null) {
                        c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*"))
                            .append(" ");
                        actuallyEdited = true;
                        continue;
                    }
                }

                //Tests for URL so we don't break URLs. Two pieces of regex to catch everything.
                if (Json.stripCodes(w).matches(
                    "^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]"
                        + "{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$") || Json
                    .stripCodes(w).matches(
                        "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)")) {
                    String http = "";
                    if (!w.contains("http"))
                        http = "http://";
                    c.append("\"},~~,{\"clickEvent\":{\"action\":\"open_url\",\"value\":\"")
                        .append(http).append(Json.stripCodes(w)).append("\"},\"text\":\"").append(w)
                        .append("\"},~~,{\"text\":\" ");
                    continue;
                }
                c.append(w).append(" ");
            }

            //only actually resend/etc the chat packet if we've edited it.
            if (!actuallyEdited)
                return;

            String s1 = c.toString();
            if (s1.endsWith(","))
                s1 = s1.substring(0, c.length() - 1);
            if (s1.endsWith(" "))
                s1 = s1.substring(0, c.length() - 1);
            String message = Json.colourCodeToJson(s1 + "\"}]", "&");
            if (message.startsWith("\"},"))
                message = message.substring(3);

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
                    if (chatHasStyle.get(style) && !partBuilder.toString()
                        .contains("\"" + style + "\":"))
                        partBuilder.append(",\"").append(style).append("\":false");
                part = partBuilder.append("}").toString();
                chat = chat.replace(chatParts[i], part);
                i++;
            }
            chat = chat.replace(",~~,", ",");
            try {
                if (isWcc)
                    e.getPacket().getChatComponents().write(0, WrappedChatComponent
                        .fromJson("[" + chat + ",{\"text\":\"\",\"color\":\"gold\"}]"));
                else
                    e.getPacket().getModifier().write(1, ComponentConverter.fromWrapper(
                        WrappedChatComponent
                            .fromJson("[" + chat + ",{\"text\":\"\",\"color\":\"gold\"}]")));
            } catch (Exception ex) {
                if (pl.failSilent) {
                    return;
                }
                pl.getLogger().severe("Error Editing Chat Packet. Please report this to GitLab");
                pl.getLogger().severe("Almost Raw " + aRMsg);
                pl.getLogger().severe("Colour Code " + cCMsg);
                pl.getLogger().severe("Regexed " + msg);
                pl.getLogger().severe("s1 " + s1);
                pl.getLogger().severe("chatt " + chatt);
                pl.getLogger().severe("Final " + chat);
            }
        }

    }
}
