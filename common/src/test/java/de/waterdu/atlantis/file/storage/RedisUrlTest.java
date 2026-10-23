package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing of Redis connection URLs. Valid URLs connect straight away, so only invalid ones go through the constructor.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class RedisUrlTest {

    @Test
    void usernameAndPassword() {
        assertThat(Redis.toURIArray("redis://user:secret@10.0.0.2:6380/")).containsExactly("10.0.0.2", 6380, "user", "secret".toCharArray());
    }

    @Test
    void passwordOnly() {
        assertThat(Redis.toURIArray("redis://secret@localhost:6379/")).containsExactly("localhost", 6379, "", "secret".toCharArray());
    }

    @Test
    void noCredentials() {
        assertThat(Redis.toURIArray("redis://localhost:6379")).containsExactly("localhost", 6379, "", new char[0]);
    }

    @Test
    void passwordMayContainColons() {
        assertThat(Redis.toURIArray("redis://user:pa:ss@host:1/")).containsExactly("host", 1, "user", "pa:ss".toCharArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"redis://localhost", "redis://a@b@host:6379", "redis://host:port", "redis://host:1:2", "redis://:6379"})
    void invalidUrlsAreRejected(String url) {
        assertThatThrownBy(() -> new Redis(url))
                .isInstanceOf(Redis.InvalidRedisUrlException.class)
                .hasMessageContaining("redis://")
                .satisfies(e -> assertThat(((Redis.InvalidRedisUrlException) e).getURI()).isEqualTo(url));
    }
}
