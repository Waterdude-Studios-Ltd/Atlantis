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
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UTeraType;
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
 * A minimal {@link PokemonDelegate} for JUnit tests. A spec is a space-separated list of requirements. The first
 * word of a spec is the species. The requirement "shiny" makes created Pokémon shiny. All functions other than
 * specs, species and serialization throw {@link UnsupportedOperationException}. Use it with
 * {@link MinecraftBootstrapped}.
 */
public final class FakePokemonDelegate implements PokemonDelegate {

    private static final ModObjectKey TYPE = new ModObjectKey("atlantis_test", "pokemon");

    /**
     * The number of calls to {@link #spec(String...)}.
     */
    public final AtomicInteger specCalls = new AtomicInteger();

    /**
     * Creates the delegate and activates it as {@code Mods.pokemon()}. Requires {@link AtlantisTestEnvironment}.
     */
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

    /**
     * Creates the exception that the unsupported functions of the fake throw.
     *
     * @return a new {@link UnsupportedOperationException}
     */
    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by FakePokemonDelegate");
    }

    /**
     * Does not get a party, because the fake has no storage.
     *
     * @param player the player
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public UParty<?, ?> party(PlayerReference player) {
        throw unsupported();
    }

    /**
     * Does not get a PC, because the fake has no storage.
     *
     * @param player the player
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public UPC<?, ?> pc(PlayerReference player) {
        throw unsupported();
    }

    /**
     * Does not get a Pokédex, because the fake has no storage.
     *
     * @param player the player
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public UPokedex<?> pokedex(PlayerReference player) {
        throw unsupported();
    }

    /**
     * Does not check the starter, because the fake has no player data.
     *
     * @param player the player
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        throw unsupported();
    }

    /**
     * Does not set the starter, because the fake has no player data.
     *
     * @param player the player
     * @param chosen if the player has chosen a starter
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        throw unsupported();
    }

    /**
     * Creates a spec from the parts, joined with spaces, and counts the call in {@link #specCalls}.
     *
     * @param specs the parts of the spec
     * @return the new spec
     */
    @Override
    public Spec spec(String... specs) {
        this.specCalls.incrementAndGet();
        return new Spec(String.join(" ", specs).trim());
    }

    /**
     * @return {@code :}, the delimiter between a requirement and its value
     */
    @Override
    public String specDelimiter() {
        return ":";
    }

    /**
     * Gets a species with the name in title case. The name is not checked.
     *
     * @param name the name of the species
     * @return a new species with the capitalized name
     */
    @Override
    public Species species(String name) {
        return new Species(name.isEmpty() ? name : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT));
    }

    /**
     * Does not get a species by dex number, because the fake has no species list.
     *
     * @param dex the dex number
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public Species species(int dex) {
        throw unsupported();
    }

    /**
     * Checks if the name is not empty. Every name that is not empty counts as a species.
     *
     * @param name the name of the species
     * @return {@code true} if the name is not empty
     */
    @Override
    public boolean hasSpecies(String name) {
        return !name.isEmpty();
    }

    /**
     * Does not list the species, because the fake has no species list.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends USpecies<?>> allSpecies() {
        throw unsupported();
    }

    /**
     * Does not pick a species, because the fake has no species list.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public USpecies<?> randomSpecies() {
        throw unsupported();
    }

    /**
     * Does not get an ability, because the fake has no abilities.
     *
     * @param name the name of the ability
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UAbility<?> ability(String name) {
        throw unsupported();
    }

    /**
     * Does not list the abilities, because the fake has no abilities.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UAbility<?>> abilities() {
        throw unsupported();
    }

    /**
     * Does not get a move, because the fake has no moves.
     *
     * @param name the name of the move
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UMoveTemplate<?> move(String name) {
        throw unsupported();
    }

    /**
     * Does not list the moves, because the fake has no moves.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UMoveTemplate<?>> moves() {
        throw unsupported();
    }

    /**
     * Does not get a Poké Ball, because the fake has no Poké Balls.
     *
     * @param name the name of the Poké Ball
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UPokeBall<?> pokeBall(String name) {
        throw unsupported();
    }

    /**
     * Does not list the Poké Balls, because the fake has no Poké Balls.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UPokeBall<?>> pokeBalls() {
        throw unsupported();
    }

    /**
     * Does not get an elemental type, because the fake has no types.
     *
     * @param name the name of the type
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UElementalType<?> elementalType(String name) {
        throw unsupported();
    }

    /**
     * Does not list the elemental types, because the fake has no types.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UElementalType<?>> elementalTypes() {
        throw unsupported();
    }

    /**
     * Does not get a stat, because the fake has no stats.
     *
     * @param name the name of the stat
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UStat<?> stat(String name) {
        throw unsupported();
    }

    /**
     * Does not list the stats, because the fake has no stats.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UStat<?>> stats() {
        throw unsupported();
    }

    /**
     * Does not list the permanent stats, because the fake has no stats.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UStat<?>> permanentStats() {
        throw unsupported();
    }

    /**
     * Does not get a nature, because the fake has no natures.
     *
     * @param name the name of the nature
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UNature<?> nature(String name) {
        throw unsupported();
    }

    /**
     * Does not list the natures, because the fake has no natures.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UNature<?>> natures() {
        throw unsupported();
    }

    /**
     * Does not get a Tera type, because the fake has no Tera types.
     *
     * @param name the name of the Tera type
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UTeraType<?> teraType(String name) {
        throw unsupported();
    }

    /**
     * Does not list the Tera types, because the fake has no Tera types.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UTeraType<?>> teraTypes() {
        throw unsupported();
    }

    /**
     * Does not pick a nature, because the fake has no natures.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public UNature<?> randomNature() {
        throw unsupported();
    }

    /**
     * Does not get a gender, because the fake has no genders.
     *
     * @param name the name of the gender
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UGender<?> gender(String name) {
        throw unsupported();
    }

    /**
     * Does not list the genders, because the fake has no genders.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UGender<?>> genders() {
        throw unsupported();
    }

    /**
     * Does not get a status, because the fake has no statuses.
     *
     * @param name the name of the status
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UStatus<?> status(String name) {
        throw unsupported();
    }

    /**
     * Does not list the statuses, because the fake has no statuses.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UStatus<?>> statuses() {
        throw unsupported();
    }

    /**
     * Does not get a move category, because the fake has no move categories.
     *
     * @param name the name of the move category
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public @Nullable UMoveCategory<?> moveCategory(String name) {
        throw unsupported();
    }

    /**
     * Does not list the move categories, because the fake has no move categories.
     *
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<? extends UMoveCategory<?>> moveCategories() {
        throw unsupported();
    }

    /**
     * Reads a Pokémon from the {@code uuid}, {@code species} and {@code shiny} keys of the tag.
     *
     * @param tag the tag that {@link Pokemon#toCompoundTag()} wrote
     * @return the new Pokémon
     */
    @Override
    public Pokemon pokemonFromTag(CompoundTag tag) {
        return new Pokemon(tag.getUUID("uuid"), this.species(tag.getString("species")), tag.getBoolean("shiny"));
    }

    /**
     * Reads a Pokémon from the {@code uuid}, {@code species} and {@code shiny} properties of the JSON object.
     *
     * @param json the JSON object that {@link Pokemon#toJson()} wrote
     * @return the new Pokémon
     */
    @Override
    public Pokemon pokemonFromJson(JsonElement json) {
        var object = json.getAsJsonObject();
        return new Pokemon(UUID.fromString(object.get("uuid").getAsString()), this.species(object.get("species").getAsString()), object.get("shiny").getAsBoolean());
    }

    /**
     * Gets no Pokémon entity, because the fake has no entities.
     *
     * @param entity the entity
     * @return always {@code null}
     */
    @Override
    public @Nullable UPokemonEntity<?> pokemonEntity(Entity entity) {
        return null;
    }

    /**
     * Gets no battle, because the fake has no battles.
     *
     * @param player the player
     * @return always {@code null}
     */
    @Override
    public @Nullable UBattle<?> battle(ServerPlayer player) {
        return null;
    }

    /**
     * Does not start a wild battle, because the fake has no battles.
     *
     * @param player the player
     * @param wild the wild Pokémon
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        throw unsupported();
    }

    /**
     * Does not start a player battle, because the fake has no battles.
     *
     * @param player1 the first player
     * @param team1 the team of the first player
     * @param player2 the second player
     * @param team2 the team of the second player
     * @param settings the battle settings
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings) {
        throw unsupported();
    }

    /**
     * Does not start a trainer battle, because the fake has no battles.
     *
     * @param player the player
     * @param team the team of the player
     * @param trainer the trainer
     * @param trainerSettings the trainer settings
     * @param settings the battle settings
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startTrainerBattle(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, UTrainer<?> trainer, TrainerSettings trainerSettings, BattleSettings settings) {
        throw unsupported();
    }

    /**
     * Does not spawn a trainer, because the fake has no trainers.
     *
     * @param level the level
     * @param position the position
     * @param yaw the yaw
     * @param name the name of the trainer
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    public UTrainer<?> spawnTrainer(ServerLevel level, Vec3 position, float yaw, Component name) {
        throw unsupported();
    }

    /**
     * Gets no trainer, because the fake has no trainers.
     *
     * @param entity the entity
     * @return always {@code null}
     */
    @Override
    public @Nullable UTrainer<?> trainer(Entity entity) {
        return null;
    }

    /**
     * Checks if the fake supports a feature. It supports none.
     *
     * @param feature the feature
     * @return always {@code false}
     */
    @Override
    public boolean supports(PokemonFeature feature) {
        return false;
    }

    /**
     * @return an empty list
     */
    @Override
    public List<? extends UBattleClause<?>> battleClauses() {
        return List.of();
    }

    /**
     * Gets no Showdown item, because the fake has no items.
     *
     * @param name the name of the item
     * @return always {@link ItemStack#EMPTY}
     */
    @Override
    public ItemStack showdownItem(String name) {
        return ItemStack.EMPTY;
    }

    /**
     * A spec of space-separated requirements. The first requirement is the species.
     */
    public final class Spec extends UPokemonSpec<String, Pokemon> {

        private final List<String> requirements;

        /**
         * Creates a spec and splits it into requirements at each space.
         *
         * @param spec the spec, or an empty string for no requirements
         */
        private Spec(String spec) {
            super(TYPE, spec);
            this.requirements = spec.isEmpty() ? List.of() : Arrays.asList(spec.split(" "));
        }

        /**
         * Creates a Pokémon with a random UUID, of the species in the first requirement. It is shiny if a
         * requirement is {@code shiny}.
         *
         * @return the new Pokémon
         */
        @Override
        public Pokemon create() {
            return new Pokemon(UUID.randomUUID(), FakePokemonDelegate.this.species(this.requirements.getFirst()), this.requirements.contains("shiny"));
        }

        /**
         * @return a new paper item stack
         */
        @Override
        public ItemStack item() {
            return new ItemStack(Items.PAPER);
        }

        /**
         * Checks if the species name matches the first requirement, ignoring case. If a requirement is
         * {@code shiny}, the Pokémon must also be shiny.
         *
         * @param pokemon the Pokémon to check
         * @return {@code true} if the Pokémon matches the spec
         */
        @Override
        public boolean matches(UPokemon<?> pokemon) {
            return pokemon.species().name().equalsIgnoreCase(this.requirements.getFirst()) && (!this.requirements.contains("shiny") || pokemon.isShiny());
        }

        /**
         * @return the number of requirements, including the species
         */
        @Override
        public int countRequirements() {
            return this.requirements.size();
        }

        /**
         * @return the spec string that created this spec
         */
        @Override
        public String originalString() {
            return this.dereference();
        }
    }

    /**
     * A species that consists only of its name.
     */
    public static final class Species extends USpecies<String> {

        /**
         * Creates a species with the name.
         *
         * @param name the name of the species
         */
        private Species(String name) {
            super(TYPE, name);
        }

        /**
         * @return the name of the species
         */
        @Override
        public String name() {
            return this.dereference();
        }

        /**
         * @return the name of the species, in lower case
         */
        @Override
        public String id() {
            return this.dereference().toLowerCase(Locale.ROOT);
        }

        /**
         * @return the name of the species as a literal component
         */
        @Override
        public Component displayName() {
            return Component.literal(this.name());
        }

        /**
         * Gets a fake dex number, which is the length of the name.
         *
         * @return the length of the name
         */
        @Override
        public int dex() {
            return this.name().length();
        }

        /**
         * Does not get the default form, because the fake has no forms.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UForm<?> defaultForm() {
            throw unsupported();
        }

        /**
         * Does not list the forms, because the fake has no forms.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public List<? extends UForm<?>> forms() {
            throw unsupported();
        }

        /**
         * Does not get a form, because the fake has no forms.
         *
         * @param name the name of the form
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public @Nullable UForm<?> form(String name) {
            throw unsupported();
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isLegendary() {
            return false;
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isMythical() {
            return false;
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isUltraBeast() {
            return false;
        }

        /**
         * Creates a Pokémon of this species that is not shiny and has a random UUID. The level is ignored.
         *
         * @param level the level, which is not used
         * @return the new Pokémon
         */
        @Override
        public Pokemon create(int level) {
            return new Pokemon(UUID.randomUUID(), this, false);
        }
    }

    /**
     * A Pokémon that has only a UUID, a species and a shiny flag.
     */
    public static final class Pokemon extends UPokemon<UUID> {

        private final Species species;
        private final boolean shiny;

        /**
         * Creates a Pokémon.
         *
         * @param uuid the UUID of the Pokémon
         * @param species the species
         * @param shiny if the Pokémon is shiny
         */
        private Pokemon(UUID uuid, Species species, boolean shiny) {
            super(TYPE, uuid);
            this.species = species;
            this.shiny = shiny;
        }

        /**
         * @return the UUID of the Pokémon
         */
        @Override
        public UUID uuid() {
            return this.dereference();
        }

        /**
         * @return the species of the Pokémon
         */
        @Override
        public Species species() {
            return this.species;
        }

        /**
         * Does not set the species, because the fake Pokémon cannot change.
         *
         * @param species the new species
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setSpecies(USpecies<?> species) {
            throw unsupported();
        }

        /**
         * Gets no form, because the fake has no forms.
         *
         * @return always {@code null}
         */
        @Override
        public UForm<?> form() {
            return null;
        }

        /**
         * Does not set the form, because the fake has no forms.
         *
         * @param form the new form
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setForm(UForm<?> form) {
            throw unsupported();
        }

        /**
         * @return {@code true} if the Pokémon is shiny
         */
        @Override
        public boolean isShiny() {
            return this.shiny;
        }

        /**
         * Does not set the shiny flag, because the fake Pokémon cannot change.
         *
         * @param shiny if the Pokémon is shiny
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setShiny(boolean shiny) {
            throw unsupported();
        }

        /**
         * @return the display name of the species
         */
        @Override
        public Component displayName() {
            return this.species.displayName();
        }

        /**
         * Gets no nickname, because the fake has no nicknames.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable String nickname() {
            return null;
        }

        /**
         * Does not set the nickname, because the fake has no nicknames.
         *
         * @param nickname the new nickname
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setNickname(@Nullable String nickname) {
            throw unsupported();
        }

        /**
         * Does not get the level, because the fake has no levels.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int level() {
            throw unsupported();
        }

        /**
         * Does not set the level, because the fake has no levels.
         *
         * @param level the new level
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setLevel(int level) {
            throw unsupported();
        }

        /**
         * Does not get the experience, because the fake has no experience.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int experience() {
            throw unsupported();
        }

        /**
         * Does not set the experience, because the fake has no experience.
         *
         * @param experience the new experience
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setExperience(int experience) {
            throw unsupported();
        }

        /**
         * Does not get the experience to the next level, because the fake has no experience.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int experienceToNextLevel() {
            throw unsupported();
        }

        /**
         * Does not get the gender, because the fake has no genders.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UGender<?> gender() {
            throw unsupported();
        }

        /**
         * Does not set the gender, because the fake has no genders.
         *
         * @param gender the new gender
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setGender(UGender<?> gender) {
            throw unsupported();
        }

        /**
         * Does not get the nature, because the fake has no natures.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UNature<?> nature() {
            throw unsupported();
        }

        /**
         * Does not set the nature, because the fake has no natures.
         *
         * @param nature the new nature
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setNature(UNature<?> nature) {
            throw unsupported();
        }

        /**
         * Does not get the Tera type, because the fake has no Tera types.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UTeraType<?> teraType() {
            throw unsupported();
        }

        /**
         * Does not set the Tera type, because the fake has no Tera types.
         *
         * @param teraType the new Tera type
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setTeraType(UTeraType<?> teraType) {
            throw unsupported();
        }

        /**
         * Does not get the mint nature, because the fake has no natures.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public @Nullable UNature<?> mintNature() {
            throw unsupported();
        }

        /**
         * Does not set the mint nature, because the fake has no natures.
         *
         * @param nature the new mint nature
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setMintNature(@Nullable UNature<?> nature) {
            throw unsupported();
        }

        /**
         * Does not get the ability, because the fake has no abilities.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UAbility<?> ability() {
            throw unsupported();
        }

        /**
         * Does not set the ability, because the fake has no abilities.
         *
         * @param ability the new ability
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setAbility(UAbility<?> ability) {
            throw unsupported();
        }

        /**
         * Does not get the Poké Ball, because the fake has no Poké Balls.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UPokeBall<?> pokeBall() {
            throw unsupported();
        }

        /**
         * Does not set the Poké Ball, because the fake has no Poké Balls.
         *
         * @param ball the new Poké Ball
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setPokeBall(UPokeBall<?> ball) {
            throw unsupported();
        }

        /**
         * Does not get the moveset, because the fake has no moves.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UMoveset<?, ?> moveset() {
            throw unsupported();
        }

        /**
         * Does not get the held item, because the fake has no held items.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public ItemStack heldItem() {
            throw unsupported();
        }

        /**
         * Does not set the held item, because the fake has no held items.
         *
         * @param item the new held item
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setHeldItem(ItemStack item) {
            throw unsupported();
        }

        /**
         * Does not get the friendship, because the fake has no friendship.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int friendship() {
            throw unsupported();
        }

        /**
         * Does not set the friendship, because the fake has no friendship.
         *
         * @param friendship the new friendship
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setFriendship(int friendship) {
            throw unsupported();
        }

        /**
         * Does not get the health, because the fake has no health.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int health() {
            throw unsupported();
        }

        /**
         * Does not set the health, because the fake has no health.
         *
         * @param health the new health
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setHealth(int health) {
            throw unsupported();
        }

        /**
         * Does not get the maximum health, because the fake has no health.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int maxHealth() {
            throw unsupported();
        }

        /**
         * Does not heal the Pokémon, because the fake has no health.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public void heal() {
            throw unsupported();
        }

        /**
         * Does not get the status, because the fake has no statuses.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public @Nullable UStatus<?> status() {
            throw unsupported();
        }

        /**
         * Does not set the status, because the fake has no statuses.
         *
         * @param status the new status
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setStatus(@Nullable UStatus<?> status) {
            throw unsupported();
        }

        /**
         * Does not get a stat, because the fake has no stats.
         *
         * @param stat the stat
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int stat(UStat<?> stat) {
            throw unsupported();
        }

        /**
         * Gets no IVs, because the fake has no stats.
         *
         * @return always {@code null}
         */
        @Override
        public UIVs<?, ?> ivs() {
            return null;
        }

        /**
         * Gets no EVs, because the fake has no stats.
         *
         * @return always {@code null}
         */
        @Override
        public UEVs<?, ?> evs() {
            return null;
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isEgg() {
            return false;
        }

        /**
         * Gets no original trainer, because the fake has no trainers.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable String originalTrainer() {
            return null;
        }

        /**
         * Gets no original trainer UUID, because the fake has no trainers.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable UUID originalTrainerUUID() {
            return null;
        }

        /**
         * Does not set the original trainer, because the fake has no trainers.
         *
         * @param player the new original trainer
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setOriginalTrainer(ServerPlayer player) {
            throw unsupported();
        }

        /**
         * Gets no owner UUID, because the fake Pokémon has no owner.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable UUID ownerUUID() {
            return null;
        }

        /**
         * Gets no owner, because the fake Pokémon has no owner.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable ServerPlayer ownerPlayer() {
            return null;
        }

        /**
         * @return always {@code true}
         */
        @Override
        public boolean isTradeable() {
            return true;
        }

        /**
         * Does not set the tradeable flag, because the fake Pokémon cannot change.
         *
         * @param tradeable if the Pokémon is tradeable
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setTradeable(boolean tradeable) {
            throw unsupported();
        }

        /**
         * Does not get the Dynamax level, because the fake has no Dynamax.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public int dynamaxLevel() {
            throw unsupported();
        }

        /**
         * Does not set the Dynamax level, because the fake has no Dynamax.
         *
         * @param level the new Dynamax level
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setDynamaxLevel(int level) {
            throw unsupported();
        }

        /**
         * Does not check the Gigantamax factor, because the fake has no Dynamax.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public boolean hasGigantamaxFactor() {
            throw unsupported();
        }

        /**
         * Does not set the Gigantamax factor, because the fake has no Dynamax.
         *
         * @param factor if the Pokémon has the Gigantamax factor
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setGigantamaxFactor(boolean factor) {
            throw unsupported();
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isLegendary() {
            return false;
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isMythical() {
            return false;
        }

        /**
         * @return always {@code false}
         */
        @Override
        public boolean isUltraBeast() {
            return false;
        }

        /**
         * Does not get the persistent data, because the fake has no persistent data.
         *
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public CompoundTag persistentData() {
            throw unsupported();
        }

        /**
         * Gets no entity, because the fake has no entities.
         *
         * @return always {@code null}
         */
        @Override
        public @Nullable UPokemonEntity<?> entity() {
            return null;
        }

        /**
         * Does not spawn the Pokémon, because the fake has no entities.
         *
         * @param level the level
         * @param position the position
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        @Override
        public UPokemonEntity<?> spawn(ServerLevel level, Vec3 position) {
            throw unsupported();
        }

        /**
         * Creates a Pokémon with the same species and shiny flag, but a new random UUID.
         *
         * @return the copy
         */
        @Override
        public Pokemon copy() {
            return new Pokemon(UUID.randomUUID(), this.species, this.shiny);
        }

        /**
         * @return a new paper item stack
         */
        @Override
        public ItemStack toItem() {
            return new ItemStack(Items.PAPER);
        }

        /**
         * Writes the UUID, species name and shiny flag to the {@code uuid}, {@code species} and {@code shiny} keys.
         *
         * @return the new tag
         */
        @Override
        public CompoundTag toCompoundTag() {
            var tag = new CompoundTag();
            tag.putUUID("uuid", this.uuid());
            tag.putString("species", this.species.name());
            tag.putBoolean("shiny", this.shiny);
            return tag;
        }

        /**
         * Writes the UUID, species name and shiny flag to the {@code uuid}, {@code species} and {@code shiny}
         * properties.
         *
         * @return the new JSON object
         */
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
