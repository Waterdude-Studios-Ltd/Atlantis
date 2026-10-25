package de.waterdu.atlantis.mixin.level;

import de.waterdu.atlantis.mod.Mods;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Emits ENTITY_JOIN_LEVEL from the one method every server-side entity addition goes through, both for new
 * entities and for those loaded with their chunk.
 */
@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager {

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void atlantis$emitEntityJoinLevel(EntityAccess access, boolean loadedFromDisk, CallbackInfoReturnable<Boolean> cir) {
        if (access instanceof Entity entity && entity.level() instanceof ServerLevel level && Mods.events().emitEntityJoinLevel(entity, level, loadedFromDisk)) {
            cir.setReturnValue(false);
        }
    }
}
