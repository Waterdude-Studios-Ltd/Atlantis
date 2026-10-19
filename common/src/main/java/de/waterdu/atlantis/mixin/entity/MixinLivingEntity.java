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
 * Emits ACTUALLY_HURT in {@code actuallyHurt}, where the damage left after armour, effects and absorption is
 * recorded and then taken from health. Listeners can change or cancel that damage. {@code Player} overrides
 * {@code actuallyHurt}, so {@code MixinPlayer} repeats these injectors.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void atlantis$emitActuallyHurt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original, @Share("reduction") LocalFloatRef reduction) {
        var applied = Mods.events().emitActuallyHurt((LivingEntity) (Object) this, source, damage);
        reduction.set(damage - applied);
        if (applied != 0F) {
            original.call(tracker, source, applied);
        }
    }

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private float atlantis$applyActuallyHurt(float health, @Share("reduction") LocalFloatRef reduction) {
        return health + reduction.get();
    }
}
