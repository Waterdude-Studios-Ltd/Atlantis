package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.pixelmonmod.api.Flags;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.battles.status.NoStatus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity.PixelmonPokemonEntity;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonAbility;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonElementalType;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonForm;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonGender;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonNature;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonPokeBall;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonStatus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonTeraType;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves.PixelmonMoveset;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonEVs;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonIVs;
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
import net.minecraft.core.Holder;
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
 * Wraps a Pixelmon Pokémon.
 */
public class PixelmonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokemon");

    /**
     * Creates a wrapper for a Pixelmon Pokémon.
     *
     * @param pokemon the Pokémon to wrap
     */
    public PixelmonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    /**
     * Throws an exception when the wrapped object is not a Pixelmon {@link Pokemon}.
     */
    public static Pokemon unwrap(UPokemon<?> pokemon) {
        if (pokemon.dereference() instanceof Pokemon it) {
            return it;
        }
        throw new IllegalArgumentException("Not a Pixelmon Pokémon: " + pokemon);
    }

    /**
     * Creates a wrapper for a Pixelmon Pokémon that may be {@code null}.
     *
     * @param pokemon the Pokémon to wrap
     * @return the wrapper, or {@code null} if the Pokémon is {@code null}
     */
    public static @Nullable PixelmonPokemon of(@Nullable Pokemon pokemon) {
        return pokemon == null ? null : new PixelmonPokemon(pokemon);
    }

    /**
     * @return the UUID of the Pokémon
     */
    @Override
    public UUID uuid() {
        return this.dereference().getUUID();
    }

    /**
     * @return a new wrapper for the species of the Pokémon
     */
    @Override
    public PixelmonSpecies species() {
        return new PixelmonSpecies(this.dereference().getSpecies());
    }

    /**
     * Sets the species of the Pokémon.
     *
     * @param species the Pixelmon species to set
     */
    @Override
    public void setSpecies(USpecies<?> species) {
        this.dereference().setSpecies((Species) species.dereference(), true);
    }

    /**
     * @return a new wrapper for the form of the Pokémon
     */
    @Override
    public PixelmonForm form() {
        return new PixelmonForm(this.dereference().getForm());
    }

    /**
     * Sets the form of the Pokémon.
     *
     * @param form the Pixelmon form to set
     */
    @Override
    public void setForm(UForm<?> form) {
        this.dereference().setForm((Stats) form.dereference());
    }

    /**
     * @return a new wrapper for the Tera type of the Pokémon
     */
    @Override
    public PixelmonTeraType teraType() {
        return new PixelmonTeraType(this.dereference().getTeraType());
    }

    /**
     * Sets the Tera type of the Pokémon. If the given type is not a {@link PixelmonTeraType}, the Tera type is set to
     * {@link Type#MYSTERY}.
     *
     * @param teraType the Tera type to set
     */
    @Override
    public void setTeraType(UTeraType<?> teraType) {
        var dereference = this.dereference();
        if (!(teraType instanceof PixelmonTeraType type)) {
            dereference.setTeraType(Type.MYSTERY.holder());
        } else {
            dereference.setTeraType(type.dereference());
        }
    }

    /**
     * Checks if the Pokémon is shiny.
     *
     * @return {@code true} if the Pokémon is shiny
     */
    @Override
    public boolean isShiny() {
        return this.dereference().isShiny();
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
     * Gets the display name of the Pokémon from Pixelmon.
     *
     * @return the display name
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }

    /**
     * Gets the nickname of the Pokémon as plain text.
     *
     * @return the nickname, or {@code null} if the Pokémon has no nickname
     */
    @Override
    public @Nullable String nickname() {
        return this.dereference().hasNickname() ? this.dereference().getNickname().getString() : null;
    }

    /**
     * Sets the nickname of the Pokémon.
     *
     * @param nickname the new nickname. {@code null} or an empty string removes the nickname.
     */
    @Override
    public void setNickname(@Nullable String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            this.dereference().removeNickname();
        } else {
            this.dereference().setNickname(nickname);
        }
    }

    /**
     * @return the level of the Pokémon
     */
    @Override
    public int level() {
        return this.dereference().getPokemonLevel();
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
     * @return the experience of the Pokémon
     */
    @Override
    public int experience() {
        return this.dereference().getExperience();
    }

    /**
     * Sets the experience of the Pokémon.
     *
     * @param experience the new experience
     */
    @Override
    public void setExperience(int experience) {
        this.dereference().setExperience(experience);
    }

    /**
     * @return the experience that the Pokémon needs to reach the next level
     */
    @Override
    public int experienceToNextLevel() {
        return this.dereference().getExperienceToLevelUp();
    }

    /**
     * @return a new wrapper for the gender of the Pokémon
     */
    @Override
    public PixelmonGender gender() {
        return new PixelmonGender(this.dereference().getGender());
    }

    /**
     * Sets the gender of the Pokémon.
     *
     * @param gender the Pixelmon gender to set
     */
    @Override
    public void setGender(UGender<?> gender) {
        this.dereference().setGender((Gender) gender.dereference());
    }

    /**
     * @return a new wrapper for the base nature of the Pokémon, without the minted nature
     */
    @Override
    public PixelmonNature nature() {
        return new PixelmonNature(this.dereference().getBaseNature());
    }

    /**
     * Sets the nature of the Pokémon.
     *
     * @param nature the Pixelmon nature to set
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
    public @Nullable PixelmonNature mintNature() {
        var nature = this.dereference().getMintNature();
        return nature == null ? null : new PixelmonNature(nature);
    }

    /**
     * Sets the minted nature of the Pokémon.
     *
     * @param nature the Pixelmon nature to set, or {@code null} to remove the minted nature
     */
    @Override
    public void setMintNature(@Nullable UNature<?> nature) {
        this.dereference().setMintNature(nature == null ? null : (Nature) nature.dereference());
    }

    /**
     * @return a new wrapper for the ability of the Pokémon
     */
    @Override
    public PixelmonAbility ability() {
        return new PixelmonAbility(this.dereference().getAbility());
    }

    /**
     * Gives the Pokémon a new instance of the ability.
     *
     * @param ability the Pixelmon ability to set
     */
    @Override
    public void setAbility(UAbility<?> ability) {
        this.dereference().setAbility(((Ability) ability.dereference()).getNewInstance());
    }

    /**
     * @return a new wrapper for the Poké Ball that the Pokémon was caught in
     */
    @Override
    public PixelmonPokeBall pokeBall() {
        return new PixelmonPokeBall(this.dereference().getBall());
    }

    /**
     * Sets the Poké Ball that the Pokémon was caught in.
     *
     * @param ball the Pixelmon Poké Ball to set
     */
    @Override
    public void setPokeBall(UPokeBall<?> ball) {
        this.dereference().setBall((PokeBall) ball.dereference());
    }

    /**
     * @return a new wrapper for the moveset of the Pokémon
     */
    @Override
    public PixelmonMoveset moveset() {
        return new PixelmonMoveset(this.dereference().getMoveset());
    }

    /**
     * @return the item that the Pokémon holds
     */
    @Override
    public ItemStack heldItem() {
        return this.dereference().getHeldItem();
    }

    /**
     * Sets the item that the Pokémon holds.
     *
     * @param item the item for the Pokémon to hold
     */
    @Override
    public void setHeldItem(ItemStack item) {
        this.dereference().setHeldItem(item);
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
        this.dereference().setFriendship(friendship);
    }

    /**
     * @return the current health of the Pokémon
     */
    @Override
    public int health() {
        return this.dereference().getHealth();
    }

    /**
     * Sets the current health of the Pokémon.
     *
     * @param health the new health
     */
    @Override
    public void setHealth(int health) {
        this.dereference().setHealth(health);
    }

    /**
     * @return the maximum health of the Pokémon
     */
    @Override
    public int maxHealth() {
        return this.dereference().getMaxHealth();
    }

    /**
     * Heals the Pokémon with the Pixelmon heal method.
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
    public @Nullable PixelmonStatus status() {
        return PixelmonStatus.of(this.dereference().getStatus());
    }

    /**
     * Applies a new instance of a persistent status to the Pokémon.
     *
     * @param status the Pixelmon status to apply. {@code null} or a status that is not a {@link PixelmonStatus}
     *     removes the status.
     */
    @Override
    public void setStatus(@Nullable UStatus<?> status) {
        this.dereference().setStatus(status instanceof PixelmonStatus pixelmon ? pixelmon.create() : NoStatus.noStatus);
    }

    /**
     * Gets the calculated value of a stat of the Pokémon.
     *
     * @param stat the stat to get
     * @return the stat value, or {@code 0} if the stat is not permanent
     */
    @Override
    public int stat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    /**
     * @return a new wrapper for the IVs of the Pokémon
     */
    @Override
    public PixelmonIVs ivs() {
        return new PixelmonIVs(this.dereference().getIVs());
    }

    /**
     * @return a new wrapper for the EVs of the Pokémon
     */
    @Override
    public PixelmonEVs evs() {
        return new PixelmonEVs(this.dereference().getEVs());
    }

    /**
     * Checks if the Pokémon is an egg.
     *
     * @return {@code true} if the Pokémon is an egg
     */
    @Override
    public boolean isEgg() {
        return this.dereference().isEgg();
    }

    /**
     * Gets the name of the original trainer of the Pokémon.
     *
     * @return the name, or {@code null} if the Pokémon has no original trainer
     */
    @Override
    public @Nullable String originalTrainer() {
        return this.dereference().getOriginalTrainer();
    }

    /**
     * Gets the UUID of the original trainer of the Pokémon.
     *
     * @return the UUID, or {@code null} if the Pokémon has no original trainer
     */
    @Override
    public @Nullable UUID originalTrainerUUID() {
        return this.dereference().getOriginalTrainerUUID();
    }

    /**
     * Sets the player as the original trainer of the Pokémon.
     *
     * @param player the new original trainer
     */
    @Override
    public void setOriginalTrainer(ServerPlayer player) {
        this.dereference().setOriginalTrainer(player);
    }

    /**
     * Gets the UUID of the player that owns the Pokémon.
     *
     * @return the owner UUID, or {@code null} if no player owns the Pokémon
     */
    @Override
    public @Nullable UUID ownerUUID() {
        return this.dereference().getOwnerPlayerUUID();
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
        return !this.dereference().isUntradeable();
    }

    /**
     * Sets if the Pokémon can be traded. This removes or adds the {@link Flags#UNTRADEABLE} flag.
     *
     * @param tradeable {@code true} to make the Pokémon tradeable
     */
    @Override
    public void setTradeable(boolean tradeable) {
        if (tradeable) {
            this.dereference().removeFlag(Flags.UNTRADEABLE);
        } else {
            this.dereference().addFlag(Flags.UNTRADEABLE);
        }
    }

    /**
     * @return the Dynamax level of the Pokémon
     */
    @Override
    public int dynamaxLevel() {
        return this.dereference().getDynamaxLevel();
    }

    /**
     * Sets the Dynamax level of the Pokémon.
     *
     * @param level the new Dynamax level
     */
    @Override
    public void setDynamaxLevel(int level) {
        this.dereference().setDynamaxLevel(level);
    }

    /**
     * Checks if the Pokémon has the Gigantamax factor.
     *
     * @return {@code true} if the Pokémon has the Gigantamax factor
     */
    @Override
    public boolean hasGigantamaxFactor() {
        return this.dereference().hasGigantamaxFactor();
    }

    /**
     * Sets if the Pokémon has the Gigantamax factor.
     *
     * @param factor {@code true} to give the Pokémon the Gigantamax factor
     */
    @Override
    public void setGigantamaxFactor(boolean factor) {
        this.dereference().setGigantamaxFactor(factor);
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
     * @return the Pixelmon persistent data tag of the Pokémon
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
    public @Nullable PixelmonPokemonEntity entity() {
        return this.dereference().getPixelmonEntity().map(PixelmonPokemonEntity::new).orElse(null);
    }

    /**
     * Gets the entity of the Pokémon, or spawns a new one at the position if there is none.
     *
     * @param level the level to spawn the entity in
     * @param position the position to spawn the entity at
     * @return the entity of the Pokémon
     */
    @Override
    public PixelmonPokemonEntity spawn(ServerLevel level, Vec3 position) {
        return new PixelmonPokemonEntity(this.dereference().getOrSpawnPixelmon(level, position.x, position.y, position.z));
    }

    /**
     * Creates a copy of the Pokémon with a new random UUID.
     *
     * @return the copy
     */
    @Override
    public PixelmonPokemon copy() {
        var copy = PokemonFactory.copy(this.dereference());
        copy.setUUID(UUID.randomUUID());
        return new PixelmonPokemon(copy);
    }

    /**
     * Creates a Pixelmon photo item of the Pokémon.
     *
     * @return the photo item
     */
    @Override
    public ItemStack toItem() {
        return SpriteItemHelper.getPhoto(this.dereference());
    }

    /**
     * Writes the Pokémon to a new tag with the registry access of the server.
     *
     * @return the tag
     */
    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().writeToNBT(new CompoundTag(), ServerUtils.registryAccess());
    }

    /**
     * Writes the Pokémon as a JSON string that holds the text of {@link #toCompoundTag()}.
     *
     * @return the JSON string
     */
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.toCompoundTag().toString());
    }

    /**
     * @return the name of the Pixelmon palette of the Pokémon
     */
    public String palette() {
        return this.dereference().getPaletteName();
    }

    /**
     * @return the Pixelmon flags of the Pokémon
     */
    public List<String> flags() {
        return this.dereference().getFlags();
    }

    /**
     * Checks if the Pokémon has a Pixelmon flag.
     *
     * @param flag the flag to check
     * @return {@code true} if the Pokémon has the flag
     */
    public boolean hasFlag(String flag) {
        return this.dereference().hasFlag(flag);
    }
}
