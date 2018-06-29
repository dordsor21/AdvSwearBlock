/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.util;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwearList {

    private Map<String, List<String>> badWords;

    private File file;
    private FileConfiguration swearFile;

    private Main plugin;

    public SwearList(Main plugin) {
        this.plugin = plugin;

        swearFile = fileExists();

        badWords = new HashMap<>();

        badWords.put("multiplier", swearFile.getStringList("multiplier"));
        badWords.put("nomultiplier", swearFile.getStringList("nomultiplier"));
        badWords.put("onlymatch", swearFile.getStringList("onlymatch"));

    }

    public Map<String, List<String>> getList() {
        return badWords;
    }

    private FileConfiguration fileExists() {
        file = new File(plugin.getDataFolder(), "swearlist.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("swearlist.yml", false);
        }

        FileConfiguration swearFile = new YamlConfiguration();

        try {
            swearFile.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return swearFile;
    }

    public void add(CommandSender sender, String[] args, String m) {
        try {
            swearFile.load(file);
            String multiplier = "nomultiplier";
            if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier"))
                multiplier = "multiplier";
            else if(m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch"))
                multiplier = "onlymatch";
            StringBuilder successes = new StringBuilder();
            StringBuilder failures = new StringBuilder();
            List<String> words = badWords.get(multiplier);
            for (String arg : args) {
                if (!words.contains(arg)) {
                    words.add(arg);
                    successes.append(arg).append(", ");
                    continue;
                }
                failures.append(arg).append(", ");
            }
            badWords.replace(multiplier, words);
            swearFile.set(multiplier, words);
            swearFile.save(file);

            if (!successes.toString().isEmpty())
                sender.sendMessage(plugin.messages.get("badWordAddSuccess").replace("{{words}}", successes.substring(0, successes.length() - 2)));
            if (!failures.toString().isEmpty())
                sender.sendMessage(plugin.messages.get("badWordAddFailure").replace("{{words}}", failures.substring(0, failures.length() - 2)));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void remove(CommandSender sender, String[] args, String m) {
        try {
            swearFile.load(file);
            String multiplier = "nomultiplier";
            if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier"))
                multiplier = "multiplier";
            else if(m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch"))
                multiplier = "onlymatch";
            StringBuilder successes = new StringBuilder();
            StringBuilder failures = new StringBuilder();
            List<String> words = badWords.get(multiplier);
            for (String arg : args) {
                if (words.contains(arg)) {
                    words.remove(arg);
                    successes.append(arg).append(", ");
                    continue;
                }
                failures.append(arg).append(", ");
            }
            badWords.replace(multiplier, words);
            swearFile.set(multiplier, words);
            swearFile.save(file);

            if (!successes.toString().isEmpty())
                sender.sendMessage(plugin.messages.get("badWordRemoveSuccess").replace("{{words}}", successes.substring(0, successes.length() - 2)));
            if (!failures.toString().isEmpty())
                sender.sendMessage(plugin.messages.get("badWordRemoveFailure").replace("{{words}}", failures.substring(0, failures.length() - 2)));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void list(CommandSender sender, int page, String m) {
        String multiplier = "nomultiplier";
        if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier"))
            multiplier = "multiplier";
        else if(m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch"))
            multiplier = "onlymatch";
        if (!(sender instanceof Player)) {
            sender.sendMessage(badWords.toString());
            return;
        }
        int pageSize = plugin.getConfig().getInt("swearing.listPageSize");
        Player p = (Player) sender;
        int pages = (int) Math.ceil(badWords.size() / pageSize);
        p.sendMessage(plugin.messages.get("listBadWordsTop").replace("{{count}}", String.valueOf(pageSize)).replace("{{total}}", String.valueOf(badWords.size())
                .replace("{{multiplier}}", multiplier)));
        for (String word : badWords.get(multiplier).subList((page * pageSize) - pageSize, page * pageSize - 1))
            p.sendMessage("   " + word);
        p.sendMessage(plugin.messages.get("listBadWordsBottom").replace("{{page}}", String.valueOf(page)).replace("{{pagecount}}", String.valueOf(pages)));
    }

}
