package de.waterdu.atlantis.ext.player;

import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;

public interface PermissionsExt {

    default boolean hasPermission(String node) {
        return false;
    }

    default <T> T hasPermission(UPermissionNode<?, T> node) {
        return null;
    }
}
