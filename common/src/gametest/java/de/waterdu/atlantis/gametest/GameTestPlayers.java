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
 * Mock players whose connection accepts writes from any thread.
 */
public final class GameTestPlayers {

    /**
     * Prevents instantiation.
     */
    private GameTestPlayers() {}

    /**
     * Same as {@link GameTestHelper#makeMockServerPlayerInLevel()}, but with a different channel. Atlantis sends
     * packets from its thread pool, for example for UI pages. A real channel queues these writes on its event loop. An
     * {@link EmbeddedChannel} counts every thread as its event loop. Thus, concurrent writes corrupt its outbound
     * buffer, and the server disconnects the mock player.
     */
    public static ServerPlayer mock(GameTestHelper helper) {
        return mock(helper, new GameProfile(UUID.randomUUID(), "test-mock-player"));
    }

    /**
     * Logs in a player with the given profile. This lets a player who logged out log in again.
     */
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

    /**
     * Disconnects the player as the server does for a closed channel. Thus, both loaders fire their logout hooks.
     */
    public static void logOut(ServerPlayer player) {
        var connection = ((MixinServerCommonPacketListenerImpl) (Object) player.connection).atlantis$getConnection();
        connection.disconnect(Component.literal("GameTest"));
        connection.handleDisconnection();
    }

    /**
     * Embedded channel that synchronizes its writes, flushes and outbound reads, so that more than one thread can use
     * it.
     */
    private static final class SynchronizedEmbeddedChannel extends EmbeddedChannel {

        /**
         * Creates a channel and adds the given handlers to its pipeline.
         *
         * @param handlers the handlers to add, for example the {@link Connection}
         */
        SynchronizedEmbeddedChannel(ChannelHandler... handlers) {
            super(handlers);
        }

        /**
         * Writes the message while it holds the lock of this channel.
         *
         * @param msg the message to write
         * @return the future of the write
         */
        @Override
        public synchronized ChannelFuture write(Object msg) {
            return super.write(msg);
        }

        /**
         * Writes the message while it holds the lock of this channel.
         *
         * @param msg the message to write
         * @param promise the promise to complete when the write is done
         * @return the future of the write
         */
        @Override
        public synchronized ChannelFuture write(Object msg, ChannelPromise promise) {
            return super.write(msg, promise);
        }

        /**
         * Writes and flushes the message while it holds the lock of this channel.
         *
         * @param msg the message to write
         * @return the future of the write
         */
        @Override
        public synchronized ChannelFuture writeAndFlush(Object msg) {
            return super.writeAndFlush(msg);
        }

        /**
         * Writes and flushes the message while it holds the lock of this channel.
         *
         * @param msg the message to write
         * @param promise the promise to complete when the write is done
         * @return the future of the write
         */
        @Override
        public synchronized ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return super.writeAndFlush(msg, promise);
        }

        /**
         * Flushes the channel while it holds the lock of this channel.
         *
         * @return this channel
         */
        @Override
        public synchronized Channel flush() {
            return super.flush();
        }

        /**
         * Reads the next outbound message while it holds the lock of this channel.
         *
         * @param <T> the type of the message
         * @return the next outbound message, or {@code null} if there is none
         */
        @Override
        public synchronized <T> T readOutbound() {
            return super.readOutbound();
        }
    }
}
