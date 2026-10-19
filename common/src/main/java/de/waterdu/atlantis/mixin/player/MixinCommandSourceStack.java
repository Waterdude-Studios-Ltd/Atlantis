package de.waterdu.atlantis.mixin.player;

import de.waterdu.atlantis.ext.player.PermissionsExt;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.permission.Permissions;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CommandSourceStack.class)
public abstract class MixinCommandSourceStack implements PermissionsExt {

    @Override
    public boolean hasPermission(String node) {
        return Permissions.hasPermission(this.self(), node);
    }

    @Override
    public <T> T hasPermission(UPermissionNode<?, T> node) {
        var player = this.self().getPlayer();
        if (player == null) {
            return null;
        }
        return node.get(player);
    }

    @Unique
    private CommandSourceStack self() {
        return (CommandSourceStack) (Object) this;
    }
}
