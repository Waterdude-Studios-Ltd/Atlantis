package de.waterdu.atlantis.mod.impl.neoforge.modloader.permission;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * Wraps a NeoForge {@link PermissionNode}.
 *
 * @param <Type> the type of the value of the node
 */
public class NeoForgePermissionNode<Type> extends UPermissionNode<PermissionNode<Type>, Type> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.NEOFORGE_ID, "permission_node");

    /**
     * Creates a wrapper for the node.
     *
     * @param node the node to wrap
     */
    public NeoForgePermissionNode(PermissionNode<Type> node) {
        super(TYPE, node);
    }

    /**
     * @return the node name of the NeoForge node
     */
    @Override
    public String key() {
        return this.dereference().getNodeName();
    }

    /**
     * Gets the value of the node for the player from {@link PermissionAPI}.
     *
     * @param player the player
     * @return the value of the node for the player
     */
    @Override
    public Type get(ServerPlayer player) {
        return PermissionAPI.getPermission(player, this.dereference());
    }
}
