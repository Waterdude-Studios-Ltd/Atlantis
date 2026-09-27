package de.waterdu.atlantis.mod.wrapper.modloader.permission;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UPermissionNode<Node, Type> extends ModWrapper<Node> {

    protected UPermissionNode(ModObjectKey type, Node node) {
        super(new ModObject(type, node));
    }
}
