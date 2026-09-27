package de.waterdu.atlantis.permission;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public enum DefaultPermissionUser {

    ALL(player -> true),
    OP(Permissions::isOperator),
    NONE(player -> false),
    ;

    private final Predicate<ServerPlayer> test;

    DefaultPermissionUser(Predicate<ServerPlayer> test) {
        this.test = test;
    }

    public boolean test(ServerPlayer player) {
        return this.test.test(player);
    }
}
