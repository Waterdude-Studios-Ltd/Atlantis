package de.waterdu.atlantis.util.network;

import io.netty.channel.local.LocalAddress;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link NetworkUtils}.
 */
class NetworkUtilsTest {

    /**
     * Checks that {@link NetworkUtils#addressToIP} gives the IPv4 address with no port.
     *
     * @throws UnknownHostException if the address cannot be parsed
     */
    @Test
    void ipv4() throws UnknownHostException {
        var address = new InetSocketAddress(InetAddress.getByName("192.168.0.12"), 25565);

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo("192.168.0.12");
    }

    /**
     * Checks that {@link NetworkUtils#addressToIP} gives the full form of an IPv6 address with no port.
     *
     * @throws UnknownHostException if the address cannot be parsed
     */
    @Test
    void ipv6() throws UnknownHostException {
        var address = new InetSocketAddress(InetAddress.getByName("::1"), 25565);

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo("0:0:0:0:0:0:0:1");
    }

    /**
     * Checks that {@link NetworkUtils#addressToIP} gives the host name of an unresolved address.
     */
    @Test
    void unresolvedHost() {
        assertThat(NetworkUtils.addressToIP(InetSocketAddress.createUnresolved("example.com", 25565))).isEqualTo("example.com");
    }

    /**
     * Checks that {@link NetworkUtils#addressToIP} gives the string form of a local channel address.
     */
    @Test
    void localChannel() {
        var address = new LocalAddress("local-test");

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo(address.toString());
    }
}
