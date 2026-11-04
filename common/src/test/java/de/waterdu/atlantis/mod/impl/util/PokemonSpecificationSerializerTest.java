package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.FakePokemonDelegate;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link PokemonSpecificationSerializer}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class PokemonSpecificationSerializerTest {

    private FakePokemonDelegate delegate;

    /**
     * Installs a new {@link FakePokemonDelegate} before each test.
     */
    @BeforeEach
    void installDelegate() {
        this.delegate = FakePokemonDelegate.install();
    }

    /**
     * Checks that each requirement becomes a JSON boolean, number or string, as its value type requires.
     */
    @Test
    void serializesEachRequirementByType() {
        var json = PokemonSpecificationSerializer.serialize("pikachu shiny !egg lvl:5 iv:0.5 ability:static");

        assertThat(json).isEqualTo(JsonParser.parseString("""
                {"species": "Pikachu", "shiny": true, "egg": false, "lvl": 5, "iv": 0.5, "ability": "static"}
                """));
    }

    /**
     * Checks that a whole number does not get a decimal point when it is serialized and deserialized.
     */
    @Test
    void wholeNumbersStayWhole() {
        var json = PokemonSpecificationSerializer.serialize("pikachu lvl:5");

        assertThat(json.get("lvl").toString()).isEqualTo("5");
        assertThat(PokemonSpecificationSerializer.deserialize(json).originalSpec()).isEqualTo("Pikachu lvl:5");
    }

    /**
     * Checks that the serializer gets the species name from the Pokémon delegate.
     */
    @Test
    void speciesIsNormalisedThroughTheDelegate() {
        assertThat(PokemonSpecificationSerializer.serialize("PIKACHU").get("species").getAsString()).isEqualTo("Pikachu");
    }

    /**
     * Checks that deserialization puts the requirements in the spec in the order of the JSON properties.
     */
    @Test
    void deserializeBuildsASpecInJsonOrder() {
        var json = JsonParser.parseString("""
                {"species": "Eevee", "shiny": true, "lvl": 30, "iv": 0.25, "nature": "timid"}
                """).getAsJsonObject();

        assertThat(PokemonSpecificationSerializer.deserialize(json).originalSpec()).isEqualTo("Eevee shiny:true lvl:30 iv:0.25 nature:timid");
    }

    /**
     * Checks that the string, {@link ConfigurablePokemon} and spec overloads of {@code serialize} give the same JSON.
     */
    @Test
    void serializeOverloadsAgree() {
        var expected = PokemonSpecificationSerializer.serialize("eevee shiny");

        assertThat(PokemonSpecificationSerializer.serialize(ConfigurablePokemon.of("eevee shiny"))).isEqualTo(expected);
        assertThat(PokemonSpecificationSerializer.serialize(this.delegate.spec("eevee shiny"))).isEqualTo(expected);
    }

    /**
     * Checks that an empty JSON object deserializes to a spec with an empty string.
     */
    @Test
    void emptyObjectDeserializesToAnEmptySpec() {
        assertThat(PokemonSpecificationSerializer.deserialize(new JsonObject()).originalSpec()).isEmpty();
    }
}
