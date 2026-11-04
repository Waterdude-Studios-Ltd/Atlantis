package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity.CobblemonPokemonEntity;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonAbility;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonElementalType;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonForm;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonGender;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonNature;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonPokeBall;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonSpecies;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonStatus;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonTeraType;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves.CobblemonMoveset;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonEVs;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonIVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
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
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Wraps a Cobblemon Pokémon.
 */
public class CobblemonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokemon");

    /**
     * Creates a wrapper for a Cobblemon Pokémon.
     *
     * @param pokemon the Pokémon to wrap
     */
    public CobblemonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    /**
     * Throws an exception when the wrapped object is not a Cobblemon {@link Pokemon}.
     */
    public static Pokemon unwrap(UPokemon<?> pokemon) {
        if (pokemon.dereference() instanceof Pokemon it) {
            return it;
        }
        throw new IllegalArgumentException("Not a Cobblemon Pokémon: " + pokemon);
    }

    /**
     * Creates a wrapper for a Cobblemon Pokémon that may be {@code null}.
     *
     * @param pokemon the Pokémon to wrap
     * @return the wrapper, or {@code null} if the Pokémon is {@code null}
     */
    public static @Nullable CobblemonPokemon of(@Nullable Pokemon pokemon) {
        return pokemon == null ? null : new CobblemonPokemon(pokemon);
    }

    /**
     * @return the UUID of the Pokémon
     */
    @Override
    public UUID uuid() {
        return this.dereference().getUuid();
    }

    /**
     * @return a new wrapper for the species of the Pokémon
     */
    @Override
    public CobblemonSpecies species() {
        return new CobblemonSpecies(this.dereference().getSpecies());
    }

    /**
     * Sets the species of the Pokémon.
     *
     * @param species the Cobblemon species to set
     */
    @Override
    public void setSpecies(USpecies<?> species) {
        this.dereference().setSpecies((Species) species.dereference());
    }

    /**
     * @return a new wrapper for the form of the Pokémon
     */
    @Override
    public CobblemonForm form() {
        return new CobblemonForm(this.dereference().getForm());
    }

    /**
     * Sets the form of the Pokémon.
     *
     * @param form the Cobblemon form to set
     */
    @Override
    public void setForm(UForm<?> form) {
        this.dereference().setForm((FormData) form.dereference());
    }

    /**
     * @return a new wrapper for the Tera type of the Pokémon
     */
    @Override
    public CobblemonTeraType teraType() {
        return new CobblemonTeraType(this.dereference().getTeraType());
    }

    /**
     * Sets the Tera type of the Pokémon. If the given type does not wrap a Cobblemon {@link TeraType}, the Tera type
     * of the primary elemental type of the Pokémon is set.
     *
     * @param teraType the Tera type to set
     */
    @Override
    public void setTeraType(UTeraType<?> teraType) {
        var dereference = this.dereference();
        if (teraType.dereference() instanceof TeraType type) {
            dereference.setTeraType(type);
        } else {
            dereference.setTeraType(TeraTypes.forElementalType(dereference.getPrimaryType()));
        }
    }

    /**
     * Checks if the Pokémon is shiny.
     *
     * @return {@code true} if the Pokémon is shiny
     */
    @Override
    public boolean isShiny() {
        return this.dereference().getShiny();
    }

    /**
     * Sets if the Pokémon is shiny.
     *
     * @param shiny {@code true} to make the Pokémon shiny
     */
    @Override
    public void setShiny(boolean shiny) {
        this.dereference().setShiny(shiny);
    }

    /**
     * Gets the display name of the Pokémon. This is the nickname if the Pokémon has one.
     *
     * @return the display name
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName(false);
    }

    /**
     * Gets the nickname of the Pokémon as plain text.
     *
     * @return the nickname, or {@code null} if the Pokémon has no nickname
     */
    @Override
    public @Nullable String nickname() {
        var nickname = this.dereference().getNickname();
        return nickname == null ? null : nickname.getString();
    }

    /**
     * Sets the nickname of the Pokémon.
     *
     * @param nickname the new nickname. {@code null} or an empty string removes the nickname.
     */
    @Override
    public void setNickname(@Nullable String nickname) {
        this.dereference().setNickname(nickname == null || nickname.isEmpty() ? null : Component.literal(nickname));
    }

    /**
     * @return the level of the Pokémon
     */
    @Override
    public int level() {
        return this.dereference().getLevel();
    }

    /**
     * Sets the level of the Pokémon.
     *
     * @param level the new level
     */
    @Override
    public void setLevel(int level) {
        this.dereference().setLevel(level);
    }

    /**
     * @return the total experience of the Pokémon
     */
    @Override
    public int experience() {
        return this.dereference().getExperience();
    }

    /**
     * Sets the experience of the Pokémon and updates its level to match.
     *
     * @param experience the new total experience
     */
    @Override
    public void setExperience(int experience) {
        this.dereference().setExperienceAndUpdateLevel(experience);
    }

    /**
     * @return the experience that the Pokémon needs to reach the next level
     */
    @Override
    public int experienceToNextLevel() {
        return this.dereference().getExperienceToNextLevel();
    }

    /**
     * @return a new wrapper for the gender of the Pokémon
     */
    @Override
    public CobblemonGender gender() {
        return new CobblemonGender(this.dereference().getGender());
    }

    /**
     * Sets the gender of the Pokémon.
     *
     * @param gender the Cobblemon gender to set
     */
    @Override
    public void setGender(UGender<?> gender) {
        this.dereference().setGender((Gender) gender.dereference());
    }

    /**
     * @return a new wrapper for the nature of the Pokémon
     */
    @Override
    public CobblemonNature nature() {
        return new CobblemonNature(this.dereference().getNature());
    }

    /**
     * Sets the nature of the Pokémon.
     *
     * @param nature the Cobblemon nature to set
     */
    @Override
    public void setNature(UNature<?> nature) {
        this.dereference().setNature((Nature) nature.dereference());
    }

    /**
     * Gets the minted nature of the Pokémon.
     *
     * @return the minted nature, or {@code null} if the Pokémon has none
     */
    @Override
    public @Nullable CobblemonNature mintNature() {
        var nature = this.dereference().getMintedNature();
        return nature == null ? null : new CobblemonNature(nature);
    }

    /**
     * Sets the minted nature of the Pokémon.
     *
     * @param nature the Cobblemon nature to set, or {@code null} to remove the minted nature
     */
    @Override
    public void setMintNature(@Nullable UNature<?> nature) {
        this.dereference().setMintedNature(nature == null ? null : (Nature) nature.dereference());
    }

    /**
     * @return a new wrapper for the template of the ability of the Pokémon
     */
    @Override
    public CobblemonAbility ability() {
        return new CobblemonAbility(this.dereference().getAbility().getTemplate());
    }

    /**
     * Gives the Pokémon a forced ability made from the template, with the lowest priority.
     *
     * @param ability the Cobblemon ability to set
     */
    @Override
    public void setAbility(UAbility<?> ability) {
        this.dereference().updateAbility(((AbilityTemplate) ability.dereference()).create(true, Priority.LOWEST));
    }

    /**
     * @return a new wrapper for the Poké Ball that the Pokémon was caught in
     */
    @Override
    public CobblemonPokeBall pokeBall() {
        return new CobblemonPokeBall(this.dereference().getCaughtBall());
    }

    /**
     * Sets the Poké Ball that the Pokémon was caught in.
     *
     * @param ball the Cobblemon Poké Ball to set
     */
    @Override
    public void setPokeBall(UPokeBall<?> ball) {
        this.dereference().setCaughtBall((PokeBall) ball.dereference());
    }

    /**
     * @return a new wrapper for the moveset of the Pokémon
     */
    @Override
    public CobblemonMoveset moveset() {
        return new CobblemonMoveset(this.dereference().getMoveSet());
    }

    /**
     * @return the item that the Pokémon holds
     */
    @Override
    public ItemStack heldItem() {
        return this.dereference().heldItem();
    }

    /**
     * Swaps the held item of the Pokémon for the given item.
     *
     * @param item the item for the Pokémon to hold
     */
    @Override
    public void setHeldItem(ItemStack item) {
        this.dereference().swapHeldItem(item, false, true);
    }

    /**
     * @return the friendship of the Pokémon
     */
    @Override
    public int friendship() {
        return this.dereference().getFriendship();
    }

    /**
     * Sets the friendship of the Pokémon.
     *
     * @param friendship the new friendship
     */
    @Override
    public void setFriendship(int friendship) {
        this.dereference().setFriendship(friendship, true);
    }

    /**
     * @return the current health of the Pokémon
     */
    @Override
    public int health() {
        return this.dereference().getCurrentHealth();
    }

    /**
     * Sets the current health of the Pokémon.
     *
     * @param health the new health
     */
    @Override
    public void setHealth(int health) {
        this.dereference().setCurrentHealth(health);
    }

    /**
     * @return the maximum health of the Pokémon
     */
    @Override
    public int maxHealth() {
        return this.dereference().getMaxHealth();
    }

    /**
     * Heals the Pokémon with the Cobblemon heal method.
     */
    @Override
    public void heal() {
        this.dereference().heal();
    }

    /**
     * Checks if the Pokémon is fainted.
     *
     * @return {@code true} if the Pokémon is fainted
     */
    @Override
    public boolean isFainted() {
        return this.dereference().isFainted();
    }

    /**
     * Gets the persistent status of the Pokémon.
     *
     * @return the status, or {@code null} if the Pokémon has no status
     */
    @Override
    public @Nullable CobblemonStatus status() {
        var container = this.dereference().getStatus();
        return container == null ? null : new CobblemonStatus(container.getStatus());
    }

    /**
     * Applies a persistent status to the Pokémon.
     *
     * @param status the Cobblemon status to apply, or {@code null} to remove the status
     */
    @Override
    public void setStatus(@Nullable UStatus<?> status) {
        if (status == null) {
            this.dereference().setStatus(null);
        } else {
            this.dereference().applyStatus((PersistentStatus) status.dereference());
        }
    }

    /**
     * Gets the calculated value of a stat of the Pokémon.
     *
     * @param stat the stat to get
     * @return the stat value, or {@code 0} if the stat is not permanent
     */
    @Override
    public int stat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((Stat) stat.dereference()) : 0;
    }

    /**
     * @return a new wrapper for the IVs of the Pokémon
     */
    @Override
    public CobblemonIVs ivs() {
        return new CobblemonIVs(this.dereference().getIvs());
    }

    /**
     * @return a new wrapper for the EVs of the Pokémon
     */
    @Override
    public CobblemonEVs evs() {
        return new CobblemonEVs(this.dereference().getEvs());
    }

    /**
     * Checks if the Pokémon is an egg. Cobblemon has no eggs, so this is always {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEgg() {
        return false;
    }

    /**
     * Gets the name of the original trainer of the Pokémon.
     *
     * @return the name, or {@code null} if the Pokémon has no original trainer name
     */
    @Override
    public @Nullable String originalTrainer() {
        return this.dereference().getOriginalTrainerName();
    }

    /**
     * Parses the original trainer of the Pokémon as a UUID.
     *
     * @return the UUID, or {@code null} if there is no original trainer or it is not a valid UUID
     */
    @Override
    public @Nullable UUID originalTrainerUUID() {
        var trainer = this.dereference().getOriginalTrainer();
        if (trainer == null) {
            return null;
        }
        try {
            return UUID.fromString(trainer);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sets the player as the original trainer of the Pokémon.
     *
     * @param player the new original trainer
     */
    @Override
    public void setOriginalTrainer(ServerPlayer player) {
        this.dereference().setOriginalTrainer(player.getUUID());
    }

    /**
     * Gets the UUID of the player that owns the Pokémon.
     *
     * @return the owner UUID, or {@code null} if no player owns the Pokémon
     */
    @Override
    public @Nullable UUID ownerUUID() {
        return this.dereference().isPlayerOwned() ? this.dereference().getOwnerUUID() : null;
    }

    /**
     * Gets the player that owns the Pokémon.
     *
     * @return the owner, or {@code null} if there is no owner player
     */
    @Override
    public @Nullable ServerPlayer ownerPlayer() {
        return this.dereference().getOwnerPlayer();
    }

    /**
     * Checks if the Pokémon can be traded.
     *
     * @return {@code true} if the Pokémon is tradeable
     */
    @Override
    public boolean isTradeable() {
        return this.dereference().getTradeable();
    }

    /**
     * Sets if the Pokémon can be traded.
     *
     * @param tradeable {@code true} to make the Pokémon tradeable
     */
    @Override
    public void setTradeable(boolean tradeable) {
        this.dereference().setTradeable(tradeable);
    }

    /**
     * @return the Dynamax level of the Pokémon
     */
    @Override
    public int dynamaxLevel() {
        return this.dereference().getDmaxLevel();
    }

    /**
     * Sets the Dynamax level of the Pokémon.
     *
     * @param level the new Dynamax level
     */
    @Override
    public void setDynamaxLevel(int level) {
        this.dereference().setDmaxLevel(level);
    }

    /**
     * Checks if the Pokémon has the Gigantamax factor.
     *
     * @return {@code true} if the Pokémon has the Gigantamax factor
     */
    @Override
    public boolean hasGigantamaxFactor() {
        return this.dereference().getGmaxFactor();
    }

    /**
     * Sets if the Pokémon has the Gigantamax factor.
     *
     * @param factor {@code true} to give the Pokémon the Gigantamax factor
     */
    @Override
    public void setGigantamaxFactor(boolean factor) {
        this.dereference().setGmaxFactor(factor);
    }

    /**
     * Checks if the Pokémon is legendary.
     *
     * @return {@code true} if the Pokémon is legendary
     */
    @Override
    public boolean isLegendary() {
        return this.dereference().isLegendary();
    }

    /**
     * Checks if the Pokémon is mythical.
     *
     * @return {@code true} if the Pokémon is mythical
     */
    @Override
    public boolean isMythical() {
        return this.dereference().isMythical();
    }

    /**
     * Checks if the Pokémon is an Ultra Beast.
     *
     * @return {@code true} if the Pokémon is an Ultra Beast
     */
    @Override
    public boolean isUltraBeast() {
        return this.dereference().isUltraBeast();
    }

    /**
     * @return the Cobblemon persistent data tag of the Pokémon
     */
    @Override
    public CompoundTag persistentData() {
        return this.dereference().getPersistentData();
    }

    /**
     * Gets the entity of the Pokémon.
     *
     * @return the entity, or {@code null} if the Pokémon has no entity in the world
     */
    @Override
    public @Nullable CobblemonPokemonEntity entity() {
        var entity = this.dereference().getEntity();
        return entity == null ? null : new CobblemonPokemonEntity(entity);
    }

    /**
     * Spawns a new entity for the Pokémon at the position. If the Pokémon already has an entity, returns that entity
     * and spawns nothing.
     *
     * @param level the level to spawn the entity in
     * @param position the position to spawn the entity at
     * @return the entity of the Pokémon
     */
    @Override
    public CobblemonPokemonEntity spawn(ServerLevel level, Vec3 position) {
        var existing = this.entity();
        if (existing != null) {
            return existing;
        }
        var entity = new PokemonEntity(level, this.dereference(), CobblemonEntities.POKEMON);
        entity.moveTo(position.x, position.y, position.z, entity.getYRot(), entity.getXRot());
        level.addFreshEntity(entity);
        return new CobblemonPokemonEntity(entity);
    }

    /**
     * Creates a copy of the Pokémon with the Cobblemon clone method.
     *
     * @return the copy
     */
    @Override
    public CobblemonPokemon copy() {
        return new CobblemonPokemon(this.dereference().clone(true, ServerUtils.registryAccess()));
    }

    /**
     * @return a Cobblemon Pokémon item that shows the Pokémon
     */
    @Override
    public ItemStack toItem() {
        return PokemonItem.from(this.dereference());
    }

    /**
     * Saves the Pokémon to a new tag.
     *
     * @return the tag that holds the Pokémon
     */
    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().saveToNBT(ServerUtils.registryAccess(), new CompoundTag());
    }

    /**
     * Saves the Pokémon to a new JSON object in the Cobblemon format.
     *
     * @return the JSON that holds the Pokémon
     */
    @Override
    public JsonElement toJson() {
        return this.dereference().saveToJSON(ServerUtils.registryAccess(), new JsonObject());
    }

    /**
     * @return the Cobblemon aspects of the Pokémon
     */
    public Set<String> aspects() {
        return this.dereference().getAspects();
    }
}
