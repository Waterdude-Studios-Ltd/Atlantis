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

public class CobblemonProxy implements PokemonDelegate {

    private static final String FALLBACK_SPECIES = "bulbasaur";
    private static final List<String> TRAINER_AIS = List.of("0", "1", "2", "3", "4", "5");

    @Override
    public void setup() {
        CobblemonEventBridge.register();
    }

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

    @Override
    public CobblemonParty party(PlayerReference player) {
        return new CobblemonParty(this.storage().getParty(player.uuid(), ServerUtils.registryAccess()));
    }

    @Override
    public CobblemonPC pc(PlayerReference player) {
        return new CobblemonPC(this.storage().getPC(player.uuid(), ServerUtils.registryAccess()));
    }

    @Override
    public CobblemonPokedex pokedex(PlayerReference player) {
        return new CobblemonPokedex(Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.uuid()));
    }

    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player.uuid()).getStarterSelected();
    }

    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player.uuid()).setStarterSelected(chosen);
    }

    @Override
    public CobblemonPokemonSpec spec(String... specs) {
        return new CobblemonPokemonSpec(PokemonProperties.Companion.parse(TextUtils.concat(specs)));
    }

    @Override
    public String specDelimiter() {
        return "=";
    }

    @Override
    public CobblemonSpecies species(String name) {
        var species = this.findSpecies(name);
        return new CobblemonSpecies(species == null ? this.fallbackSpecies() : species);
    }

    @Override
    public CobblemonSpecies species(int dex) {
        var species = PokemonSpecies.getByPokedexNumber(dex, Cobblemon.MODID);
        return new CobblemonSpecies(species == null ? this.fallbackSpecies() : species);
    }

    @Override
    public boolean hasSpecies(String name) {
        return this.findSpecies(name) != null;
    }

    @Override
    public List<CobblemonSpecies> allSpecies() {
        var species = new ArrayList<CobblemonSpecies>();
        for (var implemented : PokemonSpecies.getImplemented()) {
            species.add(new CobblemonSpecies(implemented));
        }
        return species;
    }

    @Override
    public CobblemonSpecies randomSpecies() {
        return new CobblemonSpecies(PokemonSpecies.random());
    }

    @Override
    public @Nullable CobblemonAbility ability(String name) {
        var ability = Abilities.get(name);
        return ability == null ? null : new CobblemonAbility(ability);
    }

    @Override
    public List<CobblemonAbility> abilities() {
        return Abilities.all().stream().map(CobblemonAbility::new).toList();
    }

    @Override
    public @Nullable CobblemonMoveTemplate move(String name) {
        var move = Moves.getByName(name);
        return move == null ? null : new CobblemonMoveTemplate(move);
    }

    @Override
    public List<CobblemonMoveTemplate> moves() {
        return Moves.all().stream().map(CobblemonMoveTemplate::new).toList();
    }

    @Override
    public @Nullable CobblemonPokeBall pokeBall(String name) {
        var ball = this.findPokeBall(name);
        return ball == null ? null : new CobblemonPokeBall(ball);
    }

    @Override
    public List<CobblemonPokeBall> pokeBalls() {
        return PokeBalls.all().stream().map(CobblemonPokeBall::new).toList();
    }

    @Override
    public @Nullable CobblemonElementalType elementalType(String name) {
        var type = ElementalTypes.get(name);
        return type == null ? null : new CobblemonElementalType(type);
    }

    @Override
    public List<CobblemonElementalType> elementalTypes() {
        return ElementalTypes.all().stream().map(CobblemonElementalType::new).toList();
    }

    @Override
    public @Nullable CobblemonStat stat(String name) {
        return CobblemonStat.of(name);
    }

    @Override
    public List<CobblemonStat> stats() {
        return CobblemonStat.all();
    }

    @Override
    public List<CobblemonStat> permanentStats() {
        return CobblemonStat.all().stream().filter(CobblemonStat::isPermanent).toList();
    }

    @Override
    public @Nullable CobblemonNature nature(String name) {
        var resource = ResourceLocation.tryBuild(Cobblemon.MODID, name.trim().toLowerCase(Locale.ROOT));
        var nature = resource == null ? null : Natures.getNature(resource);
        return nature == null ? null : new CobblemonNature(nature);
    }

    @Override
    public List<CobblemonNature> natures() {
        return Natures.all().stream().map(CobblemonNature::new).toList();
    }

    @Override
    public CobblemonNature randomNature() {
        return new CobblemonNature(Natures.getRandomNature());
    }

    @Override
    public @Nullable CobblemonGender gender(String name) {
        for (var gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(name)) {
                return new CobblemonGender(gender);
            }
        }
        return null;
    }

    @Override
    public List<CobblemonGender> genders() {
        var genders = new ArrayList<CobblemonGender>();
        for (var gender : Gender.values()) {
            genders.add(new CobblemonGender(gender));
        }
        return genders;
    }

    @Override
    public @Nullable CobblemonStatus status(String name) {
        for (var status : CobblemonStatus.all()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public List<CobblemonStatus> statuses() {
        return CobblemonStatus.all();
    }

    @Override
    public @Nullable CobblemonMoveCategory moveCategory(String name) {
        var category = DamageCategories.INSTANCE.get(name.toLowerCase(Locale.ROOT));
        return category == null ? null : new CobblemonMoveCategory(category);
    }

    @Override
    public List<CobblemonMoveCategory> moveCategories() {
        var categories = DamageCategories.INSTANCE;
        return List.of(
                new CobblemonMoveCategory(categories.getPHYSICAL()),
                new CobblemonMoveCategory(categories.getSPECIAL()),
                new CobblemonMoveCategory(categories.getSTATUS())
        );
    }

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

    @Override
    public @Nullable CobblemonPokemonEntity pokemonEntity(Entity entity) {
        return entity instanceof PokemonEntity pokemon ? new CobblemonPokemonEntity(pokemon) : null;
    }

    @Override
    public @Nullable CobblemonBattle battle(ServerPlayer player) {
        var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        return battle == null ? null : new CobblemonBattle(battle);
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        var result = BattleBuilder.INSTANCE.pve(player, (PokemonEntity) wild.dereference());
        return CompletableFuture.completedFuture(result instanceof SuccessfulBattleStart start ? new CobblemonBattle(start.getBattle()) : null);
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings) {
        var party1 = this.battleParty(player1, team1, settings);
        var party2 = this.battleParty(player2, team2, settings);
        var result = BattleBuilder.INSTANCE.pvp1v1(player1, player2, null, null, this.battleFormat(settings), this.cloneParties(settings), settings.fullHeal(),
                player -> player.getUUID().equals(player1.getUUID()) ? party1 : party2);
        return CompletableFuture.completedFuture(this.battle(result, player1, player2));
    }

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

    @Override
    public @Nullable CobblemonTrainer trainer(Entity entity) {
        return entity instanceof NPCEntity npc ? new CobblemonTrainer(npc) : null;
    }

    @Override
    public boolean supports(PokemonFeature feature) {
        return switch (feature) {
            case TRAINER_BATTLES, LEVEL_CAP, RAISE_TO_CAP, BATTLE_AI, IN_BATTLE_CLAUSES -> true;
            case TEAM_PREVIEW, TEAM_SELECTION_SCREEN, TURN_TIMER, BOSS_TIERS, BATTLE_GIMMICKS -> false;
        };
    }

    @Override
    public List<TeamClause> battleClauses() {
        return TeamClause.DEFAULTS;
    }

    @Override
    public List<String> trainerAIs() {
        return TRAINER_AIS;
    }

    @Override
    public ItemStack showdownItem(String name) {
        return ShowdownFormat.item(name, Cobblemon.MODID, "minecraft");
    }

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

    private boolean cloneParties(BattleSettings settings) {
        return settings.cloneTeams() || !settings.experience();
    }

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
     * The party a player battles with: their real party when nothing needs changing, otherwise a store of copies
     * trimmed to the team size and lowered to the level cap.
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

    private PokemonStoreManager storage() {
        return Cobblemon.INSTANCE.getStorage();
    }

    private @Nullable Species findSpecies(String name) {
        var resource = ResourceLocation.tryBuild(Cobblemon.MODID, name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", ""));
        return resource == null ? null : PokemonSpecies.getByIdentifier(resource);
    }

    private @Nullable PokeBall findPokeBall(String name) {
        var trimmed = name.trim().toLowerCase(Locale.ROOT);
        var resource = trimmed.contains(":") ? ResourceLocation.tryParse(trimmed) : ResourceLocation.tryBuild(Cobblemon.MODID, trimmed);
        return resource == null ? null : PokeBalls.getPokeBall(resource);
    }

    private Species fallbackSpecies() {
        var species = this.findSpecies(FALLBACK_SPECIES);
        return species == null ? PokemonSpecies.random() : species;
    }
}
