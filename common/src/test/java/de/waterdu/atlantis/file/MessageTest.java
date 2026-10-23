package de.waterdu.atlantis.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.text.Text;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class MessageTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new Message.TypeAdapter()).create();

    @Test
    void formatsArguments() {
        var message = Message.of("%1 has %2 apples");

        assertThat(message.get(false, false, "Sam", 3).get()).isEqualTo("Sam has 3 apples");
    }

    @Test
    void articleAndPluralAreOptIn() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        assertThat(message.get(false, false).get()).isEqualTo("apple");
        assertThat(message.get(true, false).get()).isEqualTo("an apple");
        assertThat(message.get(false, true).get()).isEqualTo("apples");
        assertThat(message.get(true, true).get()).isEqualTo("an apples");
    }

    @Test
    void emptyArticleAndPluralAreIgnored() {
        assertThat(Message.of("apple").get(true, true).get()).isEqualTo("apple");
    }

    @Test
    void getDoesNotModifyTheMessage() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        message.get(true, true);

        assertThat(message.get(false, false).get()).isEqualTo("apple");
    }

    @Test
    void updateReplacesTextAndClearsExtras() {
        var message = Message.of("apple").withArticle("an ").withPlural("s");

        message.update(Text.of("pear"));

        assertThat(message.get(true, true).get()).isEqualTo("pear");
    }

    @Test
    void updateFromMessageCopiesEverything() {
        var message = Message.empty().update(Message.of("apple").withArticle("an ").withPlural("s"));

        assertThat(message.get(true, true).get()).isEqualTo("an apples");
    }

    @Test
    void plainMessagesSerializeAsStrings() {
        assertThat(this.gson.toJson(Message.of("hello"))).isEqualTo("\"hello\"");
        assertThat(this.gson.fromJson("\"hello\"", Message.class).get(true, true).get()).isEqualTo("hello");
    }

    @Test
    void messagesWithExtrasSerializeAsObjects() {
        var json = this.gson.toJson(Message.of("apple").withArticle("an ").withPlural("s"));

        assertThat(JsonParser.parseString(json)).isEqualTo(JsonParser.parseString("{\"message\":\"apple\",\"article\":\"an \",\"plural\":\"s\"}"));
        assertThat(this.gson.fromJson(json, Message.class).get(true, true).get()).isEqualTo("an apples");
    }

    @Test
    void objectsWithoutExtrasDeserialize() {
        assertThat(this.gson.fromJson("{\"message\":\"apple\"}", Message.class).get(true, true).get()).isEqualTo("apple");
    }
}
