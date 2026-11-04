package de.waterdu.atlantis.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives access to the connection of a {@link ServerCommonPacketListenerImpl}.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public interface MixinServerCommonPacketListenerImpl {

    /**
     * @return the connection of this packet listener
     */
    @Accessor("connection")
    Connection atlantis$getConnection();
}
