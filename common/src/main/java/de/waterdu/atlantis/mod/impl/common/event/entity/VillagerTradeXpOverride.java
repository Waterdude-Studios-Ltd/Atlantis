package de.waterdu.atlantis.mod.impl.common.event.entity;

import java.util.function.IntSupplier;

/**
 * Carries the XP that {@code VILLAGER_TRADE_XP} decides into {@code Villager#rewardTradeXp}. Otherwise, that method
 * reads the XP from the offer. For internal use by the trade mixins of Atlantis.
 */
public final class VillagerTradeXpOverride {

    private static final ThreadLocal<Integer> XP = new ThreadLocal<>();

    private VillagerTradeXpOverride() {}

    public static void during(int xp, Runnable runnable) {
        XP.set(xp);
        try {
            runnable.run();
        } finally {
            XP.remove();
        }
    }

    public static int apply(IntSupplier offerXp) {
        var xp = XP.get();
        return xp != null ? xp : offerXp.getAsInt();
    }
}
