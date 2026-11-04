package de.waterdu.atlantis.comm;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Skin}.
 */
class SkinTest {

    /**
     * Checks that a new skin has no texture and no signature.
     */
    @Test
    void emptyByDefault() {
        var skin = new Skin();

        assertThat(skin.hasSkin()).isFalse();
        assertThat(skin.hasSignature()).isFalse();
    }

    /**
     * Checks that the setters set the texture and the signature.
     */
    @Test
    void settersUpdateState() {
        var skin = new Skin();
        skin.setSkin("texture");
        skin.setSignature("signature");

        assertThat(skin.hasSkin()).isTrue();
        assertThat(skin.skin()).isEqualTo("texture");
        assertThat(skin.hasSignature()).isTrue();
        assertThat(skin.signature()).isEqualTo("signature");
    }

    /**
     * Checks that Gson writes and reads a skin without losing the texture or the signature.
     */
    @Test
    void gsonRoundTrip() {
        var gson = new Gson();
        var copy = gson.fromJson(gson.toJson(new Skin("texture", "signature")), Skin.class);

        assertThat(copy.skin()).isEqualTo("texture");
        assertThat(copy.signature()).isEqualTo("signature");
    }
}
