package de.waterdu.atlantis.mod.impl.neoforge.modloader.permission;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public class NeoForgePermissionNode<Type> extends UPermissionNode<PermissionNode<Type>, Type> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.NEOFORGE_ID, "permission_node");

    public NeoForgePermissionNode(PermissionNode<Type> node) {
        super(TYPE, node);
    }
}
