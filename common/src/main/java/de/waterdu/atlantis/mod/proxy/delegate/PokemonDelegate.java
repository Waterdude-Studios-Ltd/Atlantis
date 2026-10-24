package de.waterdu.atlantis.mod.proxy.delegate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.waterdu.atlantis.mod.impl.util.ConfigurablePokemon;
import de.waterdu.atlantis.mod.impl.util.PokemonTypeAdapter;
import de.waterdu.atlantis.mod.impl.util.StoredPokemon;
import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveCategory;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PokemonDelegate extends ModProxy {

    @Override
    default void registerTypeAdapters(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(StoredPokemon.class, new StoredPokemon.TypeAdapter())
                .registerTypeAdapter(ConfigurablePokemon.class, new ConfigurablePokemon.TypeAdapter())
                .registerTypeHierarchyAdapter(UPokemon.class, new PokemonTypeAdapter());
    }

    @Override
    default ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return new ConfigurableItemStack.Preprocessor[]{
                ConfigurableItemStack.Preprocessor.untilEnd(spec -> this.spec(spec).item()),
                ConfigurableItemStack.Preprocessor.single(this::pokeBallItem)
        };
    }

    /**
     * Gets the item for the named Poké Ball, falling back to a plain Poké Ball if there is no ball with that name.
     */
    default ItemStack pokeBallItem(String name) {
        UPokeBall<?> ball = this.pokeBall(name);
        if (ball == null) {
            ball = this.pokeBall("poke_ball");
        }
        return ball == null ? ItemStack.EMPTY : ball.item();
    }

    UParty<?, ?> party(PlayerReference player);

    UPC<?, ?> pc(PlayerReference player);

    UPokedex<?> pokedex(PlayerReference player);

    boolean hasChosenStarter(PlayerReference player);

    void setChosenStarter(PlayerReference player, boolean chosen);

    /**
     * Gives the Pokémon to the player's party, or to their PC if the party is full.
     */
    default boolean give(PlayerReference player, UPokemon<?> pokemon) {
        return this.party(player).add(pokemon);
    }

    /**
     * Looks for the Pokémon in the player's party, then in their PC.
     */
    default @Nullable UPokemon<?> find(PlayerReference player, UUID uuid) {
        UPokemon<?> pokemon = this.party(player).find(uuid);
        return pokemon == null ? this.pc(player).find(uuid) : pokemon;
    }

    UPokemonSpec<?, ?> spec(String... specs);

    String specDelimiter();

    /**
     * Looks a species up by name, falling back to the mod's placeholder species if there is none. Use
     * {@link #hasSpecies(String)} to check first.
     */
    USpecies<?> species(String name);

    USpecies<?> species(int dex);

    boolean hasSpecies(String name);

    List<? extends USpecies<?>> allSpecies();

    USpecies<?> randomSpecies();

    @Nullable UAbility<?> ability(String name);

    List<? extends UAbility<?>> abilities();

    @Nullable UMoveTemplate<?> move(String name);

    List<? extends UMoveTemplate<?>> moves();

    @Nullable UPokeBall<?> pokeBall(String name);

    List<? extends UPokeBall<?>> pokeBalls();

    @Nullable UElementalType<?> elementalType(String name);

    List<? extends UElementalType<?>> elementalTypes();

    /**
     * Looks a stat up by one of the names in {@link UStat}, such as {@link UStat#ATTACK}.
     */
    @Nullable UStat<?> stat(String name);

    List<? extends UStat<?>> stats();

    List<? extends UStat<?>> permanentStats();

    /**
     * Looks a nature up by its English name, such as {@code adamant}.
     */
    @Nullable UNature<?> nature(String name);

    List<? extends UNature<?>> natures();

    UNature<?> randomNature();

    /**
     * Looks a gender up by one of the names in {@link UGender}.
     */
    @Nullable UGender<?> gender(String name);

    List<? extends UGender<?>> genders();

    /**
     * Looks a persistent status up by one of the names in {@link UStatus}.
     */
    @Nullable UStatus<?> status(String name);

    List<? extends UStatus<?>> statuses();

    /**
     * Looks a move category up by one of the names in {@link UMoveCategory}.
     */
    @Nullable UMoveCategory<?> moveCategory(String name);

    List<? extends UMoveCategory<?>> moveCategories();

    UPokemon<?> pokemonFromTag(CompoundTag tag);

    UPokemon<?> pokemonFromJson(JsonElement json);

    @Nullable UPokemonEntity<?> pokemonEntity(Entity entity);

    default boolean isPokemonEntity(Entity entity) {
        return this.pokemonEntity(entity) != null;
    }

    @Nullable UBattle<?> battle(ServerPlayer player);

    default boolean isInBattle(ServerPlayer player) {
        return this.battle(player) != null;
    }

    /**
     * Starts a battle between the player's party and a wild Pokémon. The future completes with {@code null} if the
     * battle could not start, for example because either side is already battling.
     */
    CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild);

    /**
     * Starts a singles battle between two players' parties. The future completes with {@code null} if the battle
     * could not start.
     */
    CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, ServerPlayer player2);
}
