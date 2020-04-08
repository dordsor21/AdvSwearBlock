/*
 *  This file is subject to the terms and conditions defined in
 *  file 'LICENSE.txt', which is part of this source code package.
 *  Original by dordsor21 : https://gitlab.com/dordsor21/AdvSwearBlock/blob/master/LICENSE
 */

package me.dordsor21.AdvSwearBlock.util;

import me.dordsor21.AdvSwearBlock.Main;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Ignore {

    public List<String> cannotIgnore;
    private HashMap<UUID, List<String>> ignoreCache;

    public Ignore(Main plugin) {
        ignoreCache = new HashMap<>();
        cannotIgnore = plugin.sql.noIgnoreList();
    }

    public boolean addIgnorer(UUID uuid, List<String> ignorees) {
        if (ignoreCache.containsKey(uuid))
            return false;
        else {
            ignoreCache.put(uuid, ignorees);
            return true;
        }
    }

    public boolean removeIgnorer(UUID uuid) {
        if (!ignoreCache.containsKey(uuid))
            return false;
        else {
            ignoreCache.remove(uuid);
            return true;
        }
    }

    public boolean editIgnorer(UUID uuid, List<String> ignorees) {
        if (!ignoreCache.containsKey(uuid))
            return false;
        else {
            removeIgnorer(uuid);
            addIgnorer(uuid, ignorees);
            return true;
        }
    }

    public boolean isIgnorer(UUID uuid) {
        return ignoreCache.containsKey(uuid);
    }

    public List<String> getIgnored(UUID uuid) {
        return ignoreCache.getOrDefault(uuid, null);
    }
}
