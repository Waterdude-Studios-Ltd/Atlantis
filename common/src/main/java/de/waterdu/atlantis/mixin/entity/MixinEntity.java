package de.waterdu.atlantis.mixin.entity;

import de.waterdu.atlantis.util.entity.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives every entity a loader-neutral persistent tag, saved under {@value #KEY}.
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements PersistentDataHolder {

    @Unique
    private static final String KEY = "AtlantisData";

    @Unique
    private CompoundTag atlantis$persistentData;

    @Override
    public CompoundTag atlantis$persistentData() {
        if (this.atlantis$persistentData == null) {
            this.atlantis$persistentData = new CompoundTag();
        }
        return this.atlantis$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void atlantis$savePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.atlantis$persistentData != null && !this.atlantis$persistentData.isEmpty()) {
            cir.getReturnValue().put(KEY, this.atlantis$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void atlantis$loadPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(KEY, Tag.TAG_COMPOUND)) {
            this.atlantis$persistentData = tag.getCompound(KEY).copy();
        }
    }
}
