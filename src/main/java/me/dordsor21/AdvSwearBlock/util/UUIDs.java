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
package me.dordsor21.AdvSwearBlock.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UUIDs {

    public UUID getUUIDFromName(String username) {
        OfflinePlayer p = Bukkit.getPlayer(username);
        if (p == null) {
            p = Bukkit.getOfflinePlayer(username);
        }
        return p.getUniqueId();
    }

    String niceUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
