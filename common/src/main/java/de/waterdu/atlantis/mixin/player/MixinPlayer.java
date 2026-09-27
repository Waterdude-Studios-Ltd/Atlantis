package de.waterdu.atlantis.mixin.player;

import de.waterdu.atlantis.ext.player.PermissionsExt;
import de.waterdu.atlantis.ext.player.PlayerExt;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class MixinPlayer implements PlayerExt, PermissionsExt {

}