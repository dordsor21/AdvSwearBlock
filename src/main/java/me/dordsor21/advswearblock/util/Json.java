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

import org.apache.commons.lang3.StringUtils;

public class Json {


    private static final String[] jsonColour =
        new String[] {"[{\"text\":\"", "[{\"color\":\"black\",\"text\":\"", "[{\"color\":\"dark_blue\",\"text\":\"",
            "[{\"color\":\"dark_green\",\"text\":\"", "[{\"color\":\"dark_aqua\",\"text\":\"",
            "[{\"color\":\"dark_red\",\"text\":\"", "[{\"color\":\"dark_purple\",\"text\":\"",
            "[{\"color\":\"gold\",\"text\":\"", "[{\"color\":\"gray\",\"text\":\"", "[{\"color\":\"dark_gray\",\"text\":\"",
            "[{\"color\":\"blue\",\"text\":\"", "[{\"color\":\"green\",\"text\":\"", "[{\"color\":\"aqua\",\"text\":\"",
            "[{\"color\":\"red\",\"text\":\"", "[{\"color\":\"light_purple\",\"text\":\"",
            "[{\"color\":\"yellow\",\"text\":\"", "[{\"color\":\"white\",\"text\":\"", "[{\"obfuscated\":true,\"text\":\"",
            "[{\"bold\":true,\"text\":\"", "[{\"strikethrough\":true,\"text\":\"", "[{\"underlined\":true,\"text\":\"",
            "[{\"italic\":true,\"text\":\"", "[{\"obfuscated\":false,\"text\":\"", "[{\"bold\":false,\"text\":\"",
            "[{\"strikethrough\":false,\"text\":\"", "[{\"underlined\":false,\"text\":\"", "[{\"italic\":false,\"text\":\"",
            "[{\"color\":\"black\",", "[{\"color\":\"dark_blue\",", "[{\"color\":\"dark_green\",",
            "[{\"color\":\"dark_aqua\",", "[{\"color\":\"dark_red\",", "[{\"color\":\"dark_purple\",",
            "[{\"color\":\"gold\",", "[{\"color\":\"gray\",", "[{\"color\":\"dark_gray\",", "[{\"color\":\"blue\",",
            "[{\"color\":\"green\",", "[{\"color\":\"aqua\",", "[{\"color\":\"red\",", "[{\"color\":\"light_purple\",",
            "[{\"color\":\"yellow\",", "[{\"color\":\"white\",", "[{\"obfuscated\":true,", "[{\"bold\":true,",
            "[{\"strikethrough\":true,", "[{\"underlined\":true,", "[{\"italic\":true,", "[{\"obfuscated\":false,",
            "[{\"bold\":false,", "[{\"strikethrough\":false,", "[{\"underlined\":false,", "[{\"italic\":false,",
            "{\"color\":\"black\",\"text\":\"", "{\"color\":\"dark_blue\",\"text\":\"",
            "{\"color\":\"dark_green\",\"text\":\"", "{\"color\":\"dark_aqua\",\"text\":\"",
            "{\"color\":\"dark_red\",\"text\":\"", "{\"color\":\"dark_purple\",\"text\":\"",
            "{\"color\":\"gold\",\"text\":\"", "{\"color\":\"gray\",\"text\":\"", "{\"color\":\"dark_gray\",\"text\":\"",
            "{\"color\":\"blue\",\"text\":\"", "{\"color\":\"green\",\"text\":\"", "{\"color\":\"aqua\",\"text\":\"",
            "{\"color\":\"red\",\"text\":\"", "{\"color\":\"light_purple\",\"text\":\"", "{\"color\":\"yellow\",\"text\":\"",
            "{\"color\":\"white\",\"text\":\"", "{\"obfuscated\":true,\"text\":\"", "{\"bold\":true,\"text\":\"",
            "{\"strikethrough\":true,\"text\":\"", "{\"underlined\":true,\"text\":\"", "{\"italic\":true,\"text\":\"",
            "{\"obfuscated\":false,\"text\":\"", "{\"bold\":false,\"text\":\"", "{\"strikethrough\":false,\"text\":\"",
            "{\"underlined\":false,\"text\":\"", "{\"italic\":false,\"text\":\"", "{\"color\":\"black\",",
            "{\"color\":\"dark_blue\",", "{\"color\":\"dark_green\",", "{\"color\":\"dark_aqua\",",
            "{\"color\":\"dark_red\",", "{\"color\":\"dark_purple\",", "{\"color\":\"gold\",", "{\"color\":\"gray\",",
            "{\"color\":\"dark_gray\",", "{\"color\":\"blue\",", "{\"color\":\"green\",", "{\"color\":\"aqua\",",
            "{\"color\":\"red\",", "{\"color\":\"light_purple\",", "{\"color\":\"yellow\",", "{\"color\":\"white\",",
            "{\"obfuscated\":true,", "{\"bold\":true,", "{\"strikethrough\":true,", "{\"underlined\":true,",
            "{\"italic\":true,", "{\"obfuscated\":false,", "{\"bold\":false,", "{\"strikethrough\":false,",
            "{\"underlined\":false,", "{\"italic\":false,", "\"color\":\"black\",\"text\":\"",
            "\"color\":\"dark_blue\",\"text\":\"", "\"color\":\"dark_green\",\"text\":\"",
            "\"color\":\"dark_aqua\",\"text\":\"", "\"color\":\"dark_red\",\"text\":\"",
            "\"color\":\"dark_purple\",\"text\":\"", "\"color\":\"gold\",\"text\":\"", "\"color\":\"gray\",\"text\":\"",
            "\"color\":\"dark_gray\",\"text\":\"", "\"color\":\"blue\",\"text\":\"", "\"color\":\"green\",\"text\":\"",
            "\"color\":\"aqua\",\"text\":\"", "\"color\":\"red\",\"text\":\"", "\"color\":\"light_purple\",\"text\":\"",
            "\"color\":\"yellow\",\"text\":\"", "\"color\":\"white\",\"text\":\"", "\"obfuscated\":true,\"text\":\"",
            "\"bold\":true,\"text\":\"", "\"strikethrough\":true,\"text\":\"", "\"underlined\":true,\"text\":\"",
            "\"italic\":true,\"text\":\"", "\"obfuscated\":false,\"text\":\"", "\"bold\":false,\"text\":\"",
            "\"strikethrough\":false,\"text\":\"", "\"underlined\":false,\"text\":\"", "\"italic\":false,\"text\":\"",
            "\"color\":\"black\",", "\"color\":\"dark_blue\",", "\"color\":\"dark_green\",", "\"color\":\"dark_aqua\",",
            "\"color\":\"dark_red\",", "\"color\":\"dark_purple\",", "\"color\":\"gold\",", "\"color\":\"gray\",",
            "\"color\":\"dark_gray\",", "\"color\":\"blue\",", "\"color\":\"green\",", "\"color\":\"aqua\",",
            "\"color\":\"red\",", "\"color\":\"light_purple\",", "\"color\":\"yellow\",", "\"color\":\"white\",",
            "\"obfuscated\":true,", "\"bold\":true,", "\"strikethrough\":true,", "\"underlined\":true,", "\"italic\":true,",
            "\"obfuscated\":false,", "\"bold\":false,", "\"strikethrough\":false,", "\"underlined\":false,",
            "\"italic\":false,"};
    private static final String[] replaceJsonColourTo =
        new String[] {"&z", "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
            "&k", "&l", "&m", "&n", "&o", "&z", "&z", "&z", "&z", "&z", "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8",
            "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k", "&l", "&m", "&n", "&o", "&z", "&z", "&z", "&z", "&z", "&0", "&1",
            "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k", "&l", "&m", "&n", "&o",
            "&z", "&z", "&z", "&z", "&z", "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d",
            "&e", "&f", "&k", "&l", "&m", "&n", "&o", "&z", "&z", "&z", "&z", "&z", "&0", "&1", "&2", "&3", "&4", "&5", "&6",
            "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k", "&l", "&m", "&n", "&o", "&z", "&z", "&z", "&z", "&z",
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k", "&l", "&m",
            "&n", "&o", "&z", "&z", "&z", "&z", "&z",};
    private static final String[] codeColour =
        new String[] {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k",
            "&l", "&m", "&n", "&o", "\"text\":\"\"},{"};
    private static final String[] singleJson =
        new String[] {"\"color\":\"black\",", "\"color\":\"dark_blue\",", "\"color\":\"dark_green\",",
            "\"color\":\"dark_aqua\",", "\"color\":\"dark_red\",", "\"color\":\"dark_purple\",", "\"color\":\"gold\",",
            "\"color\":\"gray\",", "\"color\":\"dark_gray\",", "\"color\":\"blue\",", "\"color\":\"green\",",
            "\"color\":\"aqua\",", "\"color\":\"red\",", "\"color\":\"light_purple\",", "\"color\":\"yellow\",",
            "\"color\":\"white\",", "\"obfuscated\":true,", "\"bold\":true,", "\"strikethrough\":true,",
            "\"underlined\":true,", "\"italic\":true,", ""};

    public static String fromReadJson(String input) {
        return input.replace("{\"extra\":", "").replace("],\"text\":\"\"}", "]");
    }

    public static String colourCodeToJson(String msg, String i) {
        return StringUtils.replaceEach(msg,
            new String[] {i + "0", i + "1", i + "2", i + "3", i + "4", i + "5", i + "6", i + "7", i + "8", i + "9", i + "a",
                i + "b", i + "c", i + "d", i + "e", i + "f", i + "k", i + "l", i + "m", i + "n", i + "o",
                "\"text\":\"\"},~~,{", "\"text\":\"\"},{"},
            new String[] {"\"},~~,{\"color\":\"black\",\"text\":\"", "\"},~~,{\"color\":\"dark_blue\",\"text\":\"",
                "\"},~~,{\"color\":\"dark_green\",\"text\":\"", "\"},~~,{\"color\":\"dark_aqua\",\"text\":\"",
                "\"},~~,{\"color\":\"dark_red\",\"text\":\"", "\"},~~,{\"color\":\"dark_purple\",\"text\":\"",
                "\"},~~,{\"color\":\"gold\",\"text\":\"", "\"},~~,{\"color\":\"gray\",\"text\":\"",
                "\"},~~,{\"color\":\"dark_gray\",\"text\":\"", "\"},~~,{\"color\":\"blue\",\"text\":\"",
                "\"},~~,{\"color\":\"green\",\"text\":\"", "\"},~~,{\"color\":\"aqua\",\"text\":\"",
                "\"},~~,{\"color\":\"red\",\"text\":\"", "\"},~~,{\"color\":\"light_purple\",\"text\":\"",
                "\"},~~,{\"color\":\"yellow\",\"text\":\"", "\"},~~,{\"color\":\"white\",\"text\":\"",
                "\"},~~,{\"obfuscated\":true,\"text\":\"", "\"},~~,{\"bold\":true,\"text\":\"",
                "\"},~~,{\"strikethrough\":true,\"text\":\"", "\"},~~,{\"underlined\":true,\"text\":\"",
                "\"},~~,{\"italic\":true,\"text\":\"", "", ""});
    }

    public static String[] stripCodes(String m) {
        String[] ret;
        int length = m.length();
        if (length > 4) {
            ret = new String[3];
            if (m.charAt(0) == '&') {
                ret[1] = m.substring(0, 2);
            }
            if (m.charAt(length - 2) == '&') {
                ret[2] = m.substring(length - 2);
            }
        } else {
            ret = new String[1];
        }
        ret[0] = StringUtils.replaceEach(m,
            new String[] {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
                "&k", "&l", "&m", "&n", "&o"},
            new String[] {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});
        return ret;
    }

    public static String jsonToColourCode(String msg, String reset) {
        return StringUtils.replaceEach(msg.replaceAll("\"clickEvent\":\\{\"action\":\"open_url\",\"value\":\".*?\"},", ""),
                jsonColour, replaceJsonColourTo).replace("[{\"text\":\"", reset).replace("{\"text\":\"", reset)
            .replace("\"},", "").replace("\"}]", "").replace("\"}", "")
            .replaceAll("(?<!(&[0-9a-fz]))(&z)+(?!(&[0-9a-fz]))", "&f").replace("&z", "");
    }

    public static String singleJson(String s) {
        return StringUtils.replaceEach(s, codeColour, singleJson);
    }
}
