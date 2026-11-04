package de.waterdu.atlantis.ext.player;

import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;

/**
 * Adds permission checks to players and command sources. The interface is injected into those classes.
 */
public interface PermissionsExt {

    /**
     * Throws an exception on the client player.
     *
     * @param node the permission string that the permission groups match
     * @return {@code false} for players that are not server players
     */
    default boolean hasPermission(String node) {
        return false;
    }

    /**
     * Throws an exception on the client player.
     *
     * @return the value of the node for this player. null when this is a command source that is not a player.
     */
    default <T> T hasPermission(UPermissionNode<?, T> node) {
        return null;
    }
}
