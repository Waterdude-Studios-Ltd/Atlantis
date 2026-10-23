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

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class StoredPokemonTest {

    private FakePokemonDelegate delegate;
    private final Gson gson = new GsonBuilder().serializeNulls().registerTypeAdapter(StoredPokemon.class, new StoredPokemon.TypeAdapter()).create();

    @BeforeEach
    void installDelegate() {
        this.delegate = FakePokemonDelegate.install();
    }

    @Test
    void emptyHasNoPokemon() {
        var stored = StoredPokemon.empty();

        assertThat(stored.isEmpty()).isTrue();
        assertThat(stored.actual()).isNull();
        assertThat(stored.uuid()).isEqualTo(UUIDUtils.ZEROED_UUID);
    }

    @Test
    void ofPokemonKeepsTheInstanceAndItsTag() {
        var pokemon = this.delegate.spec("pikachu").create();

        var stored = StoredPokemon.of(pokemon);

        assertThat(stored.isEmpty()).isFalse();
        assertThat(stored.actual()).isSameAs(pokemon);
        assertThat(stored.uuid()).isEqualTo(pokemon.uuid());
    }

    @Test
    void ofTagDecodesThroughTheDelegateOnce() {
        var tag = this.delegate.spec("eevee shiny").create().toCompoundTag();

        var stored = StoredPokemon.of(tag);
        var actual = stored.actual();

        assertThat(actual.species().name()).isEqualTo("Eevee");
        assertThat(actual.isShiny()).isTrue();
        assertThat(stored.actual()).isSameAs(actual);
    }

    @Test
    void emptyTagIsEmpty() {
        assertThat(StoredPokemon.of(new CompoundTag()).isEmpty()).isTrue();
    }

    @Test
    void ofSpecCreatesAPokemon() {
        assertThat(StoredPokemon.of("pikachu").actual().species().name()).isEqualTo("Pikachu");
        assertThat(StoredPokemon.of(this.delegate.spec("eevee")).actual().species().name()).isEqualTo("Eevee");
        assertThat(StoredPokemon.of(ConfigurablePokemon.of("mew")).actual().species().name()).isEqualTo("Mew");
    }

    @Test
    void setAndClear() {
        var stored = StoredPokemon.empty();
        var pokemon = this.delegate.spec("pikachu").create();

        stored.set(pokemon);
        assertThat(stored.actual()).isSameAs(pokemon);

        stored.clear();
        assertThat(stored.isEmpty()).isTrue();
    }

    @Test
    void equalityUsesTheUuid() {
        var pokemon = this.delegate.spec("pikachu").create();
        var stored = StoredPokemon.of(pokemon);

        assertThat(stored).isEqualTo(StoredPokemon.of(pokemon.toCompoundTag())).hasSameHashCodeAs(StoredPokemon.of(pokemon));
        assertThat(stored).isNotEqualTo(StoredPokemon.of("pikachu"));
        assertThat(StoredPokemon.empty()).isEqualTo(StoredPokemon.empty());
        assertThat(stored).hasToString("StoredPokemon(" + pokemon.uuid() + ")");
    }

    @Test
    void typeAdapterRoundTrips() {
        var pokemon = this.delegate.spec("eevee shiny").create();

        var json = this.gson.toJson(StoredPokemon.of(pokemon));
        var copy = this.gson.fromJson(json, StoredPokemon.class);

        assertThat(copy.uuid()).isEqualTo(pokemon.uuid());
        assertThat(copy.actual().isShiny()).isTrue();
    }

    @Test
    void typeAdapterRoundTripsAnEmptySlot() {
        var json = this.gson.toJson(new Holder(StoredPokemon.empty()));

        assertThat(json).isEqualTo("{\"slot\":\"\"}");
        assertThat(this.gson.fromJson(json, Holder.class).slot.isEmpty()).isTrue();
    }

    @Test
    void invalidTagDeserializesToEmpty() {
        assertThat(this.gson.fromJson("\"{not snbt\"", StoredPokemon.class).isEmpty()).isTrue();
    }

    @Test
    void emptyArrayHoldsDistinctEmptySlots() {
        var slots = StoredPokemon.emptyArrayOfSize(3);

        assertThat(slots).hasSize(3).allMatch(StoredPokemon::isEmpty);
        assertThat(slots[0]).isNotSameAs(slots[1]).isNotSameAs(slots[2]);
        assertThat(slots[1]).isNotSameAs(slots[2]);
    }

    private record Holder(StoredPokemon slot) {}
}
