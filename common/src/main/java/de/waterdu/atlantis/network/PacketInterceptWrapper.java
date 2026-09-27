package de.waterdu.atlantis.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.NonNull;

public class PacketInterceptWrapper<T extends PacketListener> implements Packet<T> {

    private final Packet<T> packet;

    public PacketInterceptWrapper(@NonNull Packet<T> packet) {
        this.packet = packet;
    }

    @Override
    public @NonNull PacketType<? extends Packet<T>> type() {
        return this.packet.type();
    }

    @Override
    public boolean isSkippable() {
        return this.packet.isSkippable();
    }

    @Override
    public void handle(@NonNull T handler) {
        this.packet.handle(handler);
    }

    public @NonNull Packet<T> unwrap() {
        return this.packet;
    }
}
