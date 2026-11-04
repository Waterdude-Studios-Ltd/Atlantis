package de.waterdu.atlantis.mixin.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import de.waterdu.atlantis.ext.player.PermissionsExt;
import de.waterdu.atlantis.ext.player.PlayerExt;
import de.waterdu.atlantis.mod.Mods;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Repeats the ACTUALLY_HURT injectors of {@code MixinLivingEntity}, because {@link Player} overrides
 * {@code actuallyHurt}. Also adds {@link PlayerExt} and {@link PermissionsExt} to every player.
 */
@Mixin(Player.class)
public abstract class MixinPlayer implements PlayerExt, PermissionsExt {

    /**
     * Emits ACTUALLY_HURT and records the changed damage instead of the original damage. Records nothing if the
     * damage becomes 0.
     *
     * @param tracker the combat tracker of the player
     * @param source the source of the damage
     * @param damage the damage after armor, effects and absorption
     * @param original the original {@code recordDamage} call
     * @param reduction receives the amount that the listeners removed from the damage
     */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void atlantis$emitActuallyHurt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original, @Share("reduction") LocalFloatRef reduction) {
        var applied = Mods.events().emitActuallyHurt((Player) (Object) this, source, damage);
        reduction.set(damage - applied);
        if (applied != 0F) {
            original.call(tracker, source, applied);
        }
    }

    /**
     * Adds the removed damage back to the new health, so that the player loses only the changed damage.
     *
     * @param health the health after the original damage
     * @param reduction the amount that the listeners removed from the damage
     * @return the health after the changed damage
     */
    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private float atlantis$applyActuallyHurt(float health, @Share("reduction") LocalFloatRef reduction) {
        return health + reduction.get();
    }
}
