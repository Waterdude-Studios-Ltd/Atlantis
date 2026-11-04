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

/**
 * Holds a packet that the server sends to or receives from a player. Listeners can replace the packet or cancel it.
 */
public class PacketInterceptEventPayload implements EventPayload, Cancellable {

    private final Connection connection;
    private final ServerPlayer player;
    private Packet<?> packet;
    private CustomPacketPayload customPacket;
    private boolean packetChanged = false;
    private boolean cancelled = false;

    /**
     * Creates a payload. Gets the custom payload from the packet if it is a custom payload packet.
     *
     * @param connection the connection of the player
     * @param player the player
     * @param packet the packet
     */
    public PacketInterceptEventPayload(Connection connection, ServerPlayer player, Packet<?> packet) {
        this(connection, player, packet, tryExtractPayload(packet));
    }

    /**
     * Creates a payload.
     *
     * @param connection the connection of the player
     * @param player the player
     * @param packet the packet
     * @param customPacket the custom payload of the packet, or {@code null} if it is not a custom payload packet
     */
    public PacketInterceptEventPayload(Connection connection, ServerPlayer player, Packet<?> packet, CustomPacketPayload customPacket) {
        this.connection = connection;
        this.player = player;
        this.packet = packet;
        this.customPacket = customPacket;
    }

    /**
     * Gets the payload of a clientbound or serverbound custom payload packet.
     *
     * @param packet the packet
     * @return the payload, or {@code null} if the packet is not a custom payload packet
     */
    private static CustomPacketPayload tryExtractPayload(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)) {
            return payload;
        } else if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            return payload;
        } else {
            return null;
        }
    }

    /**
     * @return the connection of the player
     */
    public Connection connection() {
        return this.connection;
    }

    /**
     * @return the player who sends or receives the packet
     */
    public ServerPlayer player() {
        return this.player;
    }

    /**
     * The direction in which this connection receives packets. The event fires only on the server end of a connection,
     * so this is always {@link PacketFlow#SERVERBOUND}.
     */
    public PacketFlow connectionDirection() {
        return this.connection.getReceiving();
    }

    /**
     * Checks if the connection receives clientbound packets.
     *
     * @return {@code true} if {@link #connectionDirection()} is {@link PacketFlow#CLIENTBOUND}
     */
    public boolean isConnectionClientbound() {
        return this.connectionDirection() == PacketFlow.CLIENTBOUND;
    }

    /**
     * Checks if the connection receives serverbound packets.
     *
     * @return {@code true} if {@link #connectionDirection()} is {@link PacketFlow#SERVERBOUND}
     */
    public boolean isConnectionServerbound() {
        return this.connectionDirection() == PacketFlow.SERVERBOUND;
    }

    /**
     * {@link PacketFlow#CLIENTBOUND} if the server sends the packet, or {@link PacketFlow#SERVERBOUND} if the server
     * receives it.
     */
    public PacketFlow packetDirection() {
        return this.packet.type().flow();
    }

    /**
     * Checks if the server sends the packet.
     *
     * @return {@code true} if {@link #packetDirection()} is {@link PacketFlow#CLIENTBOUND}
     */
    public boolean isPacketClientbound() {
        return this.packetDirection() == PacketFlow.CLIENTBOUND;
    }

    /**
     * Checks if the server receives the packet.
     *
     * @return {@code true} if {@link #packetDirection()} is {@link PacketFlow#SERVERBOUND}
     */
    public boolean isPacketServerbound() {
        return this.packetDirection() == PacketFlow.SERVERBOUND;
    }

    /**
     * Gives {@link #packet()} to the consumer if it is an instance of the type.
     *
     * @param type the packet class to check for
     * @param ifIs receives the packet if it has the type
     * @param <T> the packet type
     */
    public <T extends Packet<?>> void castPacket(Class<T> type, Consumer<T> ifIs) {
        if (type.isInstance(this.packet())) {
            ifIs.accept(this.packet());
        }
    }

    /**
     * The packet, or the replacement from {@link #setPacket(Packet)}. The server can send the same packet object to
     * many players. Thus, do not change it. Use {@link #setPacket(Packet)} instead.
     */
    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T packet() {
        return (T) (this.packet instanceof PacketInterceptWrapper<?> intercept ? intercept.unwrap() : this.packet);
    }

    /**
     * The packet as the event holds it. After {@link #setPacket(Packet)}, this is a {@link PacketInterceptWrapper}.
     */
    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T direct() {
        return (T) this.packet;
    }

    /**
     * Checks if the packet is a custom payload packet.
     *
     * @return {@code true} if {@link #customPacket()} is not {@code null}
     */
    public boolean isCustomPacket() {
        return this.customPacket != null;
    }

    /**
     * Gives {@link #customPacket()} to the consumer if it is an instance of the type.
     *
     * @param type the payload class to check for
     * @param ifIs receives the payload if it has the type
     * @param <T> the payload type
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> void castCustomPacket(Class<T> type, Consumer<T> ifIs) {
        if (type.isInstance(this.customPacket)) {
            ifIs.accept((T) this.customPacket);
        }
    }

    /**
     * The payload of a custom payload packet, or null if the packet is of a different type.
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> T customPacket() {
        return (T) this.customPacket;
    }

    /**
     * Replaces the packet that the server sends or handles. The listeners after the current one see the new packet.
     * This does not update {@link #customPacket()}. To replace a payload, use
     * {@link #setCustomPacket(CustomPacketPayload)}.
     */
    public void setPacket(Packet<?> packet) {
        this.packet = new PacketInterceptWrapper<>(packet);
        this.packetChanged = true;
    }

    /**
     * Replaces the packet with a custom payload packet of the same direction that holds the new payload.
     *
     * @param customPacket the new payload. Throws an {@link IllegalArgumentException} if it is null, or if the packet
     *     is not a custom payload packet.
     */
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

    /**
     * Checks if a listener replaced the packet.
     *
     * @return {@code true} if {@link #setPacket(Packet)} or {@link #setCustomPacket(CustomPacketPayload)} was called
     */
    public boolean isPacketChanged() {
        return this.packetChanged;
    }

    /**
     * Marks this payload as cancelled. The server then does not send or handle the packet.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Checks if a listener cancelled this payload.
     *
     * @return {@code true} if the payload is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    /**
     * @return {@link UEvent#INTERCEPT_PACKET}
     */
    @Override
    public Key<?> type() {
        return UEvent.INTERCEPT_PACKET;
    }
}
