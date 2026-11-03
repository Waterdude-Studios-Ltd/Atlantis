package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class with methods that run tasks on specific threads.
 */
public final class Concurrency {

    private Concurrency() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Runs a task on a thread other than the main thread.
     *
     * @param task task to run asynchronously
     */
    public static void async(Runnable task) {
        Atlantis.THREAD_POOL.submit(task);
    }

    /**
     * Runs a task on the main thread.
     *
     * @param task task to be run synchronously
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
     * Runs a task on the main thread, with fallback support. For internal use.
     *
     * @param player player that this task is associated with
     * @param fallback page to show if this task fails
     * @param task task to be run synchronously
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
