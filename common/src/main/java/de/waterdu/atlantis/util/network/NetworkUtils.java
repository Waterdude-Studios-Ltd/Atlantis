package de.waterdu.atlantis.util.network;

import de.waterdu.atlantis.mixin.network.MixinServerCommonPacketListenerImpl;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.server.level.ServerPlayer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Records the network addresses and IPs of the players that log in, and finds players by address or IP.
 */
public final class NetworkUtils {

    private static final Set<PlayerReference> EMPTY_SET = new HashSet<>();

    private static final Map<PlayerReference, Optional<SocketAddress>> ADDRESS_MAP = new HashMap<>();
    private static final Map<PlayerReference, Optional<String>> IP_MAP = new HashMap<>();

    private static final Map<SocketAddress, Set<PlayerReference>> INVERSE_ADDRESS_MAP = new HashMap<>();
    private static final Map<String, Set<PlayerReference>> INVERSE_IP_MAP = new HashMap<>();

    private NetworkUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Atlantis calls this when a player logs in. The entries stay after the player logs out.
     */
    public static void addAddress(ServerPlayer player) {
        var reference = PlayerReference.get(player);
        var address = ((MixinServerCommonPacketListenerImpl) (Object) player.connection).atlantis$getConnection().getRemoteAddress();
        var optionalAddress = Optional.of(address);
        var ip = NetworkUtils.addressToIP(address);
        var optionalIP = Optional.of(ip);
        NetworkUtils.ADDRESS_MAP.put(reference, optionalAddress);
        NetworkUtils.INVERSE_ADDRESS_MAP.computeIfAbsent(address, k -> new HashSet<>()).add(reference);
        NetworkUtils.IP_MAP.put(reference, optionalIP);
        NetworkUtils.INVERSE_IP_MAP.computeIfAbsent(ip, k -> new HashSet<>()).add(reference);
    }

    /**
     * Gets the last address that the player logged in from. The cast to {@code T} is not checked.
     *
     * @param reference the player
     * @param <T> the type of the address
     * @return the address, or an empty {@link Optional} if the player did not log in since the server started
     */
    @SuppressWarnings("unchecked")
    public static <T extends SocketAddress> Optional<T> address(PlayerReference reference) {
        return (Optional<T>) NetworkUtils.ADDRESS_MAP.getOrDefault(reference, Optional.empty());
    }

    /**
     * Gets the last IP that the player logged in from.
     *
     * @param reference the player
     * @return the IP, or an empty {@link Optional} if the player did not log in since the server started
     */
    public static Optional<String> ip(PlayerReference reference) {
        return NetworkUtils.IP_MAP.getOrDefault(reference, Optional.empty());
    }

    /**
     * @return all players that logged in from this address since the server started. Do not change the set.
     */
    public static Set<PlayerReference> player(SocketAddress address) {
        return NetworkUtils.INVERSE_ADDRESS_MAP.getOrDefault(address, EMPTY_SET);
    }

    /**
     * @return all players that logged in from this IP since the server started. Do not change the set.
     */
    public static Set<PlayerReference> player(String ip) {
        return NetworkUtils.INVERSE_IP_MAP.getOrDefault(ip, EMPTY_SET);
    }

    /**
     * Gets the IP of an address. For an {@link InetSocketAddress} this is the host address, or the host name if it
     * is not resolved. For other addresses it is {@link SocketAddress#toString()}.
     *
     * @param address the address
     * @return the IP as text
     */
    public static String addressToIP(SocketAddress address) {
        if (address instanceof InetSocketAddress inet) {
            return inet.getAddress() != null ? inet.getAddress().getHostAddress() : inet.getHostString();
        }
        return address.toString();
    }
}
