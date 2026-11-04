package de.waterdu.atlantis.mod.wrapper.modloader.permission;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wraps a permission node of a loader. Create one with {@code Mods.modLoader().createBooleanNode}.
 *
 * @param <Node> the type of the wrapped node
 * @param <Type> the type of the value of the node
 */
public abstract class UPermissionNode<Node, Type> extends ModWrapper<Node> {

    /**
     * Creates a wrapper for the permission node.
     *
     * @param type the type of the wrapper
     * @param node the node to wrap
     */
    protected UPermissionNode(ModObjectKey type, Node node) {
        super(new ModObject(type, node));
    }

    /**
     * @return the permission string of the node
     */
    public abstract String key();

    /**
     * Gets the value of the node for the player.
     *
     * @param player the player
     * @return the value of the node for the player
     */
    public abstract Type get(ServerPlayer player);
}
