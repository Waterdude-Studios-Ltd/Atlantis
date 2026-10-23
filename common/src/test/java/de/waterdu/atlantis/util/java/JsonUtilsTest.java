package de.waterdu.atlantis.util.java;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsTest {

    private static final Gson GSON = new Gson();
    private static final JsonDeserializationContext CONTEXT = new JsonDeserializationContext() {
        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) {
            return GSON.fromJson(json, typeOfT);
        }
    };

    @Test
    void getOrDefaultReadsPresentValues() {
        var object = JsonParser.parseString("{\"a\": 5, \"list\": [1, 2]}").getAsJsonObject();

        assertThat(JsonUtils.getOrDefault(object, "a", int.class, 0, CONTEXT)).isEqualTo(5);
        assertThat(JsonUtils.getOrDefault(object, "list", new TypeToken<List<Integer>>() {}, List.of(), CONTEXT)).containsExactly(1, 2);
    }

    @Test
    void getOrDefaultFallsBackForMissingAndNull() {
        var object = JsonParser.parseString("{\"a\": null}").getAsJsonObject();

        assertThat(JsonUtils.getOrDefault(object, "a", String.class, "default", CONTEXT)).isEqualTo("default");
        assertThat(JsonUtils.getOrDefault(object, "missing", String.class, "default", CONTEXT)).isEqualTo("default");
        assertThat(JsonUtils.getOrDefault(new JsonObject(), "x", new TypeToken<List<String>>() {}, List.of("d"), CONTEXT)).containsExactly("d");
    }

    @Test
    void serializeList() {
        var array = JsonUtils.serializeList(List.of("a", "b"), JsonPrimitive::new);

        assertThat(array.toString()).isEqualTo("[\"a\",\"b\"]");
    }

    @Test
    void deserializeListDropsNulls() {
        var array = JsonParser.parseString("[\"a\", \"\", \"b\"]").getAsJsonArray();

        var list = JsonUtils.deserializeList(array, element -> element.getAsString().isEmpty() ? null : element.getAsString());

        assertThat(list).containsExactly("a", "b");
    }

    @Test
    void deserializeListImmutable() {
        var array = JsonParser.parseString("[1, 2]").getAsJsonArray();

        var list = JsonUtils.deserializeListImmutable(array, JsonElement::getAsInt);

        assertThat(list).containsExactly(1, 2);
        assertThatThrownBy(() -> list.add(3)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deserializeArrayIsTypedAndDropsNulls() {
        var array = new JsonArray();
        array.add("x");
        array.add("");
        array.add("y");

        String[] result = JsonUtils.deserializeArray(array, String.class, element -> element.getAsString().isEmpty() ? null : element.getAsString());

        assertThat(result).containsExactly("x", "y");
    }
}
