package de.waterdu.atlantis.file;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;
import de.waterdu.atlantis.self.AtlantisCommand;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.internal.FontUtils;
import de.waterdu.atlantis.util.config.Leaderboard;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the configs of Atlantis and gives access to its player data and page decorations.
 */
public final class AtlantisData {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Map<Class<? extends Page>, Decorations> MAPPED_DECORATIONS = new HashMap<>();

    private AtlantisData() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Registers the configs of Atlantis and loads the fonts. Runs only once. When the configs are ready, sets the
     * thread pool size, schedules the async autosave and the leaderboard recalculation, and maps the decorations.
     */
    public static void register() {
        if (!AtlantisData.REGISTERED.get()) {
            AtlantisConfigFinder.init(false);

            Atlantis.CONFIG.enqueue(() -> {
                var settings = Settings.get();
                var threads = settings.getThreads();
                if (threads <= 0) {
                    threads = AtlantisData.automaticThreadCount(AtlantisCommand.countAtlantisMods());
                    AtlantisLogger.info("Automatically creating {} threads", threads);
                } else {
                    AtlantisLogger.info("Creating {} threads", threads);
                }
                Atlantis.THREAD_POOL.setSize(threads);
                if (settings.isUseAsyncAutosaver()) {
                    var delay = settings.getAsyncSaveDelaySeconds();
                    Atlantis.THREAD_POOL.scheduleAtFixedRate(AtlantisConfig::asyncSave, delay, delay, TimeUnit.SECONDS);
                }
                if (settings.getLeaderboardRecalculateGapSeconds() > 0) {
                    var delay = settings.getLeaderboardRecalculateGapSeconds();
                    var multithreaded = settings.isLeaderboardMultithreadedRecalculate();
                    Atlantis.THREAD_POOL.scheduleAtFixedRate(() -> Leaderboard.recalculateAll(multithreaded), delay, delay, TimeUnit.SECONDS);
                }
                AtlantisData.mapDecorations();
            });

            try {
                FontUtils.init();
            } catch (Exception e) {
                AtlantisLogger.error("Failed to init FontHelper: {}", e.getMessage());
            }

            AtlantisData.REGISTERED.set(true);
        }
    }

    /**
     * Gets the thread count to use when the settings do not set one.
     *
     * @param atlantisMods the number of mods that use Atlantis
     * @return two threads for each mod plus one, and at least {@link Atlantis#DEFAULT_THREADS}
     */
    public static int automaticThreadCount(int atlantisMods) {
        return Math.max(Atlantis.DEFAULT_THREADS, atlantisMods * 2 + 1);
    }

    /**
     * Maps each {@link Decorations} config to the page class that it is for. Replaces the old mapping.
     */
    public static void mapDecorations() {
        AtlantisData.MAPPED_DECORATIONS.clear();
        for (var def : Atlantis.CONFIG.all(Decorations.class)) {
            if (def.getClassDef() != null) {
                AtlantisData.MAPPED_DECORATIONS.put(def.getClassDef(), def);
            }
        }
    }

    /**
     * @return {@code null} if no decorations are set for the class of the page
     */
    public static Decorations decorations(Page page) {
        return AtlantisData.MAPPED_DECORATIONS.get(page.getClass());
    }

    /**
     * @return {@code null} if no decorations are set for the class
     */
    public static Decorations decorations(Class<? extends Page> page) {
        return AtlantisData.MAPPED_DECORATIONS.get(page);
    }

    /**
     * Gets the data with {@link #get(Player)} if the player is online, or else with {@link #get(UUID)}.
     *
     * @param player the player
     * @return a future of the data
     */
    public static CompletableFuture<AtlantisPlayer> get(PlayerReference player) {
        return player.entity().map(AtlantisData::get).orElseGet(() -> AtlantisData.get(player.uuid()));
    }

    /**
     * Same as {@link #get(UUID)}. Also updates the stored name of the player.
     */
    public static CompletableFuture<AtlantisPlayer> get(Player player) {
        return AtlantisData.get(player.getUUID()).whenComplete((p, err) -> {
            if (p != null) {
                p.setNameAndUpdate(player.getName().getString());
            }
        });
    }

    /**
     * Creates and saves the data if it does not exist.
     */
    public static CompletableFuture<AtlantisPlayer> get(UUID uuid) {
        return Atlantis.CONFIG.getForOrPut(AtlantisPlayer.class, uuid, () -> new AtlantisPlayer(uuid));
    }

    /**
     * Unlike {@link #get(UUID)}, this does not create the data.
     *
     * @return a future of {@code null} if the name is not known
     */
    public static CompletableFuture<AtlantisPlayer> get(String name) {
        return Atlantis.CONFIG.get(AtlantisPlayer.class, name);
    }

    /**
     * Does not wait for storage. Creates the data if it does not exist, as {@link #get(UUID)} does.
     *
     * @return a plain player head if the data is not available yet
     */
    public static ItemStack headItem(UUID uuid) {
        var pd = AtlantisData.get(uuid).getNow(null);
        return pd == null ? new ItemStack(Items.PLAYER_HEAD) : pd.headItem();
    }
}
