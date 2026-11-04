package de.waterdu.atlantis.mod.impl.fabric.modloader.permission;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Wraps a boolean permission node that {@code fabric-permissions-api} checks.
 */
public class FabricPermissionNode extends UPermissionNode<FabricPermissionNode.Node, Boolean> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.FABRIC_ID, "permission_node");

    /**
     * Creates a wrapper for the node.
     *
     * @param node the node to wrap
     */
    public FabricPermissionNode(Node node) {
        super(TYPE, node);
    }

    /**
     * @return the permission string of the node
     */
    @Override
    public String key() {
        return this.dereference().key();
    }

    /**
     * Checks the permission with {@code fabric-permissions-api}. If no permission provider has a value, the default
     * resolver of the node decides.
     *
     * @param player the player
     * @return {@code true} if the player has the permission
     */
    @Override
    public Boolean get(ServerPlayer player) {
        var node = this.dereference();
        return Permissions.check(player, node.key(), node.defaultResolver().test(player));
    }

    /**
     * Holds the permission string and the default value of a node.
     *
     * @param key the permission string
     * @param defaultResolver gives the value when no permission provider has one
     */
    public record Node(String key, Predicate<@Nullable ServerPlayer> defaultResolver) {}
}
