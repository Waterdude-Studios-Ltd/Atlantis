package de.waterdu.atlantis.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.waterdu.atlantis.mod.ModExtension;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonFeature;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.BattleSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.TrainerSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleClause;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UTrainer;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveCategory;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveset;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal {@link PokemonDelegate} for JUnit tests. A spec is a space-separated list of requirements whose first
 * word is the species; the requirement "shiny" makes created Pokémon shiny. Anything beyond specs, species and
 * serialisation throws {@link UnsupportedOperationException}. Use with {@link MinecraftBootstrapped}.
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

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by FakePokemonDelegate");
    }

    @Override
    public UParty<?, ?> party(PlayerReference player) {
        throw unsupported();
    }

    @Override
    public UPC<?, ?> pc(PlayerReference player) {
        throw unsupported();
    }

    @Override
    public UPokedex<?> pokedex(PlayerReference player) {
        throw unsupported();
    }

    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        throw unsupported();
    }

    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        throw unsupported();
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
        throw unsupported();
    }

    @Override
    public boolean hasSpecies(String name) {
        return !name.isEmpty();
    }

    @Override
    public List<? extends USpecies<?>> allSpecies() {
        throw unsupported();
    }

    @Override
    public USpecies<?> randomSpecies() {
        throw unsupported();
    }

    @Override
    public @Nullable UAbility<?> ability(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UAbility<?>> abilities() {
        throw unsupported();
    }

    @Override
    public @Nullable UMoveTemplate<?> move(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UMoveTemplate<?>> moves() {
        throw unsupported();
    }

    @Override
    public @Nullable UPokeBall<?> pokeBall(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UPokeBall<?>> pokeBalls() {
        throw unsupported();
    }

    @Override
    public @Nullable UElementalType<?> elementalType(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UElementalType<?>> elementalTypes() {
        throw unsupported();
    }

    @Override
    public @Nullable UStat<?> stat(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UStat<?>> stats() {
        throw unsupported();
    }

    @Override
    public List<? extends UStat<?>> permanentStats() {
        throw unsupported();
    }

    @Override
    public @Nullable UNature<?> nature(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UNature<?>> natures() {
        throw unsupported();
    }

    @Override
    public UNature<?> randomNature() {
        throw unsupported();
    }

    @Override
    public @Nullable UGender<?> gender(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UGender<?>> genders() {
        throw unsupported();
    }

    @Override
    public @Nullable UStatus<?> status(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UStatus<?>> statuses() {
        throw unsupported();
    }

    @Override
    public @Nullable UMoveCategory<?> moveCategory(String name) {
        throw unsupported();
    }

    @Override
    public List<? extends UMoveCategory<?>> moveCategories() {
        throw unsupported();
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

    @Override
    public @Nullable UPokemonEntity<?> pokemonEntity(Entity entity) {
        return null;
    }

    @Override
    public @Nullable UBattle<?> battle(ServerPlayer player) {
        return null;
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        throw unsupported();
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings) {
        throw unsupported();
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startTrainerBattle(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, UTrainer<?> trainer, TrainerSettings trainerSettings, BattleSettings settings) {
        throw unsupported();
    }

    @Override
    public UTrainer<?> spawnTrainer(ServerLevel level, Vec3 position, float yaw, Component name) {
        throw unsupported();
    }

    @Override
    public @Nullable UTrainer<?> trainer(Entity entity) {
        return null;
    }

    @Override
    public boolean supports(PokemonFeature feature) {
        return false;
    }

    @Override
    public List<? extends UBattleClause<?>> battleClauses() {
        return List.of();
    }

    @Override
    public ItemStack showdownItem(String name) {
        return ItemStack.EMPTY;
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
        public boolean matches(UPokemon<?> pokemon) {
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
        public String id() {
            return this.dereference().toLowerCase(Locale.ROOT);
        }

        @Override
        public Component displayName() {
            return Component.literal(this.name());
        }

        @Override
        public int dex() {
            return this.name().length();
        }

        @Override
        public UForm<?> defaultForm() {
            throw unsupported();
        }

        @Override
        public List<? extends UForm<?>> forms() {
            throw unsupported();
        }

        @Override
        public @Nullable UForm<?> form(String name) {
            throw unsupported();
        }

        @Override
        public boolean isLegendary() {
            return false;
        }

        @Override
        public boolean isMythical() {
            return false;
        }

        @Override
        public boolean isUltraBeast() {
            return false;
        }

        @Override
        public Pokemon create(int level) {
            return new Pokemon(UUID.randomUUID(), this, false);
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
        public void setSpecies(USpecies<?> species) {
            throw unsupported();
        }

        @Override
        public UForm<?> form() {
            return null;
        }

        @Override
        public void setForm(UForm<?> form) {
            throw unsupported();
        }

        @Override
        public boolean isShiny() {
            return this.shiny;
        }

        @Override
        public void setShiny(boolean shiny) {
            throw unsupported();
        }

        @Override
        public Component displayName() {
            return this.species.displayName();
        }

        @Override
        public @Nullable String nickname() {
            return null;
        }

        @Override
        public void setNickname(@Nullable String nickname) {
            throw unsupported();
        }

        @Override
        public int level() {
            throw unsupported();
        }

        @Override
        public void setLevel(int level) {
            throw unsupported();
        }

        @Override
        public int experience() {
            throw unsupported();
        }

        @Override
        public void setExperience(int experience) {
            throw unsupported();
        }

        @Override
        public int experienceToNextLevel() {
            throw unsupported();
        }

        @Override
        public UGender<?> gender() {
            throw unsupported();
        }

        @Override
        public void setGender(UGender<?> gender) {
            throw unsupported();
        }

        @Override
        public UNature<?> nature() {
            throw unsupported();
        }

        @Override
        public void setNature(UNature<?> nature) {
            throw unsupported();
        }

        @Override
        public @Nullable UNature<?> mintNature() {
            throw unsupported();
        }

        @Override
        public void setMintNature(@Nullable UNature<?> nature) {
            throw unsupported();
        }

        @Override
        public UAbility<?> ability() {
            throw unsupported();
        }

        @Override
        public void setAbility(UAbility<?> ability) {
            throw unsupported();
        }

        @Override
        public UPokeBall<?> pokeBall() {
            throw unsupported();
        }

        @Override
        public void setPokeBall(UPokeBall<?> ball) {
            throw unsupported();
        }

        @Override
        public UMoveset<?, ?> moveset() {
            throw unsupported();
        }

        @Override
        public ItemStack heldItem() {
            throw unsupported();
        }

        @Override
        public void setHeldItem(ItemStack item) {
            throw unsupported();
        }

        @Override
        public int friendship() {
            throw unsupported();
        }

        @Override
        public void setFriendship(int friendship) {
            throw unsupported();
        }

        @Override
        public int health() {
            throw unsupported();
        }

        @Override
        public void setHealth(int health) {
            throw unsupported();
        }

        @Override
        public int maxHealth() {
            throw unsupported();
        }

        @Override
        public void heal() {
            throw unsupported();
        }

        @Override
        public @Nullable UStatus<?> status() {
            throw unsupported();
        }

        @Override
        public void setStatus(@Nullable UStatus<?> status) {
            throw unsupported();
        }

        @Override
        public int stat(UStat<?> stat) {
            throw unsupported();
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
        public boolean isEgg() {
            return false;
        }

        @Override
        public @Nullable String originalTrainer() {
            return null;
        }

        @Override
        public @Nullable UUID originalTrainerUUID() {
            return null;
        }

        @Override
        public void setOriginalTrainer(ServerPlayer player) {
            throw unsupported();
        }

        @Override
        public @Nullable UUID ownerUUID() {
            return null;
        }

        @Override
        public @Nullable ServerPlayer ownerPlayer() {
            return null;
        }

        @Override
        public boolean isTradeable() {
            return true;
        }

        @Override
        public void setTradeable(boolean tradeable) {
            throw unsupported();
        }

        @Override
        public int dynamaxLevel() {
            throw unsupported();
        }

        @Override
        public void setDynamaxLevel(int level) {
            throw unsupported();
        }

        @Override
        public boolean hasGigantamaxFactor() {
            throw unsupported();
        }

        @Override
        public void setGigantamaxFactor(boolean factor) {
            throw unsupported();
        }

        @Override
        public boolean isLegendary() {
            return false;
        }

        @Override
        public boolean isMythical() {
            return false;
        }

        @Override
        public boolean isUltraBeast() {
            return false;
        }

        @Override
        public CompoundTag persistentData() {
            throw unsupported();
        }

        @Override
        public @Nullable UPokemonEntity<?> entity() {
            return null;
        }

        @Override
        public UPokemonEntity<?> spawn(ServerLevel level, Vec3 position) {
            throw unsupported();
        }

        @Override
        public Pokemon copy() {
            return new Pokemon(UUID.randomUUID(), this.species, this.shiny);
        }

        @Override
        public ItemStack toItem() {
            return new ItemStack(Items.PAPER);
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
