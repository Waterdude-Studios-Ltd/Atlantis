package de.waterdu.atlantis.file.typeadapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterfaceTypeAdapterTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Shape.class, new InterfaceTypeAdapter<Shape>())
            .create();

    @Test
    void roundTripsTheImplementation() {
        Shape square = new Square(4);

        var json = this.gson.toJson(square, Shape.class);

        assertThat(json).isEqualTo("{\"type\":\"" + Square.class.getName() + "\",\"data\":{\"side\":4}}");
        assertThat(this.gson.fromJson(json, Shape.class)).isEqualTo(square);
    }

    @Test
    void unknownTypeFails() {
        assertThatThrownBy(() -> this.gson.fromJson("{\"type\":\"does.not.Exist\",\"data\":{}}", Shape.class))
                .isInstanceOf(JsonParseException.class)
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void missingMembersFail() {
        assertThatThrownBy(() -> this.gson.fromJson("{\"data\":{}}", Shape.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("'type'");
        assertThatThrownBy(() -> this.gson.fromJson("{\"type\":\"" + Square.class.getName() + "\"}", Shape.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("'data'");
    }

    interface Shape {}

    record Square(int side) implements Shape {}
}
