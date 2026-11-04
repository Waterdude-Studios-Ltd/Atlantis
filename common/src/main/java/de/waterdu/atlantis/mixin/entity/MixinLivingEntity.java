package de.waterdu.atlantis.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import de.waterdu.atlantis.mod.Mods;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Emits ACTUALLY_HURT in {@code actuallyHurt}. That method records the damage that remains after armor, effects and
 * absorption, and then removes it from health. Listeners can change or cancel that damage. {@code Player} overrides
 * {@code actuallyHurt}. Thus, {@code MixinPlayer} repeats these injectors.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * Emits ACTUALLY_HURT and records the damage that the listeners return. Records nothing if that damage is
     * {@code 0}. Shares the difference to the original damage with {@link #atlantis$applyActuallyHurt}.
     *
     * @param tracker the combat tracker of the entity
     * @param source the source of the damage
     * @param damage the damage after armor, effects and absorption
     * @param original the original {@code recordDamage} call
     * @param reduction the shared amount by which the listeners reduced the damage
     */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void atlantis$emitActuallyHurt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original, @Share("reduction") LocalFloatRef reduction) {
        var applied = Mods.events().emitActuallyHurt((LivingEntity) (Object) this, source, damage);
        reduction.set(damage - applied);
        if (applied != 0F) {
            original.call(tracker, source, applied);
        }
    }

    /**
     * Adds the reduction from {@link #atlantis$emitActuallyHurt} back to the new health.
     *
     * @param health the health after the original damage
     * @param reduction the shared amount by which the listeners reduced the damage
     * @return the health after the damage that the listeners return
     */
    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private float atlantis$applyActuallyHurt(float health, @Share("reduction") LocalFloatRef reduction) {
        return health + reduction.get();
    }
}
