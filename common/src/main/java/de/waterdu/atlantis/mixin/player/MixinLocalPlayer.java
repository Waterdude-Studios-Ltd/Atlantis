package de.waterdu.atlantis.mixin.player;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements the player extensions on the client player. The client player cannot check permissions.
 */
@Mixin(value = LocalPlayer.class)
public abstract class MixinLocalPlayer extends Player {

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
    public MixinLocalPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
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
     * Always throws, because only a {@code ServerPlayer} can check permissions.
     *
     * @param node the permission node
     * @return never returns
     * @throws NotImplementedException always
     */
    @Override
    public boolean hasPermission(String node) {
        throw new NotImplementedException("Only instances of ServerPlayer can perform this");
    }

    /**
     * Always throws, because only a {@code ServerPlayer} can check permissions.
     *
     * @param node the permission node
     * @param <T> the type of the node value
     * @return never returns
     * @throws NotImplementedException always
     */
    @Override
    public <T> T hasPermission(UPermissionNode<?, T> node) {
        throw new NotImplementedException("Only instances of ServerPlayer can perform this");
    }
}
