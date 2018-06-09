package me.dordsor21.AdvSwearBlock.util;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Ignore {

    private HashMap<UUID, List<String>> ignoreCache;

    public Ignore() {
        ignoreCache = new HashMap<>();
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
