package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.FakePokemonDelegate;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.java.UUIDUtils;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link StoredPokemon}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class StoredPokemonTest {

    private FakePokemonDelegate delegate;
    private final Gson gson = new GsonBuilder().serializeNulls().registerTypeAdapter(StoredPokemon.class, new StoredPokemon.TypeAdapter()).create();

    /**
     * Installs a new {@link FakePokemonDelegate} before each test.
     */
    @BeforeEach
    void installDelegate() {
        this.delegate = FakePokemonDelegate.install();
    }

    /**
     * Checks that an empty stored Pokémon has no Pokémon and a zeroed UUID.
     */
    @Test
    void emptyHasNoPokemon() {
        var stored = StoredPokemon.empty();

        assertThat(stored.isEmpty()).isTrue();
        assertThat(stored.actual()).isNull();
        assertThat(stored.uuid()).isEqualTo(UUIDUtils.ZEROED_UUID);
    }

    /**
     * Checks that a Pokémon stored from an instance keeps that instance and its UUID.
     */
    @Test
    void ofPokemonKeepsTheInstanceAndItsTag() {
        var pokemon = this.delegate.spec("pikachu").create();

        var stored = StoredPokemon.of(pokemon);

        assertThat(stored.isEmpty()).isFalse();
        assertThat(stored.actual()).isSameAs(pokemon);
        assertThat(stored.uuid()).isEqualTo(pokemon.uuid());
    }

    /**
     * Checks that a Pokémon stored from a tag decodes the tag through the delegate, and caches the result.
     */
    @Test
    void ofTagDecodesThroughTheDelegateOnce() {
        var tag = this.delegate.spec("eevee shiny").create().toCompoundTag();

        var stored = StoredPokemon.of(tag);
        var actual = stored.actual();

        assertThat(actual.species().name()).isEqualTo("Eevee");
        assertThat(actual.isShiny()).isTrue();
        assertThat(stored.actual()).isSameAs(actual);
    }

    /**
     * Checks that a Pokémon stored from an empty tag is empty.
     */
    @Test
    void emptyTagIsEmpty() {
        assertThat(StoredPokemon.of(new CompoundTag()).isEmpty()).isTrue();
    }

    /**
     * Checks that a string spec, a spec and a {@link ConfigurablePokemon} each create a Pokémon of the correct
     * species.
     */
    @Test
    void ofSpecCreatesAPokemon() {
        assertThat(StoredPokemon.of("pikachu").actual().species().name()).isEqualTo("Pikachu");
        assertThat(StoredPokemon.of(this.delegate.spec("eevee")).actual().species().name()).isEqualTo("Eevee");
        assertThat(StoredPokemon.of(ConfigurablePokemon.of("mew")).actual().species().name()).isEqualTo("Mew");
    }

    /**
     * Checks that {@link StoredPokemon#set} stores a Pokémon and {@link StoredPokemon#clear} removes it.
     */
    @Test
    void setAndClear() {
        var stored = StoredPokemon.empty();
        var pokemon = this.delegate.spec("pikachu").create();

        stored.set(pokemon);
        assertThat(stored.actual()).isSameAs(pokemon);

        stored.clear();
        assertThat(stored.isEmpty()).isTrue();
    }

    /**
     * Checks that equality, the hash code and {@code toString} use the UUID of the Pokémon.
     */
    @Test
    void equalityUsesTheUuid() {
        var pokemon = this.delegate.spec("pikachu").create();
        var stored = StoredPokemon.of(pokemon);

        assertThat(stored).isEqualTo(StoredPokemon.of(pokemon.toCompoundTag())).hasSameHashCodeAs(StoredPokemon.of(pokemon));
        assertThat(stored).isNotEqualTo(StoredPokemon.of("pikachu"));
        assertThat(StoredPokemon.empty()).isEqualTo(StoredPokemon.empty());
        assertThat(stored).hasToString("StoredPokemon(" + pokemon.uuid() + ")");
    }

    /**
     * Checks that a Pokémon written and read through the type adapter keeps its UUID and shininess.
     */
    @Test
    void typeAdapterRoundTrips() {
        var pokemon = this.delegate.spec("eevee shiny").create();

        var json = this.gson.toJson(StoredPokemon.of(pokemon));
        var copy = this.gson.fromJson(json, StoredPokemon.class);

        assertThat(copy.uuid()).isEqualTo(pokemon.uuid());
        assertThat(copy.actual().isShiny()).isTrue();
    }

    /**
     * Checks that the type adapter writes an empty slot as an empty string and reads it back as empty.
     */
    @Test
    void typeAdapterRoundTripsAnEmptySlot() {
        var json = this.gson.toJson(new Holder(StoredPokemon.empty()));

        assertThat(json).isEqualTo("{\"slot\":\"\"}");
        assertThat(this.gson.fromJson(json, Holder.class).slot.isEmpty()).isTrue();
    }

    /**
     * Checks that the type adapter reads an invalid SNBT string as an empty slot.
     */
    @Test
    void invalidTagDeserializesToEmpty() {
        assertThat(this.gson.fromJson("\"{not snbt\"", StoredPokemon.class).isEmpty()).isTrue();
    }

    /**
     * Checks that {@link StoredPokemon#emptyArrayOfSize} fills the array with different empty instances.
     */
    @Test
    void emptyArrayHoldsDistinctEmptySlots() {
        var slots = StoredPokemon.emptyArrayOfSize(3);

        assertThat(slots).hasSize(3).allMatch(StoredPokemon::isEmpty);
        assertThat(slots[0]).isNotSameAs(slots[1]).isNotSameAs(slots[2]);
        assertThat(slots[1]).isNotSameAs(slots[2]);
    }

    /**
     * Holds a stored Pokémon as a field, so that the type adapter writes it inside an object.
     *
     * @param slot the stored Pokémon
     */
    private record Holder(StoredPokemon slot) {}
}
