package de.waterdu.atlantis.mod.impl.common.event.network;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.network.PacketInterceptWrapper;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class PacketInterceptEventPayload implements EventPayload, Cancellable {

    private final Connection connection;
    private final ServerPlayer player;
    private Packet<?> packet;
    private CustomPacketPayload customPacket;
    private boolean packetChanged = false;
    private boolean cancelled = false;

    public PacketInterceptEventPayload(Connection connection, ServerPlayer player, Packet<?> packet) {
        this(connection, player, packet, tryExtractPayload(packet));
    }

    public PacketInterceptEventPayload(Connection connection, ServerPlayer player, Packet<?> packet, CustomPacketPayload customPacket) {
        this.connection = connection;
        this.player = player;
        this.packet = packet;
        this.customPacket = customPacket;
    }

    private static CustomPacketPayload tryExtractPayload(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)) {
            return payload;
        } else if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            return payload;
        } else {
            return null;
        }
    }

    public Connection connection() {
        return this.connection;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public PacketFlow connectionDirection() {
        return this.connection.getReceiving();
    }

    public boolean isConnectionClientbound() {
        return this.connectionDirection() == PacketFlow.CLIENTBOUND;
    }

    public boolean isConnectionServerbound() {
        return this.connectionDirection() == PacketFlow.SERVERBOUND;
    }

    public PacketFlow packetDirection() {
        return this.packet.type().flow();
    }

    public boolean isPacketClientbound() {
        return this.packetDirection() == PacketFlow.CLIENTBOUND;
    }

    public boolean isPacketServerbound() {
        return this.packetDirection() == PacketFlow.SERVERBOUND;
    }

    public <T extends Packet<?>> void castPacket(Class<T> type, Consumer<T> ifIs) {
        if (type.isInstance(this.packet())) {
            ifIs.accept(this.packet());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T packet() {
        return (T) (this.packet instanceof PacketInterceptWrapper<?> intercept ? intercept.unwrap() : this.packet);
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T direct() {
        return (T) this.packet;
    }

    public boolean isCustomPacket() {
        return this.customPacket != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> void castCustomPacket(Class<T> type, Consumer<T> ifIs) {
        if (type.isInstance(this.customPacket)) {
            ifIs.accept((T) this.customPacket);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> T customPacket() {
        return (T) this.customPacket;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = new PacketInterceptWrapper<>(packet);
        this.packetChanged = true;
    }

    public void setCustomPacket(CustomPacketPayload customPacket) {
        if (this.customPacket == null) {
            throw new IllegalArgumentException("Cannot set custom packet payload on a non-custom payload packet type");
        }
        if (customPacket == null) {
            throw new IllegalArgumentException("Custom packet payloads cannot be null");
        }

        var packet = this.packet();
        if (packet instanceof ClientboundCustomPayloadPacket) {
            this.setPacket(new ClientboundCustomPayloadPacket(customPacket));
        } else if (packet instanceof ServerboundCustomPayloadPacket) {
            this.setPacket(new ServerboundCustomPayloadPacket(customPacket));
        } else {
            throw new IllegalArgumentException("Cannot set custom packet payload on a non-custom payload packet type");
        }

        this.customPacket = customPacket;
    }

    public boolean isPacketChanged() {
        return this.packetChanged;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    @Override
    public Key<?> type() {
        return UEvent.INTERCEPT_PACKET;
    }
}
