package de.waterdu.atlantis.mod.proxy.delegate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.waterdu.atlantis.mod.impl.util.ConfigurablePokemon;
import de.waterdu.atlantis.mod.impl.util.PokemonTypeAdapter;
import de.waterdu.atlantis.mod.impl.util.ShowdownFormat;
import de.waterdu.atlantis.mod.impl.util.StoredPokemon;
import de.waterdu.atlantis.mod.proxy.ModProxy;
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
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gives common code access to the active Pokémon mod, such as Pixelmon or Cobblemon. Get it with
 * {@code Mods.pokemon()}.
 */
public interface PokemonDelegate extends ModProxy {

    /**
     * Registers the type adapters for {@link StoredPokemon}, {@link ConfigurablePokemon} and every {@link UPokemon}.
     *
     * @param gsonBuilder the builder to register the type adapters on
     */
    @Override
    default void registerTypeAdapters(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(StoredPokemon.class, new StoredPokemon.TypeAdapter())
                .registerTypeAdapter(ConfigurablePokemon.class, new ConfigurablePokemon.TypeAdapter())
                .registerTypeHierarchyAdapter(UPokemon.class, new PokemonTypeAdapter());
    }

    /**
     * Creates the item preprocessors for Pokémon. One turns the rest of the input into a spec and gets its item. The
     * other gets the item for a Poké Ball name with {@link #pokeBallItem}.
     *
     * @return the item preprocessors
     */
    @Override
    default ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return new ConfigurableItemStack.Preprocessor[]{
                ConfigurableItemStack.Preprocessor.untilEnd(spec -> this.spec(spec).item()),
                ConfigurableItemStack.Preprocessor.single(this::pokeBallItem)
        };
    }

    /**
     * Gets the item for the named Poké Ball. If no ball has that name, gets the item for a plain Poké Ball.
     */
    default ItemStack pokeBallItem(String name) {
        UPokeBall<?> ball = this.pokeBall(name);
        if (ball == null) {
            ball = this.pokeBall("poke_ball");
        }
        return ball == null ? ItemStack.EMPTY : ball.item();
    }

    /**
     * Gets the party of the player.
     *
     * @param player the player
     * @return the party of the player
     */
    UParty<?, ?> party(PlayerReference player);

    /**
     * Gets the PC of the player.
     *
     * @param player the player
     * @return the PC of the player
     */
    UPC<?, ?> pc(PlayerReference player);

    /**
     * Gets the Pokédex of the player.
     *
     * @param player the player
     * @return the Pokédex of the player
     */
    UPokedex<?> pokedex(PlayerReference player);

    /**
     * Checks if the player has chosen a starter Pokémon.
     *
     * @param player the player
     * @return {@code true} if the player has chosen a starter
     */
    boolean hasChosenStarter(PlayerReference player);

    /**
     * Sets whether the player has chosen a starter Pokémon.
     *
     * @param player the player
     * @param chosen {@code true} to mark the starter as chosen
     */
    void setChosenStarter(PlayerReference player, boolean chosen);

    /**
     * Gives the Pokémon to the player's party, or to their PC if the party is full.
     */
    default boolean give(PlayerReference player, UPokemon<?> pokemon) {
        return this.party(player).add(pokemon);
    }

    /**
     * Searches for the Pokémon in the player's party, then in their PC.
     */
    default @Nullable UPokemon<?> find(PlayerReference player, UUID uuid) {
        UPokemon<?> pokemon = this.party(player).find(uuid);
        return pokemon == null ? this.pc(player).find(uuid) : pokemon;
    }

    /**
     * Parses a Pokémon spec.
     *
     * @param specs the parts of the spec
     * @return the parsed spec
     */
    UPokemonSpec<?, ?> spec(String... specs);

    /**
     * The separator between a key and its value in a spec. This is {@code :} on Pixelmon and {@code =} on Cobblemon.
     */
    String specDelimiter();

    /**
     * Finds a species by name. If there is no such species, returns the mod's placeholder species. Use
     * {@link #hasSpecies(String)} to check first.
     */
    USpecies<?> species(String name);

    /**
     * Finds a species by its National Pokédex number. If there is no such species, returns the mod's placeholder species.
     */
    USpecies<?> species(int dex);

    /**
     * Checks if a species with the name exists.
     *
     * @param name the name of the species
     * @return {@code true} if the species exists
     */
    boolean hasSpecies(String name);

    /**
     * @return all species of the mod
     */
    List<? extends USpecies<?>> allSpecies();

    /**
     * Gets a random species.
     *
     * @return a random species
     */
    USpecies<?> randomSpecies();

    /**
     * Finds an ability by name.
     *
     * @param name the name of the ability
     * @return the ability, or {@code null} if there is no such ability
     */
    @Nullable UAbility<?> ability(String name);

    /**
     * @return all abilities of the mod
     */
    List<? extends UAbility<?>> abilities();

    /**
     * Finds a move by name.
     *
     * @param name the name of the move
     * @return the move, or {@code null} if there is no such move
     */
    @Nullable UMoveTemplate<?> move(String name);

    /**
     * @return all moves of the mod
     */
    List<? extends UMoveTemplate<?>> moves();

    /**
     * Finds a Poké Ball by name.
     *
     * @param name the name of the Poké Ball
     * @return the Poké Ball, or {@code null} if there is no such Poké Ball
     */
    @Nullable UPokeBall<?> pokeBall(String name);

    /**
     * @return all Poké Balls of the mod
     */
    List<? extends UPokeBall<?>> pokeBalls();

    /**
     * Finds an elemental type by name.
     *
     * @param name the name of the elemental type
     * @return the elemental type, or {@code null} if there is no such type
     */
    @Nullable UElementalType<?> elementalType(String name);

    /**
     * @return all elemental types of the mod
     */
    List<? extends UElementalType<?>> elementalTypes();

    /**
     * Finds a Tera type by name.
     *
     * @param name the name of the Tera type
     * @return the Tera type, or {@code null} if there is no such type
     */
    @Nullable UTeraType<?> teraType(String name);

    /**
     * @return all Tera types of the mod
     */
    List<? extends UTeraType<?>> teraTypes();

    /**
     * Finds a stat by one of the names in {@link UStat}, such as {@link UStat#ATTACK}.
     */
    @Nullable UStat<?> stat(String name);

    /**
     * @return all stats of the mod
     */
    List<? extends UStat<?>> stats();

    /**
     * Gets all stats except {@link UStat#ACCURACY} and {@link UStat#EVASION}.
     */
    List<? extends UStat<?>> permanentStats();

    /**
     * Finds a nature by its English name, such as {@code adamant}.
     */
    @Nullable UNature<?> nature(String name);

    /**
     * @return all natures of the mod
     */
    List<? extends UNature<?>> natures();

    /**
     * Gets a random nature.
     *
     * @return a random nature
     */
    UNature<?> randomNature();

    /**
     * Finds a gender by one of the names in {@link UGender}.
     */
    @Nullable UGender<?> gender(String name);

    /**
     * @return all genders of the mod
     */
    List<? extends UGender<?>> genders();

    /**
     * Finds a persistent status by one of the names in {@link UStatus}.
     */
    @Nullable UStatus<?> status(String name);

    /**
     * @return all persistent statuses of the mod
     */
    List<? extends UStatus<?>> statuses();

    /**
     * Finds a move category by one of the names in {@link UMoveCategory}.
     */
    @Nullable UMoveCategory<?> moveCategory(String name);

    /**
     * @return all move categories of the mod
     */
    List<? extends UMoveCategory<?>> moveCategories();

    /**
     * If the tag is not valid, logs an error and returns a Pokémon of the mod's placeholder species.
     */
    UPokemon<?> pokemonFromTag(CompoundTag tag);

    /**
     * Reads JSON written by {@link UPokemon#toJson()}. If the JSON is not valid, logs an error and returns a Pokémon of
     * the mod's placeholder species.
     */
    UPokemon<?> pokemonFromJson(JsonElement json);

    /**
     * Wraps the entity if it is a Pokémon entity.
     *
     * @param entity the entity
     * @return the Pokémon entity, or {@code null} if the entity is not a Pokémon
     */
    @Nullable UPokemonEntity<?> pokemonEntity(Entity entity);

    /**
     * Checks if the entity is a Pokémon entity.
     *
     * @param entity the entity
     * @return {@code true} if {@link #pokemonEntity} returns a Pokémon entity
     */
    default boolean isPokemonEntity(Entity entity) {
        return this.pokemonEntity(entity) != null;
    }

    /**
     * Gets the battle that the player is in.
     *
     * @param player the player
     * @return the battle, or {@code null} if the player is not in a battle
     */
    @Nullable UBattle<?> battle(ServerPlayer player);

    /**
     * Checks if the player is in a battle.
     *
     * @param player the player
     * @return {@code true} if {@link #battle} returns a battle
     */
    default boolean isInBattle(ServerPlayer player) {
        return this.battle(player) != null;
    }

    /**
     * Starts a battle between the player's party and a wild Pokémon. If the battle cannot start, the future completes
     * with {@code null}. For example, the battle cannot start when either side is already in a battle.
     */
    CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild);

    /**
     * Starts a singles battle between two players' parties. If the battle cannot start, the future completes with
     * {@code null}.
     */
    default CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, ServerPlayer player2) {
        return this.startPlayerBattle(player1, null, player2, null, BattleSettings.defaults());
    }

    /**
     * Starts a battle between two players. A {@code null} team means the player's party. The battle uses an explicit
     * team as given. Thus, pass {@link BattleSettings#cloneTeams()} when the team contains Pokémon that are not in the
     * party. If the battle cannot start, the future completes with {@code null}.
     */
    CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings);

    /**
     * Starts a battle between a player and a trainer spawned with {@link #spawnTrainer}. The trainer uses its own team.
     * A {@code null} team means the player's party. If the battle cannot start, the future completes with
     * {@code null}. This includes when the trainer is already in a battle.
     */
    CompletableFuture<@Nullable UBattle<?>> startTrainerBattle(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, UTrainer<?> trainer, TrainerSettings trainerSettings, BattleSettings settings);

    /**
     * Spawns a stationary, invulnerable NPC trainer with no team and no interactions of its own.
     */
    UTrainer<?> spawnTrainer(ServerLevel level, Vec3 position, float yaw, Component name);

    /**
     * Wraps the entity if it is an NPC trainer.
     *
     * @param entity the entity
     * @return the trainer, or {@code null} if the entity is not a trainer
     */
    @Nullable UTrainer<?> trainer(Entity entity);

    /**
     * Checks if the entity is an NPC trainer.
     *
     * @param entity the entity
     * @return {@code true} if {@link #trainer} returns a trainer
     */
    default boolean isTrainer(Entity entity) {
        return this.trainer(entity) != null;
    }

    /**
     * Checks if the mod supports the feature.
     *
     * @param feature the feature
     * @return {@code true} if the mod supports the feature
     */
    boolean supports(PokemonFeature feature);

    /**
     * @return all battle clauses of the mod
     */
    List<? extends UBattleClause<?>> battleClauses();

    /**
     * Finds a battle clause by its id. The case of the id does not matter.
     *
     * @param id the id of the clause
     * @return the clause, or {@code null} if there is no such clause
     */
    default @Nullable UBattleClause<?> battleClause(String id) {
        for (var clause : this.battleClauses()) {
            if (clause.id().equalsIgnoreCase(id)) {
                return clause;
            }
        }
        return null;
    }

    /**
     * The names accepted by {@link TrainerSettings#ai()}.
     */
    default List<String> trainerAIs() {
        return List.of();
    }

    /**
     * The names accepted by {@link TrainerSettings#bossTier()}.
     */
    default List<String> bossTiers() {
        return List.of();
    }

    /**
     * The names accepted by {@link BattleSettings#gimmick()}.
     */
    default List<String> battleGimmicks() {
        return List.of();
    }

    /**
     * Parses Pokémon from Showdown team text. Skips sets whose species does not exist.
     */
    default List<UPokemon<?>> importShowdown(String text) {
        return ShowdownFormat.read(this, text, this.showdownFormAliases());
    }

    /**
     * Writes the team as Showdown team text.
     *
     * @param team the Pokémon to write
     * @return the Showdown team text
     */
    default String exportShowdown(List<? extends UPokemon<?>> team) {
        return ShowdownFormat.write(team);
    }

    /**
     * Maps Showdown forme names to this mod's form names where they differ.
     */
    default Map<String, String> showdownFormAliases() {
        return Map.of();
    }

    /**
     * Finds a held item by its Showdown name, such as {@code Choice Scarf}.
     */
    ItemStack showdownItem(String name);

    /**
     * Gets the items stored inside an item, such as a shulker box, a bundle or a mod's item bag.
     */
    default List<ItemStack> containedItems(ItemStack stack) {
        var items = new ArrayList<ItemStack>();
        var container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            container.nonEmptyItems().forEach(items::add);
        }
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            bundle.items().forEach(items::add);
        }
        return items;
    }

    /**
     * Finds the species' form for a Showdown forme name, such as {@code Alola} for {@code Ninetales-Alola}.
     */
    default @Nullable UForm<?> showdownForm(USpecies<?> species, String forme) {
        return ShowdownFormat.lookup(species::form, this.showdownFormAliases().getOrDefault(forme, forme));
    }
}
