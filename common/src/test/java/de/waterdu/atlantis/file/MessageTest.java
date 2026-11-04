package de.waterdu.atlantis.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.text.Text;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the formatting, update and JSON form of {@link Message}.
 */
@MinecraftBootstrapped
class MessageTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new Message.TypeAdapter()).create();

    /**
     * Checks that {@code get} fills in the numbered arguments.
     */
    @Test
    void formatsArguments() {
        var message = Message.of("%1 has %2 apples");

        assertThat(message.get(false, false, "Sam", 3).get()).isEqualTo("Sam has 3 apples");
    }

    /**
     * Checks that the article and the plural are added only when asked.
     */
    @Test
    void articleAndPluralAreOptIn() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        assertThat(message.get(false, false).get()).isEqualTo("apple");
        assertThat(message.get(true, false).get()).isEqualTo("an apple");
        assertThat(message.get(false, true).get()).isEqualTo("apples");
        assertThat(message.get(true, true).get()).isEqualTo("an apples");
    }

    /**
     * Checks that a message without an article or plural does not change when they are asked for.
     */
    @Test
    void emptyArticleAndPluralAreIgnored() {
        assertThat(Message.of("apple").get(true, true).get()).isEqualTo("apple");
    }

    /**
     * Checks that {@code get} with an article and plural does not change later results.
     */
    @Test
    void getDoesNotModifyTheMessage() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        message.get(true, true);

        assertThat(message.get(false, false).get()).isEqualTo("apple");
    }

    /**
     * Checks that an update from text replaces the text and removes the article and plural.
     */
    @Test
    void updateReplacesTextAndClearsExtras() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        message.update(Text.of("pear"));

        assertThat(message.get(true, true).get()).isEqualTo("pear");
    }

    /**
     * Checks that an update from another message copies its text, article and plural.
     */
    @Test
    void updateFromMessageCopiesEverything() {
        var message = Message.empty().update(Message.of("apple").withArticle("an ").withPlural("s"));

        assertThat(message.get(true, true).get()).isEqualTo("an apples");
    }

    /**
     * Checks that a message without an article or plural serializes as a JSON string, and reads back from one.
     */
    @Test
    void plainMessagesSerializeAsStrings() {
        assertThat(this.gson.toJson(Message.of("hello"))).isEqualTo("\"hello\"");
        assertThat(this.gson.fromJson("\"hello\"", Message.class).get(true, true).get()).isEqualTo("hello");
    }

    /**
     * Checks that a message with an article and plural serializes as a JSON object, and reads back from one.
     */
    @Test
    void messagesWithExtrasSerializeAsObjects() {
        var json = this.gson.toJson(Message.of("apple").withArticle("an ").withPlural("s"));

        assertThat(JsonParser.parseString(json)).isEqualTo(JsonParser.parseString("{\"message\":\"apple\",\"article\":\"an \",\"plural\":\"s\"}"));
        assertThat(this.gson.fromJson(json, Message.class).get(true, true).get()).isEqualTo("an apples");
    }

    /**
     * Checks that a JSON object with only a {@code message} key deserializes.
     */
    @Test
    void objectsWithoutExtrasDeserialize() {
        assertThat(this.gson.fromJson("{\"message\":\"apple\"}", Message.class).get(true, true).get()).isEqualTo("apple");
    }
}
