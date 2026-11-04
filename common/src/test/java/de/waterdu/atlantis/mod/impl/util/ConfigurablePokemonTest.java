package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.FakePokemonDelegate;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ConfigurablePokemon}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class ConfigurablePokemonTest {

    private FakePokemonDelegate delegate;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(ConfigurablePokemon.class, new ConfigurablePokemon.TypeAdapter()).create();

    /**
     * Installs a new {@link FakePokemonDelegate} before each test.
     */
    @BeforeEach
    void installDelegate() {
        this.delegate = FakePokemonDelegate.install();
    }

    /**
     * Checks that {@link ConfigurablePokemon#of} joins its specs with spaces.
     */
    @Test
    void ofJoinsSpecsWithSpaces() {
        assertThat(ConfigurablePokemon.of("pikachu", "shiny").originalSpec()).isEqualTo("pikachu shiny");
    }

    /**
     * Checks that {@link ConfigurablePokemon#from} uses the original string of the spec.
     */
    @Test
    void fromUsesTheSpecsOriginalString() {
        assertThat(ConfigurablePokemon.from(this.delegate.spec("eevee shiny")).originalSpec()).isEqualTo("eevee shiny");
    }

    /**
     * Checks that the spec is parsed only on the first call to {@link ConfigurablePokemon#spec}, and then cached.
     */
    @Test
    void specIsResolvedLazilyAndCached() {
        var pokemon = ConfigurablePokemon.of("pikachu");
        assertThat(this.delegate.specCalls).hasValue(0);

        var spec = pokemon.spec();

        assertThat(pokemon.spec()).isSameAs(spec);
        assertThat(this.delegate.specCalls).hasValue(1);
    }

    /**
     * Checks that the requirement count is correct before the spec is parsed.
     */
    @Test
    void requirementCountWorksBeforeTheSpecIsResolved() {
        assertThat(ConfigurablePokemon.of("pikachu shiny").requirementCount()).isEqualTo(2);
    }

    /**
     * Checks that a Pokémon is valid only if its spec has requirements.
     */
    @Test
    void validityNeedsASpecWithRequirements() {
        assertThat(ConfigurablePokemon.of("pikachu").isValid()).isTrue();
        assertThat(ConfigurablePokemon.of("").isValid()).isFalse();
    }

    /**
     * Checks that {@link ConfigurablePokemon#instance} is cached, that {@link ConfigurablePokemon#newInstance} creates
     * a new Pokémon, and that the instance matches the spec.
     */
    @Test
    void instanceIsCachedButNewInstanceIsFresh() {
        var pokemon = ConfigurablePokemon.of("pikachu shiny");

        var instance = pokemon.instance();

        assertThat(pokemon.instance()).isSameAs(instance);
        assertThat(pokemon.newInstance()).isNotSameAs(instance);
        assertThat(instance.species().name()).isEqualTo("Pikachu");
        assertThat(instance.isShiny()).isTrue();
    }

    /**
     * Checks that each call to {@link ConfigurablePokemon#sprite} gives a new copy, so changes to one copy do not
     * affect the next.
     */
    @Test
    void spriteIsACopy() {
        var pokemon = ConfigurablePokemon.of("pikachu");

        var sprite = pokemon.sprite();
        sprite.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));

        assertThat(sprite.is(Items.PAPER)).isTrue();
        assertThat(pokemon.sprite()).isNotSameAs(sprite);
        assertThat(pokemon.sprite().has(DataComponents.CUSTOM_NAME)).isFalse();
    }

    /**
     * Checks that {@link ConfigurablePokemon#stored} holds the cached instance.
     */
    @Test
    void storedWrapsTheCachedInstance() {
        var pokemon = ConfigurablePokemon.of("pikachu");

        assertThat(pokemon.stored().actual()).isSameAs(pokemon.instance());
    }

    /**
     * Checks that equality and the hash code ignore the case of the spec, and checks {@code toString}.
     */
    @Test
    void equalityIgnoresCase() {
        var pokemon = ConfigurablePokemon.of("Pikachu shiny");
        var lower = ConfigurablePokemon.of("pikachu SHINY");

        assertThat(pokemon).isEqualTo(lower).hasSameHashCodeAs(lower);
        assertThat(pokemon).isNotEqualTo(ConfigurablePokemon.of("eevee"));
        assertThat(pokemon).hasToString("ConfigurablePokemon(Pikachu shiny)");
    }

    /**
     * Checks that the type adapter reads a spec from a JSON string and from a JSON object.
     */
    @Test
    void typeAdapterReadsStringsAndObjects() {
        assertThat(this.gson.fromJson("\"pikachu shiny\"", ConfigurablePokemon.class).originalSpec()).isEqualTo("pikachu shiny");
        assertThat(this.gson.fromJson("{\"species\":\"Pikachu\",\"shiny\":true}", ConfigurablePokemon.class).originalSpec()).isEqualTo("Pikachu shiny:true");
    }

    /**
     * Checks that the type adapter writes a spec as a JSON object.
     */
    @Test
    void typeAdapterWritesAnObject() {
        var json = this.gson.toJson(ConfigurablePokemon.of("pikachu shiny"), ConfigurablePokemon.class);

        var expected = new JsonObject();
        expected.addProperty("species", "Pikachu");
        expected.addProperty("shiny", true);
        assertThat(JsonParser.parseString(json)).isEqualTo(expected);
    }
}
