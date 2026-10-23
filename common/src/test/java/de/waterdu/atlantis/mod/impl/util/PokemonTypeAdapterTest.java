package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.FakePokemonDelegate;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class PokemonTypeAdapterTest {

    @Test
    void roundTripsThroughTheDelegate() {
        var delegate = FakePokemonDelegate.install();
        Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(UPokemon.class, new PokemonTypeAdapter()).create();
        var pokemon = delegate.spec("pikachu shiny").create();

        var json = gson.toJson(pokemon, UPokemon.class);
        var copy = gson.fromJson(json, UPokemon.class);

        assertThat(json).isEqualTo(pokemon.toJson().toString());
        assertThat(copy.uuid()).isEqualTo(pokemon.uuid());
        assertThat(copy.species().name()).isEqualTo("Pikachu");
        assertThat(copy.isShiny()).isTrue();
    }
}
