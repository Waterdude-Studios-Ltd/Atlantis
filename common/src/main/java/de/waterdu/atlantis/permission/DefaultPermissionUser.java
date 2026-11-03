package de.waterdu.atlantis.permission;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Selects the players that have a permission node when no permission handler sets it.
 */
public enum DefaultPermissionUser {

    ALL(player -> true),
    OP(Permissions::isOperator),
    NONE(player -> false),
    ;

    private final Predicate<ServerPlayer> test;

    /**
     * @param test checks if a player has the node by default
     */
    DefaultPermissionUser(Predicate<ServerPlayer> test) {
        this.test = test;
    }

    /**
     * Checks if the player has the node by default.
     *
     * @param player the player to check
     * @return {@code true} if the player has the node
     */
    public boolean test(ServerPlayer player) {
        return this.test.test(player);
    }
}
