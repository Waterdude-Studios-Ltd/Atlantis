package de.waterdu.atlantis.mod.impl.common.event.entity;

import java.util.function.IntSupplier;

/**
 * Carries the XP decided by {@code VILLAGER_TRADE_XP} into {@code Villager#rewardTradeXp}, which otherwise reads it
 * from the offer. Internal to Atlantis' trade mixins.
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
