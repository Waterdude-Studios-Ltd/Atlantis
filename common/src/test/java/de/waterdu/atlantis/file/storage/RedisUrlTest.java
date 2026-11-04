package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing of Redis connection URLs. Valid URLs connect immediately. Thus, only invalid URLs go through the constructor.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class RedisUrlTest {

    /**
     * Checks that a URL with a username and a password parses into the host, port, username and password.
     */
    @Test
    void usernameAndPassword() {
        assertThat(Redis.toURIArray("redis://user:secret@10.0.0.2:6380/")).containsExactly("10.0.0.2", 6380, "user", "secret".toCharArray());
    }

    /**
     * Checks that a URL with only a password parses with an empty username.
     */
    @Test
    void passwordOnly() {
        assertThat(Redis.toURIArray("redis://secret@localhost:6379/")).containsExactly("localhost", 6379, "", "secret".toCharArray());
    }

    /**
     * Checks that a URL without credentials parses with an empty username and an empty password.
     */
    @Test
    void noCredentials() {
        assertThat(Redis.toURIArray("redis://localhost:6379")).containsExactly("localhost", 6379, "", new char[0]);
    }

    /**
     * Checks that only the first colon separates the username from the password.
     */
    @Test
    void passwordMayContainColons() {
        assertThat(Redis.toURIArray("redis://user:pa:ss@host:1/")).containsExactly("host", 1, "user", "pa:ss".toCharArray());
    }

    /**
     * Checks that the constructor throws {@link Redis.InvalidRedisUrlException} with the URL for an invalid URL.
     *
     * @param url the invalid URL
     */
    @ParameterizedTest
    @ValueSource(strings = {"redis://localhost", "redis://a@b@host:6379", "redis://host:port", "redis://host:1:2", "redis://:6379"})
    void invalidUrlsAreRejected(String url) {
        assertThatThrownBy(() -> new Redis(url))
                .isInstanceOf(Redis.InvalidRedisUrlException.class)
                .hasMessageContaining("redis://")
                .satisfies(e -> assertThat(((Redis.InvalidRedisUrlException) e).getURI()).isEqualTo(url));
    }
}
