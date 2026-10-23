package de.waterdu.atlantis.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.waterdu.atlantis.mod.ModExtension;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal {@link PokemonDelegate} for JUnit tests. A spec is a space-separated list of requirements whose first
 * word is the species; the requirement "shiny" makes created Pokémon shiny. Use with {@link MinecraftBootstrapped}.
 */
public final class FakePokemonDelegate implements PokemonDelegate {

    private static final ModObjectKey TYPE = new ModObjectKey("atlantis_test", "pokemon");

    /** How often {@link #spec(String...)} was called. */
    public final AtomicInteger specCalls = new AtomicInteger();

    /** Creates the delegate and activates it as {@code Mods.pokemon()}. Requires {@link AtlantisTestEnvironment}. */
    public static FakePokemonDelegate install() {
        var delegate = new FakePokemonDelegate();
        Mods.activateForTesting(ModExtension.builder()
                .modId("fake_pokemon")
                .mainClass(FakePokemonDelegate.class)
                .proxy(ModExtension.Loader.ALL, () -> delegate)
                .delegates(PokemonDelegate.class)
                .language(ModExtension.Language.JAVA)
                .build());
        return delegate;
    }

    @Override
    public UParty<?, ?> party(PlayerReference player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UPC<?, ?> pc(PlayerReference player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spec spec(String... specs) {
        this.specCalls.incrementAndGet();
        return new Spec(String.join(" ", specs).trim());
    }

    @Override
    public String specDelimiter() {
        return ":";
    }

    @Override
    public Species species(String name) {
        return new Species(name.isEmpty() ? name : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT));
    }

    @Override
    public Species species(int dex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pokemon pokemonFromTag(CompoundTag tag) {
        return new Pokemon(tag.getUUID("uuid"), this.species(tag.getString("species")), tag.getBoolean("shiny"));
    }

    @Override
    public Pokemon pokemonFromJson(JsonElement json) {
        var object = json.getAsJsonObject();
        return new Pokemon(UUID.fromString(object.get("uuid").getAsString()), this.species(object.get("species").getAsString()), object.get("shiny").getAsBoolean());
    }

    public final class Spec extends UPokemonSpec<String, Pokemon> {

        private final List<String> requirements;

        private Spec(String spec) {
            super(TYPE, spec);
            this.requirements = spec.isEmpty() ? List.of() : Arrays.asList(spec.split(" "));
        }

        @Override
        public Pokemon create() {
            return new Pokemon(UUID.randomUUID(), FakePokemonDelegate.this.species(this.requirements.getFirst()), this.requirements.contains("shiny"));
        }

        @Override
        public ItemStack item() {
            return new ItemStack(Items.PAPER);
        }

        @Override
        public boolean matches(Pokemon pokemon) {
            return pokemon.species().name().equalsIgnoreCase(this.requirements.getFirst()) && (!this.requirements.contains("shiny") || pokemon.isShiny());
        }

        @Override
        public int countRequirements() {
            return this.requirements.size();
        }

        @Override
        public String originalString() {
            return this.dereference();
        }
    }

    public static final class Species extends USpecies<String> {

        private Species(String name) {
            super(TYPE, name);
        }

        @Override
        public String name() {
            return this.dereference();
        }

        @Override
        public int dex() {
            return this.name().length();
        }
    }

    public static final class Pokemon extends UPokemon<UUID> {

        private final Species species;
        private final boolean shiny;

        private Pokemon(UUID uuid, Species species, boolean shiny) {
            super(TYPE, uuid);
            this.species = species;
            this.shiny = shiny;
        }

        @Override
        public UUID uuid() {
            return this.dereference();
        }

        @Override
        public Species species() {
            return this.species;
        }

        @Override
        public UForm<?> form() {
            return null;
        }

        @Override
        public UIVs<?, ?> ivs() {
            return null;
        }

        @Override
        public UEVs<?, ?> evs() {
            return null;
        }

        @Override
        public boolean isShiny() {
            return this.shiny;
        }

        @Override
        public CompoundTag toCompoundTag() {
            var tag = new CompoundTag();
            tag.putUUID("uuid", this.uuid());
            tag.putString("species", this.species.name());
            tag.putBoolean("shiny", this.shiny);
            return tag;
        }

        @Override
        public JsonElement toJson() {
            var json = new JsonObject();
            json.addProperty("uuid", this.uuid().toString());
            json.addProperty("species", this.species.name());
            json.addProperty("shiny", this.shiny);
            return json;
        }
    }
}
