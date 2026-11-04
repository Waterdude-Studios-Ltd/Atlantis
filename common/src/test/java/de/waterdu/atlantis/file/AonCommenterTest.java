package de.waterdu.atlantis.file;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.file.datatypes.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link AonCommenter}.
 */
class AonCommenterTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final Config<?> aon = mock(Config.class);
    private final Config<?> json = mock(Config.class);

    /**
     * Makes one mock config an AON config and the other a JSON config.
     */
    @BeforeEach
    void setUp() {
        when(this.aon.isAON()).thenReturn(true);
        when(this.json.isAON()).thenReturn(false);
    }

    /**
     * Checks that commenting writes the root comment with its indents, the field comments and AON keys.
     */
    @Test
    void writesRootAndFieldComments() {
        var commented = AonCommenter.comment(this.aon, GSON.toJson(new Small()), Small.class);

        assertThat(commented).isEqualTo("""
                # Root comment

                ## Second line

                # The display name
                display_name: "x"

                max_players: 10
                """);
    }

    /**
     * Checks that commenting and then uncommenting gives JSON equal to the original.
     */
    @Test
    void roundTripsToEquivalentJson() {
        var original = GSON.toJson(new Large());

        var commented = AonCommenter.comment(this.aon, original, Large.class);
        var uncommented = AonCommenter.uncomment(this.aon, commented, Large.class);

        assertThat(commented).contains("# Allowed ids").doesNotContain("\"display_name\"");
        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));
    }

    /**
     * Checks that a comma at the end of the last string value is kept after a round trip.
     */
    @Test
    void lastStringValueEndingInACommaSurvives() {
        var original = GSON.toJson(new TrailingComma());

        var uncommented = AonCommenter.uncomment(this.aon, AonCommenter.comment(this.aon, original, TrailingComma.class), TrailingComma.class);

        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));
    }

    /**
     * Checks that uncommenting AON text with Windows line endings gives JSON equal to the original.
     */
    @Test
    void uncommentsWindowsLineEndings() {
        var original = GSON.toJson(new Large());
        var commented = AonCommenter.comment(this.aon, original, Large.class).replace("\n", "\r\n");

        var uncommented = AonCommenter.uncomment(this.aon, commented, Large.class);

        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));

    }

    /**
     * Checks that uncommenting text that is already JSON returns the same string.
     */
    @Test
    void uncommentLeavesJsonAlone() {
        var original = GSON.toJson(new Small());

        assertThat(AonCommenter.uncomment(this.aon, original, Small.class)).isSameAs(original);
    }

    /**
     * Checks that commenting and uncommenting do not change the text of a config that is not AON.
     */
    @Test
    void nonAonConfigsAreUntouched() {
        var original = GSON.toJson(new Small());

        assertThat(AonCommenter.comment(this.json, original, Small.class)).isSameAs(original);
        assertThat(AonCommenter.uncomment(this.json, "a: 1", Small.class)).isEqualTo("a: 1");
    }

    /**
     * Checks that commenting and uncommenting do not change the text of a type without an {@link AonComment}.
     */
    @Test
    void typesWithoutARootCommentAreUntouched() {
        var original = GSON.toJson(new Uncommented());

        assertThat(AonCommenter.comment(this.aon, original, Uncommented.class)).isSameAs(original);
        assertThat(AonCommenter.uncomment(this.aon, "value: 1", Uncommented.class)).isEqualTo("value: 1");
    }

    /**
     * Holds two fields and a root comment of three lines.
     */
    @AonComment(value = {"Root comment", "", "Second line"}, indents = {1, 1, 2})
    static final class Small implements Configuration {
        @AonComment("The display name")
        String displayName = "x";
        int maxPlayers = 10;
    }

    /**
     * Holds lists, maps, nested objects, a null value and strings with colons.
     */
    @AonComment("Large")
    static final class Large implements Configuration {
        @AonComment("The display name")
        String displayName = "Atlantis: the lost city";
        @AonComment(value = "Allowed ids", indents = 2)
        List<Integer> allowedIds = List.of(1, 2, 3);
        Map<String, String> aliases = new LinkedHashMap<>(Map.of("minecraft:stone", "rock"));
        String url = "http://example.com:8080/path";
        String nothing = null;
        boolean enabled = true;
        Nested nested = new Nested();
        List<Nested> nestedList = List.of(new Nested(), new Nested());
    }

    /**
     * Holds a name and a list of tags, for use inside {@link Large}.
     */
    static final class Nested {
        String name = "inner";
        List<String> tags = List.of("a", "b");
    }

    /**
     * Holds a last string value that ends with a comma.
     */
    @AonComment("Trailing")
    static final class TrailingComma implements Configuration {
        int first = 1;
        String last = "ends with,";
    }

    /**
     * Holds one field and has no {@link AonComment}.
     */
    static final class Uncommented implements Configuration {
        int value = 1;
    }
}
