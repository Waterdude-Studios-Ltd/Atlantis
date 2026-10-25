package de.waterdu.atlantis.mixin.player;

import com.mojang.brigadier.ParseResults;
import de.waterdu.atlantis.mod.Mods;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class MixinCommands {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void atlantis$emitCommand(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        if (Mods.events().emitCommand(parse, command)) {
            ci.cancel();
        }
    }
}
