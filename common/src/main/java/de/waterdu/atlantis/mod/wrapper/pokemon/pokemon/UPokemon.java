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

public abstract class UPokemon<T> extends ModWrapper<T> {

    protected UPokemon(ModObjectKey type, T pokemon) {
        super(new ModObject(type, pokemon));
    }

    public abstract UUID uuid();

    public abstract USpecies<?> species();

    public abstract void setSpecies(USpecies<?> species);

    public abstract UForm<?> form();

    public abstract void setForm(UForm<?> form);

    public List<? extends UElementalType<?>> types() {
        return this.form().types();
    }

    public abstract boolean isShiny();

    public abstract void setShiny(boolean shiny);

    public abstract Component displayName();

    public abstract @Nullable String nickname();

    public abstract void setNickname(@Nullable String nickname);

    public abstract int level();

    public abstract void setLevel(int level);

    public abstract int experience();

    public abstract void setExperience(int experience);

    public abstract int experienceToNextLevel();

    public abstract UGender<?> gender();

    public abstract void setGender(UGender<?> gender);

    public abstract UNature<?> nature();

    public abstract void setNature(UNature<?> nature);

    public abstract @Nullable UNature<?> mintNature();

    public abstract void setMintNature(@Nullable UNature<?> nature);

    public UNature<?> effectiveNature() {
        var mint = this.mintNature();
        return mint == null ? this.nature() : mint;
    }

    public abstract UAbility<?> ability();

    public abstract void setAbility(UAbility<?> ability);

    public abstract UPokeBall<?> pokeBall();

    public abstract void setPokeBall(UPokeBall<?> ball);

    public abstract UMoveset<?, ?> moveset();

    public abstract ItemStack heldItem();

    public abstract void setHeldItem(ItemStack item);

    public abstract int friendship();

    public abstract void setFriendship(int friendship);

    public abstract int health();

    public abstract void setHealth(int health);

    public abstract int maxHealth();

    public abstract void heal();

    public boolean isFainted() {
        return this.health() <= 0;
    }

    public abstract @Nullable UStatus<?> status();

    public abstract void setStatus(@Nullable UStatus<?> status);

    public abstract int stat(UStat<?> stat);

    public abstract UIVs<?, ?> ivs();

    public abstract UEVs<?, ?> evs();

    public abstract boolean isEgg();

    public abstract @Nullable String originalTrainer();

    public abstract @Nullable UUID originalTrainerUUID();

    public abstract void setOriginalTrainer(ServerPlayer player);

    public abstract @Nullable UUID ownerUUID();

    public abstract @Nullable ServerPlayer ownerPlayer();

    public abstract boolean isTradeable();

    public abstract void setTradeable(boolean tradeable);

    public abstract int dynamaxLevel();

    public abstract void setDynamaxLevel(int level);

    public abstract boolean hasGigantamaxFactor();

    public abstract void setGigantamaxFactor(boolean factor);

    public abstract boolean isLegendary();

    public abstract boolean isMythical();

    public abstract boolean isUltraBeast();

    public abstract CompoundTag persistentData();

    public abstract @Nullable UPokemonEntity<?> entity();

    public abstract UPokemonEntity<?> spawn(ServerLevel level, Vec3 position);

    public abstract UPokemon<?> copy();

    public abstract ItemStack toItem();

    public abstract CompoundTag toCompoundTag();

    public abstract JsonElement toJson();
}
