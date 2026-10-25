package de.waterdu.atlantis.mixin.network;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.InteractEntityEventPayload;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emits CHAT before a validated message is broadcast, and PLAYER_INTERACT_ENTITY once the interaction has passed
 * the range and world border checks, just before it is dispatched to the entity.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    private void atlantis$emitChat(PlayerChatMessage message, CallbackInfo ci) {
        if (Mods.events().emitChat(this.player, message)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;dispatch(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket$Handler;)V"), cancellable = true)
    private void atlantis$emitInteractEntity(ServerboundInteractPacket packet, CallbackInfo ci) {
        var entity = packet.getTarget(this.player.serverLevel());
        if (entity == null) {
            return;
        }
        var reader = new ActionReader();
        packet.dispatch(reader);
        if (reader.action != null && Mods.events().emitInteractEntity(this.player, entity, reader.action, reader.hand)) {
            ci.cancel();
        }
    }

    private static final class ActionReader implements ServerboundInteractPacket.Handler {

        private InteractEntityEventPayload.@Nullable Action action;
        private @Nullable InteractionHand hand;

        @Override
        public void onInteraction(InteractionHand hand) {
            this.action = InteractEntityEventPayload.Action.INTERACT;
            this.hand = hand;
        }

        @Override
        public void onInteraction(InteractionHand hand, Vec3 location) {
            this.action = InteractEntityEventPayload.Action.INTERACT_AT;
            this.hand = hand;
        }

        @Override
        public void onAttack() {
            this.action = InteractEntityEventPayload.Action.ATTACK;
        }
    }
}
