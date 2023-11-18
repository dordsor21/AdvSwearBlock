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
package me.dordsor21.advswearblock.util;

import me.dordsor21.advswearblock.AdvSwearBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger("AdvSwearBlock/" + SwearList.class.getSimpleName());

    private final Map<String, List<String>> badWords;
    private final FileConfiguration swearFile;
    private final AdvSwearBlock plugin;
    private File file;

    public SwearList(AdvSwearBlock plugin) {
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
            LOGGER.error("Error loading config file", e);
        }
        return swearFile;
    }

    public void add(CommandSender sender, String[] args, String m) {
        try {
            swearFile.load(file);
            String multiplier = "nomultiplier";
            if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier")) {
                multiplier = "multiplier";
            } else if (m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch")) {
                multiplier = "onlymatch";
            }
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

            if (!successes.toString().isEmpty()) {
                sender.sendMessage(plugin.messages.get("badWordAddSuccess")
                    .replace("{{words}}", successes.substring(0, successes.length() - 2)));
            }
            if (!failures.toString().isEmpty()) {
                sender.sendMessage(plugin.messages.get("badWordAddFailure")
                    .replace("{{words}}", failures.substring(0, failures.length() - 2)));
            }
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.error("Error adding swear word to swear list", e);
        }
    }

    public void remove(CommandSender sender, String[] args, String m) {
        try {
            swearFile.load(file);
            String multiplier = "nomultiplier";
            if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier")) {
                multiplier = "multiplier";
            } else if (m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch")) {
                multiplier = "onlymatch";
            }
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

            if (!successes.toString().isEmpty()) {
                sender.sendMessage(plugin.messages.get("badWordRemoveSuccess")
                    .replace("{{words}}", successes.substring(0, successes.length() - 2)));
            }
            if (!failures.toString().isEmpty()) {
                sender.sendMessage(plugin.messages.get("badWordRemoveFailure")
                    .replace("{{words}}", failures.substring(0, failures.length() - 2)));
            }
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.error("Error removing swear word from list", e);
        }
    }

    public void list(CommandSender sender, int page, String m) {
        String multiplier = "nomultiplier";
        if (m.equalsIgnoreCase("m") || m.equalsIgnoreCase("multiplier")) {
            multiplier = "multiplier";
        } else if (m.equalsIgnoreCase("o") || m.equalsIgnoreCase("onlymatch")) {
            multiplier = "onlymatch";
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(badWords.toString());
            return;
        }
        int pageSize = plugin.getConfig().getInt("swearing.listPageSize");
        int pages = (int) Math.ceil((double) badWords.size() / pageSize);
        p.sendMessage(plugin.messages.get("listBadWordsTop").replace("{{count}}", String.valueOf(pageSize))
            .replace("{{total}}", String.valueOf(badWords.size()).replace("{{multiplier}}", multiplier)));
        for (String word : badWords.get(multiplier).subList((page * pageSize) - pageSize, page * pageSize - 1))
            p.sendMessage("   " + word);
        p.sendMessage(plugin.messages.get("listBadWordsBottom").replace("{{page}}", String.valueOf(page))
            .replace("{{pagecount}}", String.valueOf(pages)));
    }

}
