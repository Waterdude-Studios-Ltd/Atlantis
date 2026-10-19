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

public final class NetworkUtils {

    private static final Set<PlayerReference> EMPTY_SET = new HashSet<>();

    private static final Map<PlayerReference, Optional<SocketAddress>> ADDRESS_MAP = new HashMap<>();
    private static final Map<PlayerReference, Optional<String>> IP_MAP = new HashMap<>();

    private static final Map<SocketAddress, Set<PlayerReference>> INVERSE_ADDRESS_MAP = new HashMap<>();
    private static final Map<String, Set<PlayerReference>> INVERSE_IP_MAP = new HashMap<>();

    private NetworkUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

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

    @SuppressWarnings("unchecked")
    public static <T extends SocketAddress> Optional<T> address(PlayerReference reference) {
        return (Optional<T>) NetworkUtils.ADDRESS_MAP.getOrDefault(reference, Optional.empty());
    }

    public static Optional<String> ip(PlayerReference reference) {
        return NetworkUtils.IP_MAP.getOrDefault(reference, Optional.empty());
    }

    public static Set<PlayerReference> player(SocketAddress address) {
        return NetworkUtils.INVERSE_ADDRESS_MAP.getOrDefault(address, EMPTY_SET);
    }

    public static Set<PlayerReference> player(String ip) {
        return NetworkUtils.INVERSE_IP_MAP.getOrDefault(ip, EMPTY_SET);
    }

    public static String addressToIP(SocketAddress address) {
        if (address instanceof InetSocketAddress inet) {
            return inet.getAddress() != null ? inet.getAddress().getHostAddress() : inet.getHostString();
        }
        return address.toString();
    }
}
