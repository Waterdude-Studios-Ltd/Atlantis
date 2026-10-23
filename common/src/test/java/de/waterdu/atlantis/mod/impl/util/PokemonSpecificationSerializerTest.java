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

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class PokemonSpecificationSerializerTest {

    private FakePokemonDelegate delegate;

    @BeforeEach
    void installDelegate() {
        this.delegate = FakePokemonDelegate.install();
    }

    @Test
    void serializesEachRequirementByType() {
        var json = PokemonSpecificationSerializer.serialize("pikachu shiny !egg lvl:5 iv:0.5 ability:static");

        assertThat(json).isEqualTo(JsonParser.parseString("""
                {"species": "Pikachu", "shiny": true, "egg": false, "lvl": 5, "iv": 0.5, "ability": "static"}
                """));
    }

    @Test
    void wholeNumbersStayWhole() {
        var json = PokemonSpecificationSerializer.serialize("pikachu lvl:5");

        assertThat(json.get("lvl").toString()).isEqualTo("5");
        assertThat(PokemonSpecificationSerializer.deserialize(json).originalSpec()).isEqualTo("Pikachu lvl:5");
    }

    @Test
    void speciesIsNormalisedThroughTheDelegate() {
        assertThat(PokemonSpecificationSerializer.serialize("PIKACHU").get("species").getAsString()).isEqualTo("Pikachu");
    }

    @Test
    void deserializeBuildsASpecInJsonOrder() {
        var json = JsonParser.parseString("""
                {"species": "Eevee", "shiny": true, "lvl": 30, "iv": 0.25, "nature": "timid"}
                """).getAsJsonObject();

        assertThat(PokemonSpecificationSerializer.deserialize(json).originalSpec()).isEqualTo("Eevee shiny:true lvl:30 iv:0.25 nature:timid");
    }

    @Test
    void serializeOverloadsAgree() {
        var expected = PokemonSpecificationSerializer.serialize("eevee shiny");

        assertThat(PokemonSpecificationSerializer.serialize(ConfigurablePokemon.of("eevee shiny"))).isEqualTo(expected);
        assertThat(PokemonSpecificationSerializer.serialize(this.delegate.spec("eevee shiny"))).isEqualTo(expected);
    }

    @Test
    void emptyObjectDeserializesToAnEmptySpec() {
        assertThat(PokemonSpecificationSerializer.deserialize(new JsonObject()).originalSpec()).isEmpty();
    }
}
