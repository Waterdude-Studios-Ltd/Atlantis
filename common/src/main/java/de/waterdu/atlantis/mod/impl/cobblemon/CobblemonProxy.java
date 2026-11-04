package de.waterdu.atlantis.mod.impl.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.api.npc.configuration.interaction.NoneNPCInteractionConfiguration;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.cobblemon.event.CobblemonEventBridge;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattle;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity.CobblemonPokemonEntity;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity.CobblemonTrainer;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.parsing.CobblemonPokemonSpec;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonAbility;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonElementalType;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonGender;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonNature;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonPokeBall;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonSpecies;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonStatus;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonTeraType;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves.CobblemonMoveCategory;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves.CobblemonMoveTemplate;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonStat;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage.CobblemonPC;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage.CobblemonParty;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage.CobblemonPokedex;
import de.waterdu.atlantis.mod.impl.util.ShowdownFormat;
import de.waterdu.atlantis.mod.impl.util.clause.TeamClause;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonFeature;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.BattleSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.TrainerSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UTrainer;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implements {@link PokemonDelegate} with Cobblemon.
 */
public class CobblemonProxy implements PokemonDelegate {

    private static final String FALLBACK_SPECIES = "bulbasaur";
    private static final List<String> TRAINER_AIS = List.of("0", "1", "2", "3", "4", "5");

    /**
     * Registers the Cobblemon event handlers with {@link CobblemonEventBridge#register()}.
     */
    @Override
    public void setup() {
        CobblemonEventBridge.register();
    }

    /**
     * Registers config serializers for Cobblemon species, moves, abilities and Poké Balls. A species is read from its
     * name or its Pokédex number and is written as its name.
     *
     * @param gson the Gson builder to register the serializers with
     */
    @Override
    public void registerConfigurableContexts(GsonBuilder gson) {
        Configurable.registerContext(gson, Species.class,
                (json, context) -> {
                    var primitive = json.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        return PokemonSpecies.getByPokedexNumber(primitive.getAsInt(), Cobblemon.MODID);
                    }
                    return this.findSpecies(primitive.getAsString());
                },
                (src, context) -> new JsonPrimitive(src.getResourceIdentifier().getPath())
        );

        Configurable.registerContext(gson, MoveTemplate.class,
                (json, context) -> Moves.getByName(json.getAsString()),
                (src, context) -> new JsonPrimitive(src.getName())
        );

        Configurable.registerContext(gson, AbilityTemplate.class,
                (json, context) -> Abilities.get(json.getAsString()),
                (src, context) -> new JsonPrimitive(src.getName())
        );

        Configurable.registerContext(gson, PokeBall.class,
                (json, context) -> this.findPokeBall(json.getAsString()),
                (src, context) -> new JsonPrimitive(src.getName().getPath())
        );
    }

    /**
     * Gets the party of the player from the Cobblemon storage.
     *
     * @param player the player
     * @return the wrapped party
     */
    @Override
    public CobblemonParty party(PlayerReference player) {
        return new CobblemonParty(this.storage().getParty(player.uuid(), ServerUtils.registryAccess()));
    }

    /**
     * Gets the PC of the player from the Cobblemon storage.
     *
     * @param player the player
     * @return the wrapped PC
     */
    @Override
    public CobblemonPC pc(PlayerReference player) {
        return new CobblemonPC(this.storage().getPC(player.uuid(), ServerUtils.registryAccess()));
    }

    /**
     * Gets the Pokédex data of the player from the Cobblemon player data.
     *
     * @param player the player
     * @return the wrapped Pokédex
     */
    @Override
    public CobblemonPokedex pokedex(PlayerReference player) {
        return new CobblemonPokedex(Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.uuid()));
    }

    /**
     * Checks if the player selected a starter, from the Cobblemon player data.
     *
     * @param player the player
     * @return {@code true} if the player selected a starter
     */
    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player.uuid()).getStarterSelected();
    }

    /**
     * Sets if the player selected a starter, in the Cobblemon player data.
     *
     * @param player the player
     * @param chosen {@code true} to mark the starter as selected
     */
    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player.uuid()).setStarterSelected(chosen);
    }

    /**
     * Joins the specs and parses them as Cobblemon Pokémon properties.
     *
     * @param specs the parts of the spec
     * @return the wrapped spec
     */
    @Override
    public CobblemonPokemonSpec spec(String... specs) {
        return new CobblemonPokemonSpec(PokemonProperties.Companion.parse(TextUtils.concat(specs)));
    }

    /**
     * @return {@code =}, the separator between a key and its value in a Cobblemon spec
     */
    @Override
    public String specDelimiter() {
        return "=";
    }

    /**
     * Gets a species by name. The name is trimmed, put in lower case, and stripped of characters that are not valid
     * in a resource path.
     *
     * @param name the name of the species
     * @return the species, or the fallback species if no species has the name
     */
    @Override
    public CobblemonSpecies species(String name) {
        var species = this.findSpecies(name);
        return new CobblemonSpecies(species == null ? this.fallbackSpecies() : species);
    }

    /**
     * Gets a Cobblemon species by its Pokédex number.
     *
     * @param dex the Pokédex number
     * @return the species, or the fallback species if no species has the number
     */
    @Override
    public CobblemonSpecies species(int dex) {
        var species = PokemonSpecies.getByPokedexNumber(dex, Cobblemon.MODID);
        return new CobblemonSpecies(species == null ? this.fallbackSpecies() : species);
    }

    /**
     * Checks if a species has the name.
     *
     * @param name the name of the species
     * @return {@code true} if a species has the name
     */
    @Override
    public boolean hasSpecies(String name) {
        return this.findSpecies(name) != null;
    }

    /**
     * Gets every species that Cobblemon implements.
     *
     * @return the implemented species
     */
    @Override
    public List<CobblemonSpecies> allSpecies() {
        var species = new ArrayList<CobblemonSpecies>();
        for (var implemented : PokemonSpecies.getImplemented()) {
            species.add(new CobblemonSpecies(implemented));
        }
        return species;
    }

    /**
     * Gets a random species from Cobblemon.
     *
     * @return the random species
     */
    @Override
    public CobblemonSpecies randomSpecies() {
        return new CobblemonSpecies(PokemonSpecies.random());
    }

    /**
     * Gets an ability by name.
     *
     * @param name the name of the ability
     * @return the ability, or {@code null} if no ability has the name
     */
    @Override
    public @Nullable CobblemonAbility ability(String name) {
        var ability = Abilities.get(name);
        return ability == null ? null : new CobblemonAbility(ability);
    }

    /**
     * Gets every Cobblemon ability.
     *
     * @return the abilities
     */
    @Override
    public List<CobblemonAbility> abilities() {
        return Abilities.all().stream().map(CobblemonAbility::new).toList();
    }

    /**
     * Gets a move by name.
     *
     * @param name the name of the move
     * @return the move, or {@code null} if no move has the name
     */
    @Override
    public @Nullable CobblemonMoveTemplate move(String name) {
        var move = Moves.getByName(name);
        return move == null ? null : new CobblemonMoveTemplate(move);
    }

    /**
     * Gets every Cobblemon move.
     *
     * @return the moves
     */
    @Override
    public List<CobblemonMoveTemplate> moves() {
        return Moves.all().stream().map(CobblemonMoveTemplate::new).toList();
    }

    /**
     * Gets a Poké Ball by name. A name without a namespace uses the Cobblemon namespace.
     *
     * @param name the name or resource location of the Poké Ball
     * @return the Poké Ball, or {@code null} if no Poké Ball has the name
     */
    @Override
    public @Nullable CobblemonPokeBall pokeBall(String name) {
        var ball = this.findPokeBall(name);
        return ball == null ? null : new CobblemonPokeBall(ball);
    }

    /**
     * Gets every Cobblemon Poké Ball.
     *
     * @return the Poké Balls
     */
    @Override
    public List<CobblemonPokeBall> pokeBalls() {
        return PokeBalls.all().stream().map(CobblemonPokeBall::new).toList();
    }

    /**
     * Gets an elemental type by name.
     *
     * @param name the name of the type
     * @return the type, or {@code null} if no type has the name
     */
    @Override
    public @Nullable CobblemonElementalType elementalType(String name) {
        var type = ElementalTypes.get(name);
        return type == null ? null : new CobblemonElementalType(type);
    }

    /**
     * Gets every Cobblemon elemental type.
     *
     * @return the elemental types
     */
    @Override
    public List<CobblemonElementalType> elementalTypes() {
        return ElementalTypes.all().stream().map(CobblemonElementalType::new).toList();
    }

    /**
     * Gets a Tera type by name. The name is trimmed and put in lower case.
     *
     * @param name the name of the Tera type
     * @return the Tera type, or {@code null} if no Tera type has the name or the name is not a valid resource path
     */
    @Override
    public @Nullable CobblemonTeraType teraType(String name) {
        try {
            var type = TeraTypes.get(name.trim().toLowerCase(Locale.ROOT));
            return type == null ? null : new CobblemonTeraType(type);
        } catch (ResourceLocationException e) {
            return null;
        }
    }

    /**
     * Gets every Cobblemon Tera type.
     *
     * @return the Tera types
     */
    @Override
    public List<CobblemonTeraType> teraTypes() {
        var types = new ArrayList<CobblemonTeraType>();
        TeraTypes.INSTANCE.forEach(type -> types.add(new CobblemonTeraType(type)));
        return types;
    }

    /**
     * Gets a stat by name with {@link CobblemonStat#of}.
     *
     * @param name the name of the stat
     * @return the stat, or {@code null} if no stat has the name
     */
    @Override
    public @Nullable CobblemonStat stat(String name) {
        return CobblemonStat.of(name);
    }

    /**
     * Gets every Cobblemon stat.
     *
     * @return the stats
     */
    @Override
    public List<CobblemonStat> stats() {
        return CobblemonStat.all();
    }

    /**
     * Gets the stats that are permanent.
     *
     * @return the stats for which {@link CobblemonStat#isPermanent} is {@code true}
     */
    @Override
    public List<CobblemonStat> permanentStats() {
        return CobblemonStat.all().stream().filter(CobblemonStat::isPermanent).toList();
    }

    /**
     * Gets a nature by name, in the Cobblemon namespace. The name is trimmed and put in lower case.
     *
     * @param name the name of the nature
     * @return the nature, or {@code null} if no nature has the name
     */
    @Override
    public @Nullable CobblemonNature nature(String name) {
        var resource = ResourceLocation.tryBuild(Cobblemon.MODID, name.trim().toLowerCase(Locale.ROOT));
        var nature = resource == null ? null : Natures.getNature(resource);
        return nature == null ? null : new CobblemonNature(nature);
    }

    /**
     * Gets every Cobblemon nature.
     *
     * @return the natures
     */
    @Override
    public List<CobblemonNature> natures() {
        return Natures.all().stream().map(CobblemonNature::new).toList();
    }

    /**
     * Gets a random nature from Cobblemon.
     *
     * @return the random nature
     */
    @Override
    public CobblemonNature randomNature() {
        return new CobblemonNature(Natures.getRandomNature());
    }

    /**
     * Gets a gender by name, ignoring case.
     *
     * @param name the name of the gender
     * @return the gender, or {@code null} if no gender has the name
     */
    @Override
    public @Nullable CobblemonGender gender(String name) {
        for (var gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(name)) {
                return new CobblemonGender(gender);
            }
        }
        return null;
    }

    /**
     * Gets every Cobblemon gender.
     *
     * @return the genders
     */
    @Override
    public List<CobblemonGender> genders() {
        var genders = new ArrayList<CobblemonGender>();
        for (var gender : Gender.values()) {
            genders.add(new CobblemonGender(gender));
        }
        return genders;
    }

    /**
     * Gets a status by name, ignoring case.
     *
     * @param name the name of the status
     * @return the status, or {@code null} if no status has the name
     */
    @Override
    public @Nullable CobblemonStatus status(String name) {
        for (var status : CobblemonStatus.all()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Gets every status from {@link CobblemonStatus#all()}.
     *
     * @return the statuses
     */
    @Override
    public List<CobblemonStatus> statuses() {
        return CobblemonStatus.all();
    }

    /**
     * Gets a move category by name. The name is put in lower case.
     *
     * @param name the name of the category
     * @return the category, or {@code null} if no category has the name
     */
    @Override
    public @Nullable CobblemonMoveCategory moveCategory(String name) {
        var category = DamageCategories.INSTANCE.get(name.toLowerCase(Locale.ROOT));
        return category == null ? null : new CobblemonMoveCategory(category);
    }

    /**
     * Gets the physical, special and status move categories.
     *
     * @return the three move categories, in that order
     */
    @Override
    public List<CobblemonMoveCategory> moveCategories() {
        var categories = DamageCategories.INSTANCE;
        return List.of(
                new CobblemonMoveCategory(categories.getPHYSICAL()),
                new CobblemonMoveCategory(categories.getSPECIAL()),
                new CobblemonMoveCategory(categories.getSTATUS())
        );
    }

    /**
     * Loads a Pokémon from NBT. If the tag is not valid, logs an error and creates a level 5 Pokémon of the fallback
     * species.
     *
     * @param tag the NBT of the Pokémon
     * @return the loaded Pokémon, or the fallback Pokémon
     */
    @Override
    public CobblemonPokemon pokemonFromTag(CompoundTag tag) {
        Pokemon pokemon;
        try {
            pokemon = Pokemon.Companion.loadFromNBT(ServerUtils.registryAccess(), tag);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to parse Cobblemon Pokémon data from tag: {}, {}", e.getMessage(), tag);
            pokemon = this.fallbackSpecies().create(5);
        }
        return new CobblemonPokemon(pokemon);
    }

    /**
     * Loads a Pokémon from JSON. If the JSON is not valid, logs an error and creates a level 5 Pokémon of the fallback
     * species.
     *
     * @param json the JSON object of the Pokémon
     * @return the loaded Pokémon, or the fallback Pokémon
     */
    @Override
    public CobblemonPokemon pokemonFromJson(JsonElement json) {
        Pokemon pokemon;
        try {
            pokemon = Pokemon.Companion.loadFromJSON(ServerUtils.registryAccess(), json.getAsJsonObject());
        } catch (Exception e) {
            AtlantisLogger.error("Failed to parse Cobblemon Pokémon data from json: {}, {}", e.getMessage(), json);
            pokemon = this.fallbackSpecies().create(5);
        }
        return new CobblemonPokemon(pokemon);
    }

    /**
     * Wraps the entity if it is a Cobblemon Pokémon entity.
     *
     * @param entity the entity
     * @return the wrapped entity, or {@code null} if the entity is not a Pokémon
     */
    @Override
    public @Nullable CobblemonPokemonEntity pokemonEntity(Entity entity) {
        return entity instanceof PokemonEntity pokemon ? new CobblemonPokemonEntity(pokemon) : null;
    }

    /**
     * Gets the battle that the player takes part in, from the Cobblemon battle registry.
     *
     * @param player the player
     * @return the battle, or {@code null} if the player is not in a battle
     */
    @Override
    public @Nullable CobblemonBattle battle(ServerPlayer player) {
        var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        return battle == null ? null : new CobblemonBattle(battle);
    }

    /**
     * Starts a battle between the player and the wild Pokémon. The future is already complete.
     *
     * @param player the player
     * @param wild the wild Pokémon entity
     * @return the battle, or {@code null} if the battle did not start
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        var result = BattleBuilder.INSTANCE.pve(player, (PokemonEntity) wild.dereference());
        return CompletableFuture.completedFuture(result instanceof SuccessfulBattleStart start ? new CobblemonBattle(start.getBattle()) : null);
    }

    /**
     * Starts a battle between two players. The future is already complete. If the battle does not start, both players
     * get the error.
     *
     * @param player1 the first player
     * @param team1 the team of the first player, or {@code null} for their party
     * @param player2 the second player
     * @param team2 the team of the second player, or {@code null} for their party
     * @param settings the battle settings
     * @return the battle, or {@code null} if the battle did not start
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings) {
        var party1 = this.battleParty(player1, team1, settings);
        var party2 = this.battleParty(player2, team2, settings);
        var result = BattleBuilder.INSTANCE.pvp1v1(player1, player2, null, null, this.battleFormat(settings), this.cloneParties(settings), settings.fullHeal(),
                player -> player.getUUID().equals(player1.getUUID()) ? party1 : party2);
        return CompletableFuture.completedFuture(this.battle(result, player1, player2));
    }

    /**
     * Starts a battle between the player and the trainer. The future is already complete. Sets the team of the trainer
     * to healed copies of its team, trimmed to the team size and lowered to the level cap. Sets the skill of the NPC
     * from the trainer AI.
     *
     * @param player the player
     * @param team the team of the player, or {@code null} for their party
     * @param trainer the trainer
     * @param trainerSettings the trainer settings
     * @param settings the battle settings
     * @return the battle, or {@code null} if the trainer is not a Cobblemon trainer, is in a battle, or the battle did
     *     not start
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startTrainerBattle(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, UTrainer<?> trainer, TrainerSettings trainerSettings, BattleSettings settings) {
        if (!(trainer instanceof CobblemonTrainer cobblemonTrainer) || trainer.isInBattle()) {
            return CompletableFuture.completedFuture(null);
        }
        var npc = cobblemonTrainer.dereference();
        var trainerTeam = new ArrayList<UPokemon<?>>();
        for (var pokemon : cobblemonTrainer.team()) {
            var copy = pokemon.copy();
            if (settings.hasLevelCap() && !settings.raiseToCap() && copy.level() > settings.levelCap()) {
                copy.setLevel(settings.levelCap());
            }
            copy.heal();
            trainerTeam.add(copy);
        }
        cobblemonTrainer.setTeam(trainerTeam.subList(0, Math.min(trainerTeam.size(), settings.teamSize())));
        var skill = this.skill(trainerSettings.ai());
        if (skill != null) {
            npc.setSkill(skill);
        }
        var result = BattleBuilder.INSTANCE.pvn(player, npc, null, this.battleFormat(settings), this.cloneParties(settings), settings.fullHeal(), this.battleParty(player, team, settings));
        return CompletableFuture.completedFuture(this.battle(result, player));
    }

    /**
     * Spawns a persistent NPC with the dummy NPC class and an empty party. The NPC cannot move, despawn or take
     * damage, and has no interaction.
     *
     * @param level the level to spawn in
     * @param position the position of the NPC
     * @param yaw the yaw of the body and the head
     * @param name the custom name of the NPC
     * @return the spawned trainer
     */
    @Override
    public CobblemonTrainer spawnTrainer(ServerLevel level, Vec3 position, float yaw, Component name) {
        var npc = new NPCEntity(level);
        var npcClass = NPCClasses.INSTANCE.dummy();
        npcClass.setCanDespawn(false);
        npcClass.setInteraction(new NoneNPCInteractionConfiguration());
        npc.setNpc(npcClass);
        npc.moveTo(position.x, position.y, position.z, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.initialize(1);
        npc.setCustomName(name);
        npc.setMovable(false);
        npc.setInvulnerable(Boolean.TRUE);
        npc.setPersistenceRequired();
        npc.setParty(new NPCPartyStore(npc));
        level.addFreshEntity(npc);
        return new CobblemonTrainer(npc);
    }

    /**
     * Wraps the entity if it is a Cobblemon NPC.
     *
     * @param entity the entity
     * @return the wrapped trainer, or {@code null} if the entity is not an NPC
     */
    @Override
    public @Nullable CobblemonTrainer trainer(Entity entity) {
        return entity instanceof NPCEntity npc ? new CobblemonTrainer(npc) : null;
    }

    /**
     * Checks if Cobblemon supports the feature. Trainer battles, level caps, raising to the cap, battle AI and
     * in-battle clauses are supported.
     *
     * @param feature the feature to check
     * @return {@code true} if the feature is supported
     */
    @Override
    public boolean supports(PokemonFeature feature) {
        return switch (feature) {
            case TRAINER_BATTLES, LEVEL_CAP, RAISE_TO_CAP, BATTLE_AI, IN_BATTLE_CLAUSES -> true;
            case TEAM_PREVIEW, TEAM_SELECTION_SCREEN, TURN_TIMER, BOSS_TIERS, BATTLE_GIMMICKS -> false;
        };
    }

    /**
     * @return the default team clauses, {@link TeamClause#DEFAULTS}
     */
    @Override
    public List<TeamClause> battleClauses() {
        return TeamClause.DEFAULTS;
    }

    /**
     * @return the skill levels {@code 0} to {@code 5} of a Cobblemon NPC, as strings
     */
    @Override
    public List<String> trainerAIs() {
        return TRAINER_AIS;
    }

    /**
     * Finds a held item by its Showdown name, first in the Cobblemon namespace, then in the Minecraft namespace.
     *
     * @param name the Showdown name of the item
     * @return the item, or {@link ItemStack#EMPTY} if no item has the name
     */
    @Override
    public ItemStack showdownItem(String name) {
        return ShowdownFormat.item(name, Cobblemon.MODID, "minecraft");
    }

    /**
     * Parses a trainer AI as an NPC skill level.
     *
     * @param ai the trainer AI, or {@code null} for none
     * @return the skill level, clamped to {@code 0} to {@code 5}, or {@code null} if the AI is {@code null} or not a
     *     number
     */
    private @Nullable Integer skill(@Nullable String ai) {
        if (ai == null) {
            return null;
        }
        try {
            return Math.clamp(Integer.parseInt(ai.trim()), 0, 5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if the battle must use copies of the parties. This is so when the settings clone the teams or give no
     * experience.
     *
     * @param settings the battle settings
     * @return {@code true} if the battle must clone the parties
     */
    private boolean cloneParties(BattleSettings settings) {
        return settings.cloneTeams() || !settings.experience();
    }

    /**
     * Creates the Gen 9 battle format for the settings. Adds the Showdown rule of each team clause in the settings. If
     * the settings raise Pokémon to the level cap, the format adjusts levels to the cap.
     *
     * @param settings the battle settings
     * @return the battle format
     */
    private BattleFormat battleFormat(BattleSettings settings) {
        var base = switch (settings.format()) {
            case SINGLES -> BattleFormat.Companion.getGEN_9_SINGLES();
            case DOUBLES -> BattleFormat.Companion.getGEN_9_DOUBLES();
            case TRIPLES -> BattleFormat.Companion.getGEN_9_TRIPLES();
        };
        var rules = new LinkedHashSet<>(base.getRuleSet());
        for (var id : settings.clauses()) {
            if (this.battleClause(id) instanceof TeamClause clause && clause.showdownRule() != null) {
                rules.add(clause.showdownRule());
            }
        }
        var adjustLevel = settings.hasLevelCap() && settings.raiseToCap() ? settings.levelCap() : -1;
        return base.copy(base.getMod(), base.getBattleType(), rules, base.getGen(), adjustLevel);
    }

    /**
     * The party that a player battles with. This is their real party when no change is necessary. Otherwise, it is a
     * store of copies, trimmed to the team size and lowered to the level cap.
     */
    private PartyStore battleParty(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, BattleSettings settings) {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        var members = new ArrayList<UPokemon<?>>();
        if (team == null) {
            for (var pokemon : party) {
                members.add(new CobblemonPokemon(pokemon));
            }
        } else {
            members.addAll(team);
        }
        var capped = settings.hasLevelCap() && !settings.raiseToCap() && members.stream().anyMatch(pokemon -> pokemon.level() > settings.levelCap());
        if (team == null && !capped && members.size() <= settings.teamSize()) {
            return party;
        }
        var store = new PartyStore(UUID.randomUUID());
        for (var slot = 0; slot < Math.min(members.size(), Math.min(settings.teamSize(), 6)); slot++) {
            var copy = members.get(slot).copy();
            if (capped && copy.level() > settings.levelCap()) {
                copy.setLevel(settings.levelCap());
            }
            store.set(slot, CobblemonPokemon.unwrap(copy));
        }
        return store;
    }

    /**
     * Wraps the battle of a successful start. If the start failed with an error, sends the error to each player.
     *
     * @param result the result of the battle start
     * @param players the players that get the error
     * @return the battle, or {@code null} if the battle did not start
     */
    private @Nullable CobblemonBattle battle(BattleStartResult result, ServerPlayer... players) {
        if (result instanceof SuccessfulBattleStart start) {
            return new CobblemonBattle(start.getBattle());
        }
        if (result instanceof ErroredBattleStart errored) {
            for (var player : players) {
                errored.sendTo(player, component -> component);
            }
        }
        return null;
    }

    /**
     * @return the Cobblemon storage manager
     */
    private PokemonStoreManager storage() {
        return Cobblemon.INSTANCE.getStorage();
    }

    /**
     * Finds a species by name, in the Cobblemon namespace. The name is trimmed, put in lower case, and stripped of
     * characters that are not valid in a resource path.
     *
     * @param name the name of the species
     * @return the species, or {@code null} if no species has the name
     */
    private @Nullable Species findSpecies(String name) {
        var resource = ResourceLocation.tryBuild(Cobblemon.MODID, name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", ""));
        return resource == null ? null : PokemonSpecies.getByIdentifier(resource);
    }

    /**
     * Finds a Poké Ball by name. The name is trimmed and put in lower case. A name without a namespace uses the
     * Cobblemon namespace.
     *
     * @param name the name or resource location of the Poké Ball
     * @return the Poké Ball, or {@code null} if no Poké Ball has the name
     */
    private @Nullable PokeBall findPokeBall(String name) {
        var trimmed = name.trim().toLowerCase(Locale.ROOT);
        var resource = trimmed.contains(":") ? ResourceLocation.tryParse(trimmed) : ResourceLocation.tryBuild(Cobblemon.MODID, trimmed);
        return resource == null ? null : PokeBalls.getPokeBall(resource);
    }

    /**
     * Gets the species to use when a lookup fails.
     *
     * @return Bulbasaur, or a random species if Bulbasaur is not registered
     */
    private Species fallbackSpecies() {
        var species = this.findSpecies(FALLBACK_SPECIES);
        return species == null ? PokemonSpecies.random() : species;
    }
}
