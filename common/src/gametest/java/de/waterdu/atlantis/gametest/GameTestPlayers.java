package de.waterdu.atlantis.gametest;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.mixin.network.MixinServerCommonPacketListenerImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.UUID;

/**
 * Mock players whose connection can be written to from any thread.
 */
public final class GameTestPlayers {

    private GameTestPlayers() {}

    /**
     * Same as {@link GameTestHelper#makeMockServerPlayerInLevel()}, except for the channel. Atlantis sends packets
     * from its thread pool (UI pages, for example). A real channel queues those writes onto its event loop, but an
     * {@link EmbeddedChannel} counts every thread as its event loop, so concurrent writes corrupt its outbound buffer
     * and the mock player is disconnected.
     */
    public static ServerPlayer mock(GameTestHelper helper) {
        return mock(helper, new GameProfile(UUID.randomUUID(), "test-mock-player"));
    }

    /** Logs a player in with the given profile, so a player who logged out can log back in. */
    public static ServerPlayer mock(GameTestHelper helper, GameProfile profile) {
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new SynchronizedEmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** Disconnects the player as the server does for a closed channel, so both loaders fire their logout hooks. */
    public static void logOut(ServerPlayer player) {
        var connection = ((MixinServerCommonPacketListenerImpl) (Object) player.connection).atlantis$getConnection();
        connection.disconnect(Component.literal("GameTest"));
        connection.handleDisconnection();
    }

    private static final class SynchronizedEmbeddedChannel extends EmbeddedChannel {

        SynchronizedEmbeddedChannel(ChannelHandler... handlers) {
            super(handlers);
        }

        @Override
        public synchronized ChannelFuture write(Object msg) {
            return super.write(msg);
        }

        @Override
        public synchronized ChannelFuture write(Object msg, ChannelPromise promise) {
            return super.write(msg, promise);
        }

        @Override
        public synchronized ChannelFuture writeAndFlush(Object msg) {
            return super.writeAndFlush(msg);
        }

        @Override
        public synchronized ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return super.writeAndFlush(msg, promise);
        }

        @Override
        public synchronized Channel flush() {
            return super.flush();
        }

        @Override
        public synchronized <T> T readOutbound() {
            return super.readOutbound();
        }
    }
}
