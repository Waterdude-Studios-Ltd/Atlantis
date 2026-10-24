package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.types.tera.TeraType;
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
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonForm;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonGender;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonNature;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonPokeBall;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonSpecies;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonStatus;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves.CobblemonMoveset;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonEVs;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonIVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;
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

public class CobblemonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokemon");

    public CobblemonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    public static Pokemon unwrap(UPokemon<?> pokemon) {
        if (pokemon.dereference() instanceof Pokemon it) {
            return it;
        }
        throw new IllegalArgumentException("Not a Cobblemon Pokémon: " + pokemon);
    }

    public static @Nullable CobblemonPokemon of(@Nullable Pokemon pokemon) {
        return pokemon == null ? null : new CobblemonPokemon(pokemon);
    }

    @Override
    public UUID uuid() {
        return this.dereference().getUuid();
    }

    @Override
    public CobblemonSpecies species() {
        return new CobblemonSpecies(this.dereference().getSpecies());
    }

    @Override
    public void setSpecies(USpecies<?> species) {
        this.dereference().setSpecies((Species) species.dereference());
    }

    @Override
    public CobblemonForm form() {
        return new CobblemonForm(this.dereference().getForm());
    }

    @Override
    public void setForm(UForm<?> form) {
        this.dereference().setForm((FormData) form.dereference());
    }

    @Override
    public boolean isShiny() {
        return this.dereference().getShiny();
    }

    @Override
    public void setShiny(boolean shiny) {
        this.dereference().setShiny(shiny);
    }

    @Override
    public Component displayName() {
        return this.dereference().getDisplayName(false);
    }

    @Override
    public @Nullable String nickname() {
        var nickname = this.dereference().getNickname();
        return nickname == null ? null : nickname.getString();
    }

    @Override
    public void setNickname(@Nullable String nickname) {
        this.dereference().setNickname(nickname == null || nickname.isEmpty() ? null : Component.literal(nickname));
    }

    @Override
    public int level() {
        return this.dereference().getLevel();
    }

    @Override
    public void setLevel(int level) {
        this.dereference().setLevel(level);
    }

    @Override
    public int experience() {
        return this.dereference().getExperience();
    }

    @Override
    public void setExperience(int experience) {
        this.dereference().setExperienceAndUpdateLevel(experience);
    }

    @Override
    public int experienceToNextLevel() {
        return this.dereference().getExperienceToNextLevel();
    }

    @Override
    public CobblemonGender gender() {
        return new CobblemonGender(this.dereference().getGender());
    }

    @Override
    public void setGender(UGender<?> gender) {
        this.dereference().setGender((Gender) gender.dereference());
    }

    @Override
    public CobblemonNature nature() {
        return new CobblemonNature(this.dereference().getNature());
    }

    @Override
    public void setNature(UNature<?> nature) {
        this.dereference().setNature((Nature) nature.dereference());
    }

    @Override
    public @Nullable CobblemonNature mintNature() {
        var nature = this.dereference().getMintedNature();
        return nature == null ? null : new CobblemonNature(nature);
    }

    @Override
    public void setMintNature(@Nullable UNature<?> nature) {
        this.dereference().setMintedNature(nature == null ? null : (Nature) nature.dereference());
    }

    @Override
    public CobblemonAbility ability() {
        return new CobblemonAbility(this.dereference().getAbility().getTemplate());
    }

    @Override
    public void setAbility(UAbility<?> ability) {
        this.dereference().updateAbility(((AbilityTemplate) ability.dereference()).create(true, Priority.LOWEST));
    }

    @Override
    public CobblemonPokeBall pokeBall() {
        return new CobblemonPokeBall(this.dereference().getCaughtBall());
    }

    @Override
    public void setPokeBall(UPokeBall<?> ball) {
        this.dereference().setCaughtBall((PokeBall) ball.dereference());
    }

    @Override
    public CobblemonMoveset moveset() {
        return new CobblemonMoveset(this.dereference().getMoveSet());
    }

    @Override
    public ItemStack heldItem() {
        return this.dereference().heldItem();
    }

    @Override
    public void setHeldItem(ItemStack item) {
        this.dereference().swapHeldItem(item, false, true);
    }

    @Override
    public int friendship() {
        return this.dereference().getFriendship();
    }

    @Override
    public void setFriendship(int friendship) {
        this.dereference().setFriendship(friendship, true);
    }

    @Override
    public int health() {
        return this.dereference().getCurrentHealth();
    }

    @Override
    public void setHealth(int health) {
        this.dereference().setCurrentHealth(health);
    }

    @Override
    public int maxHealth() {
        return this.dereference().getMaxHealth();
    }

    @Override
    public void heal() {
        this.dereference().heal();
    }

    @Override
    public boolean isFainted() {
        return this.dereference().isFainted();
    }

    @Override
    public @Nullable CobblemonStatus status() {
        var container = this.dereference().getStatus();
        return container == null ? null : new CobblemonStatus(container.getStatus());
    }

    @Override
    public void setStatus(@Nullable UStatus<?> status) {
        if (status == null) {
            this.dereference().setStatus(null);
        } else {
            this.dereference().applyStatus((PersistentStatus) status.dereference());
        }
    }

    @Override
    public int stat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((Stat) stat.dereference()) : 0;
    }

    @Override
    public CobblemonIVs ivs() {
        return new CobblemonIVs(this.dereference().getIvs());
    }

    @Override
    public CobblemonEVs evs() {
        return new CobblemonEVs(this.dereference().getEvs());
    }

    @Override
    public boolean isEgg() {
        return false;
    }

    @Override
    public @Nullable String originalTrainer() {
        return this.dereference().getOriginalTrainerName();
    }

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

    @Override
    public void setOriginalTrainer(ServerPlayer player) {
        this.dereference().setOriginalTrainer(player.getUUID());
    }

    @Override
    public @Nullable UUID ownerUUID() {
        return this.dereference().isPlayerOwned() ? this.dereference().getOwnerUUID() : null;
    }

    @Override
    public @Nullable ServerPlayer ownerPlayer() {
        return this.dereference().getOwnerPlayer();
    }

    @Override
    public boolean isTradeable() {
        return this.dereference().getTradeable();
    }

    @Override
    public void setTradeable(boolean tradeable) {
        this.dereference().setTradeable(tradeable);
    }

    @Override
    public int dynamaxLevel() {
        return this.dereference().getDmaxLevel();
    }

    @Override
    public void setDynamaxLevel(int level) {
        this.dereference().setDmaxLevel(level);
    }

    @Override
    public boolean hasGigantamaxFactor() {
        return this.dereference().getGmaxFactor();
    }

    @Override
    public void setGigantamaxFactor(boolean factor) {
        this.dereference().setGmaxFactor(factor);
    }

    @Override
    public boolean isLegendary() {
        return this.dereference().isLegendary();
    }

    @Override
    public boolean isMythical() {
        return this.dereference().isMythical();
    }

    @Override
    public boolean isUltraBeast() {
        return this.dereference().isUltraBeast();
    }

    @Override
    public CompoundTag persistentData() {
        return this.dereference().getPersistentData();
    }

    @Override
    public @Nullable CobblemonPokemonEntity entity() {
        var entity = this.dereference().getEntity();
        return entity == null ? null : new CobblemonPokemonEntity(entity);
    }

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

    @Override
    public CobblemonPokemon copy() {
        return new CobblemonPokemon(this.dereference().clone(true, ServerUtils.registryAccess()));
    }

    @Override
    public ItemStack toItem() {
        return PokemonItem.from(this.dereference());
    }

    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().saveToNBT(ServerUtils.registryAccess(), new CompoundTag());
    }

    @Override
    public JsonElement toJson() {
        return this.dereference().saveToJSON(ServerUtils.registryAccess(), new JsonObject());
    }

    public Set<String> aspects() {
        return this.dereference().getAspects();
    }

    public TeraType teraType() {
        return this.dereference().getTeraType();
    }
}
