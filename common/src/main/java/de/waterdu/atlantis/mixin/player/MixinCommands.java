package de.waterdu.atlantis.mixin.player;

import com.mojang.brigadier.ParseResults;
import de.waterdu.atlantis.mod.Mods;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emits COMMAND before the server performs a command.
 */
@Mixin(Commands.class)
public abstract class MixinCommands {

    /**
     * Emits COMMAND. If a listener cancels it, the command is not performed.
     *
     * @param parse the parse results of the command
     * @param command the command string
     * @param ci the callback info
     */
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void atlantis$emitCommand(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        if (Mods.events().emitCommand(parse, command)) {
            ci.cancel();
        }
    }
}
