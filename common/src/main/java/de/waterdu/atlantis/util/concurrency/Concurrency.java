package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.world.entity.player.Player;

/**
 * Util class for methods relating to running tasks on specific threads.
 */
public final class Concurrency {

    private Concurrency() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Runs task on an off-thread.
     *
     * @param task Task to be run asynchronously.
     */
    public static void async(Runnable task) {
        Atlantis.THREAD_POOL.submit(task);
    }

    /**
     * Runs task on main thread.
     *
     * @param task Task to be run synchronously.
     */
    public static void sync(Runnable task) {
        ServerUtils.server().execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                AtlantisLogger.error("Caught exception in Atlantis! Continuing...");
                AtlantisLogger.error("Cause: {}", e.getMessage());
                AtlantisLogger.debugBuilder().withThrowable(e).log();
            }
        });
    }

    /**
     * Runs task on main thread, with fallback support for internal use.
     *
     * @param player Player this task is associated with.
     * @param fallback Page to show if this task failed.
     * @param task Task to be run synchronously.
     */
    public static void sync(Player player, Page fallback, Runnable task) {
        ServerUtils.server().execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                AtlantisLogger.error("Caught exception in Atlantis! Falling back...");
                AtlantisLogger.error("Cause: {}", e.getMessage());
                AtlantisLogger.debugBuilder().withThrowable(e).log();
                player.sendSystemMessage(Atlantis.ERROR_MESSAGE.get());
                AtlantisUI.open(player, null);
                ServerUtils.server().execute(() -> AtlantisUI.open(player, fallback));
            }
        });
    }
}
