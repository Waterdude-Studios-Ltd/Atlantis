package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon;

import com.google.gson.JsonElement;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UTeraType;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveset;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Wraps a Pokémon from the Pokémon mod.
 *
 * @param <T> the type of the wrapped Pokémon
 */
public abstract class UPokemon<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a Pokémon.
     *
     * @param type the key of the mod object type
     * @param pokemon the Pokémon to wrap
     */
    protected UPokemon(ModObjectKey type, T pokemon) {
        super(new ModObject(type, pokemon));
    }

    /**
     * @return the UUID of the Pokémon
     */
    public abstract UUID uuid();

    /**
     * @return the species of the Pokémon
     */
    public abstract USpecies<?> species();

    /**
     * Sets the species of the Pokémon.
     *
     * @param species the new species
     */
    public abstract void setSpecies(USpecies<?> species);

    /**
     * @return the form of the Pokémon
     */
    public abstract UForm<?> form();

    /**
     * Sets the form of the Pokémon.
     *
     * @param form the new form
     */
    public abstract void setForm(UForm<?> form);

    /**
     * @return the elemental types of the {@link #form()}
     */
    public List<? extends UElementalType<?>> types() {
        return this.form().types();
    }

    /**
     * @return the Tera type of the Pokémon
     */
    public abstract UTeraType<?> teraType();

    /**
     * Sets the Tera type of the Pokémon.
     *
     * @param teraType the new Tera type
     */
    public abstract void setTeraType(UTeraType<?> teraType);

    /**
     * Checks if the Pokémon is shiny.
     *
     * @return {@code true} if the Pokémon is shiny
     */
    public abstract boolean isShiny();

    /**
     * Sets if the Pokémon is shiny.
     *
     * @param shiny {@code true} to make the Pokémon shiny
     */
    public abstract void setShiny(boolean shiny);

    /**
     * @return the name of the Pokémon as a text component
     */
    public abstract Component displayName();

    /**
     * Gets the nickname of the Pokémon.
     *
     * @return the nickname, or {@code null} if the Pokémon has no nickname
     */
    public abstract @Nullable String nickname();

    /**
     * Sets the nickname of the Pokémon.
     *
     * @param nickname the new nickname, or {@code null} to remove it
     */
    public abstract void setNickname(@Nullable String nickname);

    /**
     * @return the level of the Pokémon
     */
    public abstract int level();

    /**
     * Sets the level of the Pokémon.
     *
     * @param level the new level
     */
    public abstract void setLevel(int level);

    /**
     * @return the experience of the Pokémon
     */
    public abstract int experience();

    /**
     * Sets the experience of the Pokémon.
     *
     * @param experience the new experience
     */
    public abstract void setExperience(int experience);

    /**
     * @return the experience that the Pokémon needs to reach the next level
     */
    public abstract int experienceToNextLevel();

    /**
     * @return the gender of the Pokémon
     */
    public abstract UGender<?> gender();

    /**
     * Sets the gender of the Pokémon.
     *
     * @param gender the new gender
     */
    public abstract void setGender(UGender<?> gender);

    /**
     * @return the nature of the Pokémon, without a mint
     */
    public abstract UNature<?> nature();

    /**
     * Sets the nature of the Pokémon.
     *
     * @param nature the new nature
     */
    public abstract void setNature(UNature<?> nature);

    /**
     * Gets the nature that a mint gives the Pokémon.
     *
     * @return the mint nature, or {@code null} if the Pokémon has no mint nature
     */
    public abstract @Nullable UNature<?> mintNature();

    /**
     * Sets the nature that a mint gives the Pokémon.
     *
     * @param nature the new mint nature, or {@code null} to remove it
     */
    public abstract void setMintNature(@Nullable UNature<?> nature);

    /**
     * Gets the nature that has an effect on the stats of the Pokémon.
     *
     * @return the {@link #mintNature()} if there is one, otherwise the {@link #nature()}
     */
    public UNature<?> effectiveNature() {
        var mint = this.mintNature();
        return mint == null ? this.nature() : mint;
    }

    /**
     * @return the ability of the Pokémon
     */
    public abstract UAbility<?> ability();

    /**
     * Sets the ability of the Pokémon.
     *
     * @param ability the new ability
     */
    public abstract void setAbility(UAbility<?> ability);

    /**
     * @return the Poké Ball that the Pokémon was caught in
     */
    public abstract UPokeBall<?> pokeBall();

    /**
     * Sets the Poké Ball of the Pokémon.
     *
     * @param ball the new Poké Ball
     */
    public abstract void setPokeBall(UPokeBall<?> ball);

    /**
     * @return the moves of the Pokémon
     */
    public abstract UMoveset<?, ?> moveset();

    /**
     * @return the item that the Pokémon holds
     */
    public abstract ItemStack heldItem();

    /**
     * Sets the item that the Pokémon holds.
     *
     * @param item the new held item
     */
    public abstract void setHeldItem(ItemStack item);

    /**
     * @return the friendship of the Pokémon
     */
    public abstract int friendship();

    /**
     * Sets the friendship of the Pokémon.
     *
     * @param friendship the new friendship
     */
    public abstract void setFriendship(int friendship);

    /**
     * @return the current health of the Pokémon
     */
    public abstract int health();

    /**
     * Sets the current health of the Pokémon.
     *
     * @param health the new health
     */
    public abstract void setHealth(int health);

    /**
     * @return the maximum health of the Pokémon
     */
    public abstract int maxHealth();

    /**
     * Heals the Pokémon with the heal method of the Pokémon mod.
     */
    public abstract void heal();

    /**
     * Checks if the Pokémon has fainted.
     *
     * @return {@code true} if the {@link #health()} is {@code 0} or less
     */
    public boolean isFainted() {
        return this.health() <= 0;
    }

    /**
     * Gets the status condition of the Pokémon.
     *
     * @return the status, or {@code null} if the Pokémon has no status
     */
    public abstract @Nullable UStatus<?> status();

    /**
     * Sets the status condition of the Pokémon.
     *
     * @param status the new status, or {@code null} to remove it
     */
    public abstract void setStatus(@Nullable UStatus<?> status);

    /**
     * @return the calculated value of the stat. This is {@code 0} for {@link UStat#ACCURACY} and {@link UStat#EVASION}.
     */
    public abstract int stat(UStat<?> stat);

    /**
     * @return the IVs of the Pokémon
     */
    public abstract UIVs<?, ?> ivs();

    /**
     * @return the EVs of the Pokémon
     */
    public abstract UEVs<?, ?> evs();

    /**
     * Checks if the Pokémon is an egg.
     *
     * @return {@code true} if the Pokémon is an egg
     */
    public abstract boolean isEgg();

    /**
     * Gets the name of the original trainer of the Pokémon.
     *
     * @return the name, or {@code null} if the Pokémon has no original trainer name
     */
    public abstract @Nullable String originalTrainer();

    /**
     * Gets the UUID of the original trainer of the Pokémon.
     *
     * @return the UUID, or {@code null} if the Pokémon has no original trainer UUID
     */
    public abstract @Nullable UUID originalTrainerUUID();

    /**
     * Sets the player as the original trainer of the Pokémon.
     *
     * @param player the new original trainer
     */
    public abstract void setOriginalTrainer(ServerPlayer player);

    /**
     * Gets the UUID of the owner of the Pokémon.
     *
     * @return the UUID, or {@code null} if the Pokémon has no owner
     */
    public abstract @Nullable UUID ownerUUID();

    /**
     * Gets the player that owns the Pokémon.
     *
     * @return the owner, or {@code null} if there is no owner player
     */
    public abstract @Nullable ServerPlayer ownerPlayer();

    /**
     * Checks if the Pokémon can be traded.
     *
     * @return {@code true} if the Pokémon is tradeable
     */
    public abstract boolean isTradeable();

    /**
     * Sets if the Pokémon can be traded.
     *
     * @param tradeable {@code true} to let the Pokémon be traded
     */
    public abstract void setTradeable(boolean tradeable);

    /**
     * @return the Dynamax level of the Pokémon
     */
    public abstract int dynamaxLevel();

    /**
     * Sets the Dynamax level of the Pokémon.
     *
     * @param level the new Dynamax level
     */
    public abstract void setDynamaxLevel(int level);

    /**
     * Checks if the Pokémon has the Gigantamax factor.
     *
     * @return {@code true} if the Pokémon has the Gigantamax factor
     */
    public abstract boolean hasGigantamaxFactor();

    /**
     * Sets if the Pokémon has the Gigantamax factor.
     *
     * @param factor {@code true} to give the Pokémon the Gigantamax factor
     */
    public abstract void setGigantamaxFactor(boolean factor);

    /**
     * Checks if the Pokémon is legendary.
     *
     * @return {@code true} if the Pokémon is legendary
     */
    public abstract boolean isLegendary();

    /**
     * Checks if the Pokémon is mythical.
     *
     * @return {@code true} if the Pokémon is mythical
     */
    public abstract boolean isMythical();

    /**
     * Checks if the Pokémon is an Ultra Beast.
     *
     * @return {@code true} if the Pokémon is an Ultra Beast
     */
    public abstract boolean isUltraBeast();

    /**
     * @return the Pokémon's own tag for custom data. Changes to it are saved with the Pokémon.
     */
    public abstract CompoundTag persistentData();

    /**
     * Gets the entity of the Pokémon in the world.
     *
     * @return the entity, or {@code null} if the Pokémon has no entity
     */
    public abstract @Nullable UPokemonEntity<?> entity();

    /**
     * If the Pokémon already has an entity, returns that entity and spawns nothing.
     */
    public abstract UPokemonEntity<?> spawn(ServerLevel level, Vec3 position);

    /**
     * Creates a copy of the Pokémon.
     *
     * @return the copy
     */
    public abstract UPokemon<?> copy();

    /**
     * @return a display item that shows the Pokémon
     */
    public abstract ItemStack toItem();

    /**
     * Saves the Pokémon to a new tag.
     *
     * @return the tag that holds the Pokémon
     */
    public abstract CompoundTag toCompoundTag();

    /**
     * The format depends on the mod. Read the result with {@code PokemonDelegate#pokemonFromJson}.
     */
    public abstract JsonElement toJson();
}
