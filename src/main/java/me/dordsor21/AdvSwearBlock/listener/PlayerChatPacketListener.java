package me.dordsor21.AdvSwearBlock.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.dordsor21.AdvSwearBlock.Main;
import me.dordsor21.AdvSwearBlock.util.Json;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerChatPacketListener implements Listener {
    public PlayerChatPacketListener(Main pl) {
        String[] kw = pl.getConfig().getStringList("ignoring.noIgnoringPacketIfContains").toArray(new String[0]);//If the packet contains one of these strings, it cannot be ignored
        double multiplier = pl.getConfig().getDouble("swearing.swearWordMultiplier") >= 1 ? pl.getConfig().getDouble("swearing.swearWordMultiplier") : 1;
        ProtocolManager pM = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, pl);
        pM.addPacketListener(new PacketAdapter(pl, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() == PacketType.Play.Server.CHAT) {
                    Player p = e.getPlayer();
                    UUID puuid = p.getUniqueId();
                    if (p.hasMetadata("swearBlock") || (pl.ignoring && pl.ignore.isIgnorer(puuid))) {
                        boolean actuallyEdited = false;//false unless the chat packet has actually been edited. improves performance and reduces bugs
                        String aRMsg = Json.fromReadJson(e.getPacket().getChatComponents().read(0).getJson());//parse the packet to be nice and readable.
                        String msg = aRMsg;//debug purposes.
                        if (!(msg.contains(",{\"color\":\"gold\",\"text\":\"\"}]") || msg.contains("\"action\":\"run_command\",")
                                || msg.contains(",\"hoverEvent\":{\"action\":\"") || msg.equals("{\"text\":\"\"}"))) {//#circumstances in which we don't want to edit packets
                            String cCMsg = Json.jsonToColourCode(msg.replace("&", "§§"), "&f");//if a player puts &e in chat, it won't make it a colour when converting back to Json
                            msg = cCMsg;
                            String m = Json.stripCodes(msg);
                            if (pl.ignoring && pl.ignore.isIgnorer(puuid)) {//test if packet contains an ignored player's name (SUPER OP)
                                for (String ignoree : pl.ignore.getIgnored(puuid)) {
                                    if (m.toLowerCase().contains(ignoree.toLowerCase()) && Arrays.stream(kw).parallel().noneMatch(m.toLowerCase()::startsWith)) {
                                        e.setCancelled(true);
                                        return;
                                    }
                                }
                            }
                            String[] words = msg.split(" ");
                            StringBuilder c = new StringBuilder("{\"text\":\"");
                            for (String w : words) {//iterate through all the words in the packet's message
                                String temp = Json.stripCodes(w.replaceAll("[^a-zA-Z\\d&_]", ""));
                                if (p.hasMetadata("swearBlock") && !pl.ignoreSwear.contains(temp.toLowerCase()) && Bukkit.getPlayer(temp) == null) {
                                    try {
                                        String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

                                        //Java 8 Streams (very very fast)                          [-this is the important bit-] [-puts all +'ves into a list-]
                                        List<String> bads = pl.swearList.getList().parallelStream().filter(testTemp::contains).collect(Collectors.toList());
                                        String bad = "";
                                        for (String potential : bads)//finds longest swear words of list from above.
                                            if (potential.length() > bad.length())
                                                bad = potential;
                                        if (!bad.equals("") && !bad.isEmpty() && (temp.length() < multiplier * bad.length())) { // a couple of things to reduce false +'ve
                                            if (p.hasMetadata("firstSwear")) {//sends a message on how to toggle if it's the first blocked-swear-word after login.
                                                p.removeMetadata("firstSwear", pl);
                                                Bukkit.getScheduler().runTaskLater(pl, () -> p.sendMessage(pl.messages.get("firstSwear")), 2);
                                            }
                                            c.append(w.replaceAll("(((?<!&)[a-fk-o\\d])|[g-jp-zA-Z_])", "*")).append(" ");
                                            actuallyEdited = true;
                                            continue;
                                        }
                                    } catch (NoSuchElementException ignored) {
                                    }
                                }
                                //Tests for URL so we don't break URLs. Two pieces of regex to catch everything.
                                if (Json.stripCodes(w).matches("^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]"
                                        + "{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$")
                                        || Json.stripCodes(w).matches("(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)")) {
                                    String http = "";
                                    if (!w.contains("http"))
                                        http = "http://";
                                    c.append("\"},~~,{\"clickEvent\":{\"action\":\"open_url\",\"value\":\"").append(http).append(Json.stripCodes(w)).append("\"},\"text\":\"").append(w).append("\"},~~,{\"text\":\" ");
                                    actuallyEdited = true;
                                    continue;
                                }
                                c.append(w).append(" ");
                            }
                            if (actuallyEdited) {//only actually resend/etc the chat packet if we've edited it.
                                if (c.toString().endsWith(","))
                                    c.substring(0, c.length() - 1);
                                if (c.toString().endsWith(" "))
                                    c.substring(0, c.length() - 1);
                                String message = Json.colourCodeToJson(c + "\"}]");
                                if (message.startsWith("\"},"))
                                    message = message.substring(3);

                                String chat = message.replace("§", "&").substring(0, message.length() - 1);

                                HashMap<String, Boolean> chatHasStyle = new HashMap<>();
                                for (String style : new String[]{"bold", "italics", "underline"})
                                    chatHasStyle.put(style, chat.contains("\"" + style + "\":true"));

                                String chatParts[] = chat.split(",~~,");
                                int i = 0;
                                for (String part : chatParts) {
                                    StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
                                    for (String style : chatHasStyle.keySet())
                                        if (chatHasStyle.get(style) && !partBuilder.toString().contains("\"" + style + "\":"))
                                            partBuilder.append(",\"").append(style).append("\":false");
                                    part = partBuilder.append("}").toString();
                                    chat = chat.replace(chatParts[i], part);
                                    i++;
                                }
                                chat = chat.replace(",~~,", ",");
                                try {
                                    e.getPacket().getChatComponents().write(0, WrappedChatComponent.fromJson("[" + chat + ",{\"text\":\"\",\"color\":\"gold\"}]"));
                                } catch (Exception ex) {
                                    pl.getLogger().severe("Error Editing Chat Packet. Please report this to GitLab");
                                    pl.getLogger().severe("Almost Raw " + aRMsg);
                                    pl.getLogger().severe("Colour Code " + cCMsg);
                                    pl.getLogger().severe("Final " + chat);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
