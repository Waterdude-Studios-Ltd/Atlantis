package de.waterdu.atlantis.file;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.text.Text;
import de.waterdu.atlantis.util.text.TextUtils;

import java.lang.reflect.Type;

/**
 * Holds a text with an optional article and plural. The i18n files of a {@link Config} map to messages.
 */
public class Message {

    private Text.Immutable message;
    private String article = "";
    private String plural = "";

    /**
     * Creates a message with no article and no plural.
     *
     * @param message the text. The message keeps an immutable copy.
     */
    private Message(Text message) {
        this.message = message.immutable();
    }

    /**
     * Creates a message with an empty text.
     *
     * @return the new message
     */
    public static Message empty() {
        return new Message(Text.empty());
    }

    /**
     * Creates a message from the text.
     *
     * @param message the text
     * @return the new message
     */
    public static Message of(Text message) {
        return new Message(message);
    }

    /**
     * Creates a message from the text that {@link Text#of(Object)} makes from the value.
     *
     * @param <T> the type of the value
     * @param message the value
     * @return the new message
     */
    public static <T> Message of(T message) {
        return new Message(Text.of(message));
    }

    /**
     * Adds an article to this message and returns it.
     *
     * @param article text that goes directly in front of the message, with no space between. For example {@code "a "}.
     */
    public Message withArticle(String article) {
        this.article = article;
        return this;
    }

    /**
     * Adds a plural to this message and returns it.
     *
     * @param plural text that goes directly after the message. For example {@code "s"}.
     */
    public Message withPlural(String plural) {
        this.plural = plural;
        return this;
    }

    /**
     * Also clears the article and the plural.
     */
    public Message update(Text message) {
        this.message = message.immutable();
        this.article = "";
        this.plural = "";
        return this;
    }

    /**
     * Copies the text, the article and the plural of the other message to this message.
     *
     * @param message the message to copy from
     * @return this message
     */
    public Message update(Message message) {
        this.message = message.message;
        this.article = message.article;
        this.plural = message.plural;
        return this;
    }

    /**
     * @param args the values for {@code %1}, {@code %2} and so on
     */
    public Text get(boolean withArticle, boolean withPlural, Object... args) {
        Text result = this.message;
        if (withArticle && !this.article.isEmpty()) {
            result = this.message.prepend("").prepend(this.article);
        }
        if (withPlural && !this.plural.isEmpty()) {
            result = result.append(this.plural);
        }
        return TextUtils.format(result, args);
    }

    /**
     * Converts messages to and from JSON. A message with no article and no plural is a plain string. Other messages
     * are objects with {@code message}, {@code article} and {@code plural}.
     */
    public static class TypeAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

        /**
         * Reads a message from a string, or from an object with {@code message} and optional {@code article} and
         * {@code plural}.
         *
         * @param json the JSON to read
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return the message
         */
        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return Message.of(Text.of(json.getAsString()));
            }
            var object = json.getAsJsonObject();
            var message = Message.of(Text.of(object.get("message").getAsString()));
            if (object.has("article")) {
                message.article = object.get("article").getAsString();
            }
            if (object.has("plural")) {
                message.plural = object.get("plural").getAsString();
            }
            return message;
        }

        /**
         * Writes the message as a string if it has no article and no plural. If not, writes it as an object.
         *
         * @param src the message to write
         * @param typeOfSrc the type of the message
         * @param context the serialization context
         * @return the JSON of the message
         */
        @Override
        public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
            if (src.article.isEmpty() && src.plural.isEmpty()) {
                return new JsonPrimitive(src.message.get());
            }
            var object = new JsonObject();
            object.addProperty("message", src.message.get());
            object.addProperty("article", src.article);
            object.addProperty("plural", src.plural);
            return object;
        }
    }
}
