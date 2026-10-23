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

class AonCommenterTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final Config<?> aon = mock(Config.class);
    private final Config<?> json = mock(Config.class);

    @BeforeEach
    void setUp() {
        when(this.aon.isAON()).thenReturn(true);
        when(this.json.isAON()).thenReturn(false);
    }

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

    @Test
    void roundTripsToEquivalentJson() {
        var original = GSON.toJson(new Large());

        var commented = AonCommenter.comment(this.aon, original, Large.class);
        var uncommented = AonCommenter.uncomment(this.aon, commented, Large.class);

        assertThat(commented).contains("# Allowed ids").doesNotContain("\"display_name\"");
        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));
    }

    @Test
    void lastStringValueEndingInACommaSurvives() {
        var original = GSON.toJson(new TrailingComma());

        var uncommented = AonCommenter.uncomment(this.aon, AonCommenter.comment(this.aon, original, TrailingComma.class), TrailingComma.class);

        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));
    }

    @Test
    void uncommentsWindowsLineEndings() {
        var original = GSON.toJson(new Large());
        var commented = AonCommenter.comment(this.aon, original, Large.class).replace("\n", "\r\n");

        var uncommented = AonCommenter.uncomment(this.aon, commented, Large.class);

        assertThat(JsonParser.parseString(uncommented)).isEqualTo(JsonParser.parseString(original));

    }

    @Test
    void uncommentLeavesJsonAlone() {
        var original = GSON.toJson(new Small());

        assertThat(AonCommenter.uncomment(this.aon, original, Small.class)).isSameAs(original);
    }

    @Test
    void nonAonConfigsAreUntouched() {
        var original = GSON.toJson(new Small());

        assertThat(AonCommenter.comment(this.json, original, Small.class)).isSameAs(original);
        assertThat(AonCommenter.uncomment(this.json, "a: 1", Small.class)).isEqualTo("a: 1");
    }

    @Test
    void typesWithoutARootCommentAreUntouched() {
        var original = GSON.toJson(new Uncommented());

        assertThat(AonCommenter.comment(this.aon, original, Uncommented.class)).isSameAs(original);
        assertThat(AonCommenter.uncomment(this.aon, "value: 1", Uncommented.class)).isEqualTo("value: 1");
    }

    @AonComment(value = {"Root comment", "", "Second line"}, indents = {1, 1, 2})
    static final class Small implements Configuration {
        @AonComment("The display name")
        String displayName = "x";
        int maxPlayers = 10;
    }

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

    static final class Nested {
        String name = "inner";
        List<String> tags = List.of("a", "b");
    }

    @AonComment("Trailing")
    static final class TrailingComma implements Configuration {
        int first = 1;
        String last = "ends with,";
    }

    static final class Uncommented implements Configuration {
        int value = 1;
    }
}
