package de.waterdu.atlantis.mixin.player;

import de.waterdu.atlantis.ext.player.PermissionsExt;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.permission.Permissions;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements {@link PermissionsExt} on {@link CommandSourceStack}.
 */
@Mixin(CommandSourceStack.class)
public abstract class MixinCommandSourceStack implements PermissionsExt {

    /**
     * Checks the permission with {@link Permissions#hasPermission(CommandSourceStack, String)}.
     *
     * @param node the permission string that the permission groups match
     * @return {@code true} if this source has the permission
     */
    @Override
    public boolean hasPermission(String node) {
        return Permissions.hasPermission(this.self(), node);
    }

    /**
     * Gets the value of the node for the player of this source.
     *
     * @param node the permission node
     * @param <T> the type of the node value
     * @return the value of the node, or {@code null} if this source is not a player
     */
    @Override
    public <T> T hasPermission(UPermissionNode<?, T> node) {
        var player = this.self().getPlayer();
        if (player == null) {
            return null;
        }
        return node.get(player);
    }

    /**
     * @return this mixin cast to {@link CommandSourceStack}
     */
    @Unique
    private CommandSourceStack self() {
        return (CommandSourceStack) (Object) this;
    }
}
