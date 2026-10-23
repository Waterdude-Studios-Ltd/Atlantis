package de.waterdu.atlantis.util.network;

import io.netty.channel.local.LocalAddress;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkUtilsTest {

    @Test
    void ipv4() throws UnknownHostException {
        var address = new InetSocketAddress(InetAddress.getByName("192.168.0.12"), 25565);

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo("192.168.0.12");
    }

    @Test
    void ipv6() throws UnknownHostException {
        var address = new InetSocketAddress(InetAddress.getByName("::1"), 25565);

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    void unresolvedHost() {
        assertThat(NetworkUtils.addressToIP(InetSocketAddress.createUnresolved("example.com", 25565))).isEqualTo("example.com");
    }

    @Test
    void localChannel() {
        var address = new LocalAddress("local-test");

        assertThat(NetworkUtils.addressToIP(address)).isEqualTo(address.toString());
    }
}
