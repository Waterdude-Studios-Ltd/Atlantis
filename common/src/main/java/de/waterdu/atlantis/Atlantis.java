package de.waterdu.atlantis;

import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.self.AtlantisListeners;
import de.waterdu.atlantis.util.concurrency.AtlantisThreadPool;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public class Atlantis {

    public static final String ID = "atlantis";
    public static final String MOD_ID = ID;
    public static final String NAME = "Atlantis";
    public static final String MOD_NAME = NAME;
    public static final String VERSION = "9.0.0";
    public static final String WATER_NAME = "Water";
    public static final String[] WATER_NAME_AS_ARRAY = new String[] { WATER_NAME };
    public static final String ERROR_TEXT = "An error occurred in Atlantis";
    public static final Supplier<Component> ERROR_MESSAGE = () -> Text.of("&cAn error occurred in Atlantis!").compile();

    public static final int DEFAULT_THREADS = 4;
    public static final AtlantisThreadPool THREAD_POOL = AtlantisThreadPool.createNewThreadPool(NAME, DEFAULT_THREADS, true);
    public static final AtlantisConfigProxy.Impl CONFIG = AtlantisConfigProxy.of(ID);

    private static AtlantisListeners LISTENERS;

    public static AtlantisListeners listeners() {
        if (LISTENERS == null) {
            LISTENERS = new AtlantisListeners();
        }
        return LISTENERS;
    }

    /**
     * Whether the given player is the Atlantis author account ("Water").
     */
    public static boolean isPlayerWater(Player player) {
        if (player == null) {
            return false;
        }
        var name = player.getGameProfile().getName();
        return name != null && name.equalsIgnoreCase(WATER_NAME);
    }

    public static boolean isPlayerWater(ServerPlayer player) {
        return isPlayerWater((Player) player);
    }

    public static boolean isPlayerWater(UUID uuid) {
        return false;
    }

    public static boolean isPlayerWater(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).equals(WATER_NAME.toLowerCase(Locale.ROOT));
    }
}
