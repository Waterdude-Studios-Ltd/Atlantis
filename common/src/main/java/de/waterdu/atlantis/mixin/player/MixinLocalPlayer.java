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

@Mixin(value = LocalPlayer.class)
public abstract class MixinLocalPlayer extends Player {

    @Unique
    private final Lazy<PlayerReference> atlantis$reference = Lazy.of(() -> PlayerReference.get(this.uuid));

    public MixinLocalPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Override
    public PlayerReference reference() {
        return this.atlantis$reference.get();
    }

    @Override
    public void openPage(Page page) {
        page.open(this);
    }

    @Override
    public ItemStack headItem() {
        return AtlantisData.headItem(this.uuid);
    }

    @Override
    public boolean hasPermission(String node) {
        throw new NotImplementedException("Only instances of ServerPlayer can perform this");
    }

    @Override
    public <T> T hasPermission(UPermissionNode<?, T> node) {
        throw new NotImplementedException("Only instances of ServerPlayer can perform this");
    }
}
