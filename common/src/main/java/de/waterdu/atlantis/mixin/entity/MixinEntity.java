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
 * Gives every entity a loader-neutral persistent tag. The entity saves the tag under {@value #KEY}.
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements PersistentDataHolder {

    @Unique
    private static final String KEY = "AtlantisData";

    @Unique
    private CompoundTag atlantis$persistentData;

    /**
     * Gets the persistent tag. Creates an empty tag the first time.
     *
     * @return the persistent tag of this entity
     */
    @Override
    public CompoundTag atlantis$persistentData() {
        if (this.atlantis$persistentData == null) {
            this.atlantis$persistentData = new CompoundTag();
        }
        return this.atlantis$persistentData;
    }

    /**
     * Writes a copy of the persistent tag under {@value #KEY} into the saved tag, if the persistent tag is not empty.
     *
     * @param tag the tag that the entity saves into
     * @param cir the callback that holds the saved tag
     */
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void atlantis$savePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.atlantis$persistentData != null && !this.atlantis$persistentData.isEmpty()) {
            cir.getReturnValue().put(KEY, this.atlantis$persistentData.copy());
        }
    }

    /**
     * Reads a copy of the persistent tag from {@value #KEY} in the loaded tag, if it exists.
     *
     * @param tag the tag that the entity loads from
     * @param ci the callback info
     */
    @Inject(method = "load", at = @At("HEAD"))
    private void atlantis$loadPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(KEY, Tag.TAG_COMPOUND)) {
            this.atlantis$persistentData = tag.getCompound(KEY).copy();
        }
    }
}
