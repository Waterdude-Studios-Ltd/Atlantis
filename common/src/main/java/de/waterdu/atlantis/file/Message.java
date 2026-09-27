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

public class Message {

    private Text.Immutable message;
    private String article = "";
    private String plural = "";

    private Message(Text message) {
        this.message = message.immutable();
    }

    public static Message empty() {
        return new Message(Text.empty());
    }

    public static Message of(Text message) {
        return new Message(message);
    }

    public static <T> Message of(T message) {
        return new Message(Text.of(message));
    }

    public Message withArticle(String article) {
        this.article = article;
        return this;
    }

    public Message withPlural(String plural) {
        this.plural = plural;
        return this;
    }

    public Message update(Text message) {
        this.message = message.immutable();
        this.article = "";
        this.plural = "";
        return this;
    }

    public Message update(Message message) {
        this.message = message.message;
        this.article = message.article;
        this.plural = message.plural;
        return this;
    }

    public Text get(boolean withArticle, boolean withPlural, Object... args) {
        Text result = this.message;
        if (withArticle && !this.article.isEmpty()) {
            result = this.message.prepend("").prepend(this.article);
        }
        if (withPlural && !this.plural.isEmpty()) {
            result = this.message.append(this.plural);
        }
        return TextUtils.format(result, args);
    }

    public static class TypeAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

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
