package de.waterdu.atlantis.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.NonNull;

/**
 * Marks a packet that a listener canceled. Use {@link #INSTANCE}.
 */
public final class CancelledPacket implements Packet<PacketListener> {

    /**
     * Replaces a packet that a listener canceled. The connection does not send it.
     * Its methods throw {@link UnsupportedOperationException}.
     */
    public static final CancelledPacket INSTANCE = new CancelledPacket();

    /**
     * Creates the only instance.
     */
    private CancelledPacket() {
        // noop
    }

    /**
     * Always throws, because a canceled packet has no type.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @NonNull PacketType<? extends Packet<PacketListener>> type() {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws, because a canceled packet must not be handled.
     *
     * @param packetListener the listener that would handle the packet
     * @throws UnsupportedOperationException always
     */
    @Override
    public void handle(@NonNull PacketListener packetListener) {
        throw new UnsupportedOperationException();
    }
}
