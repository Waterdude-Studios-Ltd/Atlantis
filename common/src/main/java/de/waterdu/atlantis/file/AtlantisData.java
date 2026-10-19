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

public final class AtlantisData {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Map<Class<? extends Page>, Decorations> MAPPED_DECORATIONS = new HashMap<>();

    private AtlantisData() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {
        if (!AtlantisData.REGISTERED.get()) {
            AtlantisConfigFinder.init(false);

            Atlantis.CONFIG.enqueue(() -> {
                var settings = Settings.get();
                var threads = settings.getThreads();
                if (threads <= 0) {
                    threads = AtlantisCommand.countAtlantisMods() * 2 + 1;
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

    public static void mapDecorations() {
        AtlantisData.MAPPED_DECORATIONS.clear();
        for (var def : Atlantis.CONFIG.all(Decorations.class)) {
            if (def.getClassDef() != null) {
                AtlantisData.MAPPED_DECORATIONS.put(def.getClassDef(), def);
            }
        }
    }

    public static Decorations decorations(Page page) {
        return AtlantisData.MAPPED_DECORATIONS.get(page.getClass());
    }

    public static Decorations decorations(Class<? extends Page> page) {
        return AtlantisData.MAPPED_DECORATIONS.get(page);
    }

    public static CompletableFuture<AtlantisPlayer> get(PlayerReference player) {
        return player.entity().map(AtlantisData::get).orElseGet(() -> AtlantisData.get(player.uuid()));
    }

    public static CompletableFuture<AtlantisPlayer> get(Player player) {
        return AtlantisData.get(player.getUUID()).whenComplete((p, err) -> p.setNameAndUpdate(player.getName().getString()));
    }

    public static CompletableFuture<AtlantisPlayer> get(UUID uuid) {
        return Atlantis.CONFIG.getForOrPut(AtlantisPlayer.class, uuid, () -> new AtlantisPlayer(uuid));
    }

    public static CompletableFuture<AtlantisPlayer> get(String name) {
        return Atlantis.CONFIG.get(AtlantisPlayer.class, name);
    }

    public static ItemStack headItem(UUID uuid) {
        var pd = AtlantisData.get(uuid).getNow(null);
        return pd == null ? new ItemStack(Items.PLAYER_HEAD) : pd.headItem();
    }
}
