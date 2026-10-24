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
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.battles.status.NoStatus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity.PixelmonPokemonEntity;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonAbility;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonForm;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonGender;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonNature;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonPokeBall;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonStatus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves.PixelmonMoveset;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonEVs;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonIVs;
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

import java.util.List;
import java.util.UUID;

public class PixelmonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokemon");

    public PixelmonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    public static Pokemon unwrap(UPokemon<?> pokemon) {
        if (pokemon.dereference() instanceof Pokemon it) {
            return it;
        }
        throw new IllegalArgumentException("Not a Pixelmon Pokémon: " + pokemon);
    }

    public static @Nullable PixelmonPokemon of(@Nullable Pokemon pokemon) {
        return pokemon == null ? null : new PixelmonPokemon(pokemon);
    }

    @Override
    public UUID uuid() {
        return this.dereference().getUUID();
    }

    @Override
    public PixelmonSpecies species() {
        return new PixelmonSpecies(this.dereference().getSpecies());
    }

    @Override
    public void setSpecies(USpecies<?> species) {
        this.dereference().setSpecies((Species) species.dereference(), true);
    }

    @Override
    public PixelmonForm form() {
        return new PixelmonForm(this.dereference().getForm());
    }

    @Override
    public void setForm(UForm<?> form) {
        this.dereference().setForm((Stats) form.dereference());
    }

    @Override
    public boolean isShiny() {
        return this.dereference().isShiny();
    }

    @Override
    public void setShiny(boolean shiny) {
        this.dereference().setShiny(shiny);
    }

    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }

    @Override
    public @Nullable String nickname() {
        return this.dereference().hasNickname() ? this.dereference().getNickname().getString() : null;
    }

    @Override
    public void setNickname(@Nullable String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            this.dereference().removeNickname();
        } else {
            this.dereference().setNickname(nickname);
        }
    }

    @Override
    public int level() {
        return this.dereference().getPokemonLevel();
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
        this.dereference().setExperience(experience);
    }

    @Override
    public int experienceToNextLevel() {
        return this.dereference().getExperienceToLevelUp();
    }

    @Override
    public PixelmonGender gender() {
        return new PixelmonGender(this.dereference().getGender());
    }

    @Override
    public void setGender(UGender<?> gender) {
        this.dereference().setGender((Gender) gender.dereference());
    }

    @Override
    public PixelmonNature nature() {
        return new PixelmonNature(this.dereference().getBaseNature());
    }

    @Override
    public void setNature(UNature<?> nature) {
        this.dereference().setNature((Nature) nature.dereference());
    }

    @Override
    public @Nullable PixelmonNature mintNature() {
        var nature = this.dereference().getMintNature();
        return nature == null ? null : new PixelmonNature(nature);
    }

    @Override
    public void setMintNature(@Nullable UNature<?> nature) {
        this.dereference().setMintNature(nature == null ? null : (Nature) nature.dereference());
    }

    @Override
    public PixelmonAbility ability() {
        return new PixelmonAbility(this.dereference().getAbility());
    }

    @Override
    public void setAbility(UAbility<?> ability) {
        this.dereference().setAbility(((Ability) ability.dereference()).getNewInstance());
    }

    @Override
    public PixelmonPokeBall pokeBall() {
        return new PixelmonPokeBall(this.dereference().getBall());
    }

    @Override
    public void setPokeBall(UPokeBall<?> ball) {
        this.dereference().setBall((PokeBall) ball.dereference());
    }

    @Override
    public PixelmonMoveset moveset() {
        return new PixelmonMoveset(this.dereference().getMoveset());
    }

    @Override
    public ItemStack heldItem() {
        return this.dereference().getHeldItem();
    }

    @Override
    public void setHeldItem(ItemStack item) {
        this.dereference().setHeldItem(item);
    }

    @Override
    public int friendship() {
        return this.dereference().getFriendship();
    }

    @Override
    public void setFriendship(int friendship) {
        this.dereference().setFriendship(friendship);
    }

    @Override
    public int health() {
        return this.dereference().getHealth();
    }

    @Override
    public void setHealth(int health) {
        this.dereference().setHealth(health);
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
    public @Nullable PixelmonStatus status() {
        return PixelmonStatus.of(this.dereference().getStatus());
    }

    @Override
    public void setStatus(@Nullable UStatus<?> status) {
        this.dereference().setStatus(status instanceof PixelmonStatus pixelmon ? pixelmon.create() : NoStatus.noStatus);
    }

    @Override
    public int stat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    @Override
    public PixelmonIVs ivs() {
        return new PixelmonIVs(this.dereference().getIVs());
    }

    @Override
    public PixelmonEVs evs() {
        return new PixelmonEVs(this.dereference().getEVs());
    }

    @Override
    public boolean isEgg() {
        return this.dereference().isEgg();
    }

    @Override
    public @Nullable String originalTrainer() {
        return this.dereference().getOriginalTrainer();
    }

    @Override
    public @Nullable UUID originalTrainerUUID() {
        return this.dereference().getOriginalTrainerUUID();
    }

    @Override
    public void setOriginalTrainer(ServerPlayer player) {
        this.dereference().setOriginalTrainer(player);
    }

    @Override
    public @Nullable UUID ownerUUID() {
        return this.dereference().getOwnerPlayerUUID();
    }

    @Override
    public @Nullable ServerPlayer ownerPlayer() {
        return this.dereference().getOwnerPlayer();
    }

    @Override
    public boolean isTradeable() {
        return !this.dereference().isUntradeable();
    }

    @Override
    public void setTradeable(boolean tradeable) {
        if (tradeable) {
            this.dereference().removeFlag(Flags.UNTRADEABLE);
        } else {
            this.dereference().addFlag(Flags.UNTRADEABLE);
        }
    }

    @Override
    public int dynamaxLevel() {
        return this.dereference().getDynamaxLevel();
    }

    @Override
    public void setDynamaxLevel(int level) {
        this.dereference().setDynamaxLevel(level);
    }

    @Override
    public boolean hasGigantamaxFactor() {
        return this.dereference().hasGigantamaxFactor();
    }

    @Override
    public void setGigantamaxFactor(boolean factor) {
        this.dereference().setGigantamaxFactor(factor);
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
    public @Nullable PixelmonPokemonEntity entity() {
        return this.dereference().getPixelmonEntity().map(PixelmonPokemonEntity::new).orElse(null);
    }

    @Override
    public PixelmonPokemonEntity spawn(ServerLevel level, Vec3 position) {
        return new PixelmonPokemonEntity(this.dereference().getOrSpawnPixelmon(level, position.x, position.y, position.z));
    }

    @Override
    public PixelmonPokemon copy() {
        var copy = PokemonFactory.copy(this.dereference());
        copy.setUUID(UUID.randomUUID());
        return new PixelmonPokemon(copy);
    }

    @Override
    public ItemStack toItem() {
        return SpriteItemHelper.getPhoto(this.dereference());
    }

    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().writeToNBT(new CompoundTag(), ServerUtils.registryAccess());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.toCompoundTag().toString());
    }

    public String palette() {
        return this.dereference().getPaletteName();
    }

    public List<String> flags() {
        return this.dereference().getFlags();
    }

    public boolean hasFlag(String flag) {
        return this.dereference().hasFlag(flag);
    }
}
