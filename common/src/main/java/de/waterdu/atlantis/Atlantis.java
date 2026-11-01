package de.waterdu.atlantis;

import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.self.AtlantisListeners;
import de.waterdu.atlantis.util.concurrency.AtlantisThreadPool;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * <h1>Atlantis</h1>
 * <p>
 * A collection of useful tools for mod plugin developers. It includes a sub-modloader that makes development simpler.
 * You can use it as an external library, shade it in, or copy from it directly.
 * It also provides meta and convenience functions that make server administration easier.
 * <p>
 * ---
 * <p>
 * Atlantis has a dual license: GNU LGPL v3 and MIT.
 * The license text for both is below and in LICENSE.md.
 * You can choose the license that applies to your use case.
 * <p>
 * ---
 * <p>
 * GNU LGPL v3:
 * <p>
 * Atlantis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Atlantis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with Atlantis. If not, see <a href="https://www.gnu.org/licenses/lgpl.txt">LGPL v3</a>.
 * <p>
 * ---
 * <p>
 * MIT:
 * <p>
 * Copyright 2019-2026 Waterdude Studios Ltd
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p>
 * ---
 * <p>
 * Previously AquaAPI.
 * <p>
 * ---
 * <p>
 * @author Copyright (C) 2019-2026 Atlantis Development (Waterdude Studios Ltd), sam@waterdu.de
 * @version 1.0
 * @since 02/03/2019
 */
public class Atlantis {

    /**
     * Mod ID. Atlantis also uses it as the namespace for its own config files and keys.
     */
    public static final String ID = BuildInfo.ID;
    public static final String NAME = BuildInfo.NAME;
    public static final String VERSION = BuildInfo.VERSION;
    /**
     * Username of the Atlantis author account. {@link #isPlayerWater(String)} matches it case-insensitively.
     */
    public static final String WATER_NAME = "Waterdude";
    /**
     * {@link #WATER_NAME} as an author list, for APIs that take one.
     */
    public static final String[] WATER_NAME_AS_ARRAY = new String[] { WATER_NAME };
    /**
     * Logged when a task submitted to an {@link AtlantisThreadPool} throws.
     */
    public static final String ERROR_TEXT = "An error occurred in Atlantis";
    /**
     * Sent to players when an action fails. Builds a new {@link Component} on every call.
     */
    public static final Supplier<Component> ERROR_MESSAGE = () -> Text.of("&cAn error occurred in Atlantis!").compile();

    /**
     * Size of {@link #THREAD_POOL}, and the minimum thread count for data storage.
     */
    public static final int DEFAULT_THREADS = 4;
    /**
     * Shared pool of daemon threads for async work. The pool catches and logs errors that tasks throw.
     */
    public static final AtlantisThreadPool THREAD_POOL = AtlantisThreadPool.createNewThreadPool(NAME, DEFAULT_THREADS, true);
    /**
     * Access to Atlantis' own config and data files.
     */
    public static final AtlantisConfigProxy.Impl CONFIG = AtlantisConfigProxy.of(ID);

    private static final Lazy<AtlantisListeners> LISTENERS = Lazy.of(AtlantisListeners::new);

    /**
     * The first call creates the listeners and registers them through {@code Mods.modLoader()}.
     * Do not call this before {@code Mods} has bootstrapped.
     *
     * @return                  Atlantis' own event listeners.
     */
    public static AtlantisListeners listeners() {
        return LISTENERS.get();
    }

    /**
     * @param       player      The player to check. May be null.
     * @return                  Whether the player's username is {@link #WATER_NAME}, ignoring case.
     */
    public static boolean isPlayerWater(Player player) {
        if (player == null) {
            return false;
        }
        var name = player.getGameProfile().getName();
        return name != null && name.equalsIgnoreCase(WATER_NAME);
    }

    /**
     * @param       player      The player to check. May be null.
     * @return                  Whether the player's username is {@link #WATER_NAME}, ignoring case.
     */
    public static boolean isPlayerWater(ServerPlayer player) {
        return isPlayerWater((Player) player);
    }

    /**
     * Always false, because no UUID is registered for the author account. Use a name-based overload instead.
     *
     * @param       uuid        The UUID to check.
     * @return                  false.
     */
    public static boolean isPlayerWater(UUID uuid) {
        return false;
    }

    /**
     * @param       name        The username to check. May be null.
     * @return                  Whether the username is {@link #WATER_NAME}, ignoring case.
     */
    public static boolean isPlayerWater(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).equals(WATER_NAME.toLowerCase(Locale.ROOT));
    }
}
