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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public final class SwearBlocker {

    public static final String[] STYLES = new String[] {"bold", "italics", "underline"};

    public final AdvSwearBlock pl;
    public final String[] kw;
    public final List<String> mList;
    public final List<String> nomList;
    public final List<String> oList;
    public final HashMap<String, Pattern> allPatterns;
    public final double multiplier;
    public final int incr;

    public SwearBlocker(AdvSwearBlock pl) {
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
    }

    @NotNull
    public static FinaliseResult finalise(StringBuilder c) {
        String subbed = c.toString();
        if (subbed.endsWith(",")) {
            subbed = subbed.substring(0, c.length() - 1);
        }
        if (subbed.endsWith(" ")) {
            subbed = subbed.substring(0, c.length() - 1);
        }
        String message = Json.colourCodeToJson(subbed + "\"}]", "&");
        if (message.startsWith("\"},")) {
            message = message.substring(3);
        }

        String result = message.replace("§§", "&").substring(0, message.length() - 1);
        String mid = result;

        HashMap<String, Boolean> chatHasStyle = new HashMap<>();
        for (String style : STYLES) {
            chatHasStyle.put(style, result.contains("\"" + style + "\":true"));
        }
        String[] chatParts = result.split(",~~,");
        int i = 0;
        for (String part : chatParts) {
            StringBuilder partBuilder = new StringBuilder(part.substring(0, part.length() - 1));
            for (String style : chatHasStyle.keySet())
                if (chatHasStyle.get(style) && !partBuilder.toString().contains("\"" + style + "\":")) {
                    partBuilder.append(",\"").append(style).append("\":false");
                }
            part = partBuilder.append("}").toString();
            result = result.replace(chatParts[i], part);
            i++;
        }
        return new FinaliseResult(subbed, mid, result);
    }

    public Result removeSwears(String w, String temp, StringBuilder c) {
        String testTemp = temp.replaceAll("\\d", "").replace("_", "").toLowerCase();

        if (pl.ignoreSwear.contains(testTemp)) {
            c.append(w).append(" ");
            return Result.SKIPPED;
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
            return Result.EDITED;
        }
        return Result.FALLTHROUGH;
    }

    public record FinaliseResult(String subbed, String mid, String result) {
    }


    public enum Result {
        SKIPPED, EDITED, FALLTHROUGH
    }
}
