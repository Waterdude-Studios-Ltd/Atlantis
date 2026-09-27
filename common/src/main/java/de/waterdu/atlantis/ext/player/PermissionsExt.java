package de.waterdu.atlantis.ext.player;

import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public interface PermissionsExt {

    default boolean hasPermission(String node) {
        return false;
    }

    default <T> T hasPermission(PermissionNode<T> node) {
        return null;
    }
}
