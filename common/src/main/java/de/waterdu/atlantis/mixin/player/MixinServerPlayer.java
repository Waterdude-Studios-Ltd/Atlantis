package de.waterdu.atlantis.mixin.player;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerPlayer.class)
public abstract class MixinServerPlayer extends Player {

    @Accessor("containerCounter")
    @Override
    public abstract int getContainerCounter();

    @Accessor("containerCounter")
    @Override
    public abstract void setContainerCounter(int counter);

    @Unique
    private final Lazy<PlayerReference> atlantis$reference = Lazy.of(() -> PlayerReference.get(this.uuid));

    public MixinServerPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
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
        return Permissions.hasPermission(this, node);
    }

    @Override
    public <T> T hasPermission(PermissionNode<T> node) {
        return PermissionAPI.getPermission((ServerPlayer) (Object) this, node);
    }
}
