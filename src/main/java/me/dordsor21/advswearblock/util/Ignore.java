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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Ignore {

    public final List<String> cannotIgnore;
    private final HashMap<UUID, List<String>> ignoreCache;

    public Ignore(AdvSwearBlock plugin) {
        ignoreCache = new HashMap<>();
        cannotIgnore = plugin.sql.noIgnoreList();
    }

    public void setIgnorer(UUID uuid, List<String> ignorees) {
        ignoreCache.put(uuid, ignorees);
    }

    public void removeIgnorer(UUID uuid) {
        ignoreCache.remove(uuid);
    }

    public boolean isIgnorer(UUID uuid) {
        return ignoreCache.containsKey(uuid);
    }

    public List<String> getIgnored(UUID uuid) {
        return ignoreCache.getOrDefault(uuid, null);
    }
}
