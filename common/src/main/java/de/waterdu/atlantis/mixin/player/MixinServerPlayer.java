package de.waterdu.atlantis.mixin.player;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Implements the player extensions on the server player, including permission checks.
 */
@Mixin(value = ServerPlayer.class)
public abstract class MixinServerPlayer extends Player {

    /**
     * @return the {@code containerCounter} field of this player
     */
    @Accessor("containerCounter")
    @Override
    public abstract int getContainerCounter();

    /**
     * Sets the {@code containerCounter} field of this player.
     *
     * @param counter the new container counter
     */
    @Accessor("containerCounter")
    @Override
    public abstract void setContainerCounter(int counter);

    @Unique
    private final Lazy<PlayerReference> atlantis$reference = Lazy.of(() -> PlayerReference.get(this.uuid));

    /**
     * Satisfies the constructor of {@link Player}. Mixin does not call it.
     *
     * @param level the level of the player
     * @param pos the position of the player
     * @param yRot the yaw of the player
     * @param gameProfile the game profile of the player
     */
    public MixinServerPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    /**
     * Gets the reference of this player. The reference is created on the first call.
     *
     * @return the player reference for the UUID of this player
     */
    @Override
    public PlayerReference reference() {
        return this.atlantis$reference.get();
    }

    /**
     * Opens the page for this player.
     *
     * @param page the page to open
     */
    @Override
    public void openPage(Page page) {
        page.open(this);
    }

    /**
     * Gets the head item of this player from {@link AtlantisData}.
     *
     * @return the head item for the UUID of this player
     */
    @Override
    public ItemStack headItem() {
        return AtlantisData.headItem(this.uuid);
    }

    /**
     * Checks if this player has the permission, with {@link Permissions#hasPermission}.
     *
     * @param node the permission string
     * @return {@code true} if the player has the permission
     */
    @Override
    public boolean hasPermission(String node) {
        return Permissions.hasPermission(this, node);
    }

    /**
     * Gets the value of the permission node for this player.
     *
     * @param node the permission node
     * @param <T> the type of the node value
     * @return the value of the node for this player
     */
    @Override
    public <T> T hasPermission(UPermissionNode<?, T> node) {
        return node.get((ServerPlayer) (Object) this);
    }
}
