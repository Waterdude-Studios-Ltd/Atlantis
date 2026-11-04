package de.waterdu.atlantis.mod.impl.common.event.entity;

import java.util.function.IntSupplier;

/**
 * Carries the XP that {@code VILLAGER_TRADE_XP} decides into {@code Villager#rewardTradeXp}. Otherwise, that method
 * reads the XP from the offer. For internal use by the trade mixins of Atlantis.
 */
public final class VillagerTradeXpOverride {

    private static final ThreadLocal<Integer> XP = new ThreadLocal<>();

    /**
     * Prevents instances of this class.
     */
    private VillagerTradeXpOverride() {}

    /**
     * Runs the runnable with the XP set for the current thread. Removes the XP after the runnable ends, also when it
     * throws.
     *
     * @param xp the XP that {@link #apply} returns while the runnable runs
     * @param runnable the code to run
     */
    public static void during(int xp, Runnable runnable) {
        XP.set(xp);
        try {
            runnable.run();
        } finally {
            XP.remove();
        }
    }

    /**
     * Gets the XP set by {@link #during} on the current thread.
     *
     * @param offerXp supplies the XP of the offer
     * @return the set XP, or the XP of the offer if no XP is set
     */
    public static int apply(IntSupplier offerXp) {
        var xp = XP.get();
        return xp != null ? xp : offerXp.getAsInt();
    }
}
