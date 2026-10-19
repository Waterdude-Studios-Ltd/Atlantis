package de.waterdu.atlantis.mod.impl.fabric.modloader.permission;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public class FabricPermissionNode extends UPermissionNode<FabricPermissionNode.Node, Boolean> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.FABRIC_ID, "permission_node");

    public FabricPermissionNode(Node node) {
        super(TYPE, node);
    }

    @Override
    public String key() {
        return this.dereference().key();
    }

    @Override
    public Boolean get(ServerPlayer player) {
        var node = this.dereference();
        return Permissions.check(player, node.key(), node.defaultResolver().test(player));
    }

    public record Node(String key, Predicate<@Nullable ServerPlayer> defaultResolver) {}
}
