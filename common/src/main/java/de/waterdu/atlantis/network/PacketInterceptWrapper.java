package de.waterdu.atlantis.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.NonNull;

/**
 * Wraps a packet that a packet intercept listener put in place of the original packet. Every call goes to the wrapped
 * packet.
 *
 * @param <T> the type of the listener that handles the packet
 */
public class PacketInterceptWrapper<T extends PacketListener> implements Packet<T> {

    private final Packet<T> packet;

    /**
     * Creates a wrapper for the packet.
     *
     * @param packet the packet to wrap
     */
    public PacketInterceptWrapper(@NonNull Packet<T> packet) {
        this.packet = packet;
    }

    /**
     * @return the type of the wrapped packet
     */
    @Override
    public @NonNull PacketType<? extends Packet<T>> type() {
        return this.packet.type();
    }

    /**
     * Checks if the wrapped packet is skippable.
     *
     * @return the result of the wrapped packet
     */
    @Override
    public boolean isSkippable() {
        return this.packet.isSkippable();
    }

    /**
     * Lets the wrapped packet handle itself with the handler.
     *
     * @param handler the listener that handles the packet
     */
    @Override
    public void handle(@NonNull T handler) {
        this.packet.handle(handler);
    }

    /**
     * @return the wrapped packet
     */
    public @NonNull Packet<T> unwrap() {
        return this.packet;
    }
}
