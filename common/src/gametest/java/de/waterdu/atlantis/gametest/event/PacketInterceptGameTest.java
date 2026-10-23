package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestConnections;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.network.PacketInterceptEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PacketInterceptGameTest {

    private PacketInterceptGameTest() {}

    @AtlantisGameTest
    public static void outboundPacketsAreSeen(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var seen = new ArrayList<PacketInterceptEventPayload>();
        listen(player, seen::add);
        GameTestConnections.sent(player);

        player.connection.send(chat("hello"));

        var payload = seen.stream().filter(it -> isChat(it.packet(), "hello")).findFirst().orElse(null);
        helper.assertTrue(payload != null, "INTERCEPT_PACKET did not see the outbound packet");
        helper.assertTrue(payload.player() == player, "payload player");
        helper.assertTrue(payload.isPacketClientbound(), "packet direction");
        helper.assertTrue(containsChat(GameTestConnections.sent(player), "hello"), "packet was not sent");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void cancellingStopsAnOutboundPacket(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        listen(player, payload -> {
            if (isChat(payload.packet(), "secret")) {
                payload.cancel();
            }
        });
        GameTestConnections.sent(player);

        player.connection.send(chat("secret"));
        player.connection.send(chat("public"));

        var sent = GameTestConnections.sent(player);
        helper.assertFalse(containsChat(sent, "secret"), "cancelled packet was sent");
        helper.assertTrue(containsChat(sent, "public"), "other packets stopped too");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void replacingChangesAnOutboundPacket(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        listen(player, payload -> {
            if (isChat(payload.packet(), "original")) {
                payload.setPacket(chat("replaced"));
            }
        });
        GameTestConnections.sent(player);

        player.connection.send(chat("original"));

        var sent = GameTestConnections.sent(player);
        helper.assertFalse(containsChat(sent, "original"), "original packet was sent");
        helper.assertTrue(containsChat(sent, "replaced"), "replacement was not sent");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void inboundPacketsAreHandled(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var seen = new ArrayList<PacketInterceptEventPayload>();
        listen(player, seen::add);

        GameTestConnections.channel(player).writeInbound(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        helper.assertTrue(seen.stream().anyMatch(it -> it.packet() instanceof ServerboundSwingPacket && it.isPacketServerbound()), "INTERCEPT_PACKET did not see the inbound packet");
        helper.assertTrue(player.swinging, "inbound packet was not handled");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void cancellingStopsAnInboundPacket(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        listen(player, payload -> {
            if (payload.packet() instanceof ServerboundSwingPacket) {
                payload.cancel();
            }
        });

        GameTestConnections.channel(player).writeInbound(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        helper.assertFalse(player.swinging, "cancelled inbound packet was handled");
        helper.succeed();
    }

    private static ClientboundSystemChatPacket chat(String text) {
        return new ClientboundSystemChatPacket(Component.literal(text), false);
    }

    private static boolean isChat(Packet<?> packet, String text) {
        return packet instanceof ClientboundSystemChatPacket chat && chat.content().getString().equals(text);
    }

    private static boolean containsChat(List<Packet<?>> packets, String text) {
        return packets.stream().anyMatch(packet -> isChat(packet, text));
    }

    private static void listen(ServerPlayer player, Consumer<PacketInterceptEventPayload> listener) {
        Mods.modLoader().event(UEvent.INTERCEPT_PACKET).listen(payload -> {
            if (payload.player() == player) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }
}
