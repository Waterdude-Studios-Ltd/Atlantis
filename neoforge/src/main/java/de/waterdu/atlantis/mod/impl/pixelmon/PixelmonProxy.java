package de.waterdu.atlantis.mod.impl.pixelmon;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.api.context.ContextKeys;
import com.pixelmonmod.pixelmon.api.model.type.PlayerRenderingHandler;
import com.pixelmonmod.pixelmon.api.npc.entity.EntityProperties;
import com.pixelmonmod.pixelmon.api.pokedex.PokeDexStorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTier;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTiers;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.texture.type.PlayerSkinURLTextureProvider;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.api.BattleBuilder;
import com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelectionRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.battles.controller.ai.BattleAI;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.EntityParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.entities.npcs.NPC;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumOldGenMode;
import com.pixelmonmod.pixelmon.enums.TextJustification;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import com.pixelmonmod.pixelmon.items.PokeBagItem;
import com.pixelmonmod.pixelmon.items.UIElementItem;
import com.pixelmonmod.pixelmon.items.helpers.ItemHelper;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.FileExtensions;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.event.NeoForgeEventBus;
import de.waterdu.atlantis.mod.impl.pixelmon.event.PixelmonEventBridge;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattle;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattleClause;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity.PixelmonPokemonEntity;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity.PixelmonTrainer;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.parsing.PixelmonPokemonSpec;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonAbility;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonElementalType;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonGender;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonNature;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonPokeBall;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonStatus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves.PixelmonMoveCategory;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves.PixelmonMoveTemplate;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonStat;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage.PixelmonPC;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage.PixelmonParty;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage.PixelmonPokedex;
import de.waterdu.atlantis.mod.impl.util.ShowdownFormat;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonFeature;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.BattleSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.TrainerSettings;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UTrainer;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemUtils;
import de.waterdu.atlantis.util.item.ProcessorUtils;
import de.waterdu.atlantis.util.java.collection.ArrayUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PixelmonProxy implements PokemonDelegate, EconomyDelegate<PlayerPartyStorage> {

    @Override
    public void setup() {
        PixelmonEventBridge.register(Pixelmon.EVENT_BUS);
    }

    @Override
    public void registerConfigurableContexts(GsonBuilder gson) {
        Configurable.registerContext(gson, Species.class,
                (json, context) -> {
                    var primitive = json.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        return PixelmonSpecies.fromName(primitive.getAsString()).getValueUnsafe();
                    } else if (primitive.isNumber()) {
                        return PixelmonSpecies.fromNationalDex(primitive.getAsInt());
                    } else {
                        return null;
                    }
                },
                (src, context) -> new JsonPrimitive(src.getName())
        );

        Configurable.registerContext(gson, ImmutableAttack.class,
                (json, context) -> AttackRegistry.getAttackBase(json.getAsString()).orElse(null),
                (src, context) -> new JsonPrimitive(src.getAttackName())
        );

        Configurable.registerContext(gson, Ability.class,
                (json, context) -> AbilityRegistry.getNewAbility(json.getAsString()).orElse(null),
                (src, context) -> new JsonPrimitive(src.getName())
        );

        Configurable.registerContext(gson, PokeBall.class,
                (json, context) -> PokeBallRegistry.getPokeBall(json.getAsString()).getValueUnsafe(),
                (src, context) -> new JsonPrimitive(src.getName())
        );
    }

    @Override
    public void registerEventBus(Consumer<UEventBus<?, ?>> consumer) {
        consumer.accept(new NeoForgeEventBus(Pixelmon.EVENT_BUS));
    }

    @Override
    public ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return ArrayUtils.expand(
                PokemonDelegate.super.makeItemPreprocessors(),
                ConfigurableItemStack.Preprocessor.single(name ->
                        PokeBallRegistry.getPokeBall(name).orElse(PokeBallRegistry.POKE_BALL).getLidItem()
                )
        );
    }

    @Override
    public ConfigurableItemStack.Processor[] makeItemProcessors() {
        return new ConfigurableItemStack.Processor[]{
                new ConfigurableItemStack.Processor((kv, s) -> {
                    if (!kv[1].endsWith(FileExtensions.PNG)) {
                        kv[1] += FileExtensions.PNG;
                    }
                    ItemUtils.tag(s, tag -> tag.putString(UIElementItem.IMAGE, kv[1]));
                }),
                new ConfigurableItemStack.Processor((kv, s) -> {
                    if (!kv[1].endsWith(FileExtensions.PNG)) {
                        kv[1] += FileExtensions.PNG;
                    }
                    ItemUtils.tag(s, tag -> tag.putString(UIElementItem.HOVER, kv[1]));
                }),
                new ConfigurableItemStack.Processor((kv, s) -> ItemUtils.tag(s, tag -> tag.putString(UIElementItem.TEXT, kv[1]))),
                new ConfigurableItemStack.Processor((kv, s) -> ItemUtils.tag(s, tag -> tag.putString(UIElementItem.HOVER_TEXT, kv[1]))),
                new ConfigurableItemStack.Processor((kv, s) -> {
                    try {
                        short justification;
                        if (TextUtils.isNumberParsable(kv[1])) {
                            justification = Short.parseShort(kv[1]);
                        } else {
                            justification = (short) (TextJustification.valueOf(kv[1].toUpperCase(Locale.ROOT)).ordinal() - 1);
                        }
                        ItemUtils.tag(s, tag -> tag.putShort(UIElementItem.TEXT_JUSTIFICATION, justification));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        AtlantisLogger.warn(ProcessorUtils.INVALID_PROCESSOR_ERROR, ConfigurableItemStack.Processor.UI_TEXT_JUSTIFICATION_IDENTIFIER, kv[1]);
                    }
                }),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.shortValue(kv, s, UIElementItem.TEXT_SCALE, ConfigurableItemStack.Processor.UI_TEXT_SCALE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.X_OFFSET, ConfigurableItemStack.Processor.UI_X_OFFSET_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.Y_OFFSET, ConfigurableItemStack.Processor.UI_Y_OFFSET_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.dualIntValue(kv, s, UIElementItem.X_OFFSET, UIElementItem.Y_OFFSET, ConfigurableItemStack.Processor.UI_OFFSET_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.X_OVERRIDE, ConfigurableItemStack.Processor.UI_X_OVERRIDE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.Y_OVERRIDE, ConfigurableItemStack.Processor.UI_Y_OVERRIDE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.dualIntValue(kv, s, UIElementItem.X_OVERRIDE, UIElementItem.Y_OVERRIDE, ConfigurableItemStack.Processor.UI_OVERRIDE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.Z_LEVEL, ConfigurableItemStack.Processor.UI_Z_LEVEL_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.IMAGE_WIDTH, ConfigurableItemStack.Processor.UI_IMAGE_WIDTH_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.intValue(kv, s, UIElementItem.IMAGE_HEIGHT, ConfigurableItemStack.Processor.UI_IMAGE_HEIGHT_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.dualIntValue(kv, s, UIElementItem.IMAGE_WIDTH, UIElementItem.IMAGE_HEIGHT, ConfigurableItemStack.Processor.UI_IMAGE_SIZE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.floatValue(kv, s, UIElementItem.IMAGE_U, ConfigurableItemStack.Processor.UI_IMAGE_U_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.floatValue(kv, s, UIElementItem.IMAGE_V, ConfigurableItemStack.Processor.UI_IMAGE_V_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.dualFloatValue(kv, s, UIElementItem.IMAGE_U, UIElementItem.IMAGE_V, ConfigurableItemStack.Processor.UI_IMAGE_UV_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.floatValue(kv, s, UIElementItem.SCALE_X, ConfigurableItemStack.Processor.UI_SCALE_X_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.floatValue(kv, s, UIElementItem.SCALE_Y, ConfigurableItemStack.Processor.UI_SCALE_Y_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.dualFloatValue(kv, s, UIElementItem.SCALE_X, UIElementItem.SCALE_Y, ConfigurableItemStack.Processor.UI_SCALE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.floatValue(kv, s, UIElementItem.ROTATION, ConfigurableItemStack.Processor.UI_ROTATION_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.R, ConfigurableItemStack.Processor.UI_RED_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.G, ConfigurableItemStack.Processor.UI_GREEN_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.B, ConfigurableItemStack.Processor.UI_BLUE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.A, ConfigurableItemStack.Processor.UI_ALPHA_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.colorValue(kv, s, UIElementItem.R, UIElementItem.G, UIElementItem.B, UIElementItem.A, ConfigurableItemStack.Processor.UI_COLOR_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.HR, ConfigurableItemStack.Processor.UI_HOVER_RED_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.HG, ConfigurableItemStack.Processor.UI_HOVER_GREEN_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.HB, ConfigurableItemStack.Processor.UI_HOVER_BLUE_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.singleColorValue(kv, s, UIElementItem.HA, ConfigurableItemStack.Processor.UI_HOVER_ALPHA_IDENTIFIER)),
                new ConfigurableItemStack.Processor((kv, s) -> ProcessorUtils.colorValue(kv, s, UIElementItem.HR, UIElementItem.HG, UIElementItem.HB, UIElementItem.HA, ConfigurableItemStack.Processor.UI_HOVER_COLOR_IDENTIFIER))
        };
    }

    @Override
    public PixelmonParty party(PlayerReference player) {
        return new PixelmonParty(player.uuid(), StorageProxy.getParty(player.uuid()));
    }

    @Override
    public PixelmonPC pc(PlayerReference player) {
        return new PixelmonPC(player.uuid(), StorageProxy.getPCForPlayer(player.uuid()));
    }

    @Override
    public PixelmonPokedex pokedex(PlayerReference player) {
        return new PixelmonPokedex(PokeDexStorageProxy.getStorage(player.uuid()));
    }

    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        return StorageProxy.getPartyNow(player.uuid()).starterPicked;
    }

    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        var party = StorageProxy.getPartyNow(player.uuid());
        party.starterPicked = chosen;
        party.setNeedsSaving();
    }

    @Override
    public PixelmonPokemonSpec spec(String... specs) {
        return new PixelmonPokemonSpec(PokemonSpecificationProxy.create(specs).get());
    }

    @Override
    public String specDelimiter() {
        return ":";
    }

    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies species(String name) {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(
                PixelmonSpecies.fromName(name).orElse(PixelmonSpecies.MISSINGNO.getValueUnsafe())
        );
    }

    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies species(int dex) {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(
                PixelmonSpecies.fromDex(dex).orElse(PixelmonSpecies.MISSINGNO.getValueUnsafe())
        );
    }

    @Override
    public PixelmonPokemon pokemonFromTag(CompoundTag tag) {
        Pokemon pokemon;
        try {
            pokemon = PokemonFactory.create(tag, ServerUtils.registryAccess());
        } catch (Exception e) {
            AtlantisLogger.error("Failed to parse Pixelmon Pokémon data from tag: {}, {}", e.getMessage(), tag);
            pokemon = PokemonFactory.create(PixelmonSpecies.MISSINGNO.getValueUnsafe());
        }
        return new PixelmonPokemon(pokemon);
    }

    @Override
    public PixelmonPokemon pokemonFromJson(JsonElement json) {
        var nbt = json.getAsString();
        try {
            return this.pokemonFromTag(TagParser.parseTag(nbt));
        } catch (CommandSyntaxException e) {
            AtlantisLogger.error("Failed to parse Pixelmon Pokémon data from json: {}, {}", e.getMessage(), nbt);
            return new PixelmonPokemon(PokemonFactory.create(PixelmonSpecies.MISSINGNO.getValueUnsafe()));
        }
    }

    @Override
    public boolean hasSpecies(String name) {
        return PixelmonSpecies.has(name);
    }

    @Override
    public List<de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies> allSpecies() {
        return PixelmonSpecies.getAll().stream().map(de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies::new).toList();
    }

    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies randomSpecies() {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(PixelmonSpecies.getRandomSpecies());
    }

    @Override
    public @Nullable PixelmonAbility ability(String name) {
        return AbilityRegistry.getAbility(name).map(PixelmonAbility::new).orElse(null);
    }

    @Override
    public List<PixelmonAbility> abilities() {
        return AbilityRegistry.getAllAbilities().stream().map(PixelmonAbility::new).toList();
    }

    @Override
    public @Nullable PixelmonMoveTemplate move(String name) {
        return AttackRegistry.getAttackBase(name).map(PixelmonMoveTemplate::new).orElse(null);
    }

    @Override
    public List<PixelmonMoveTemplate> moves() {
        return AttackRegistry.getAllAttacks().stream().map(PixelmonMoveTemplate::new).toList();
    }

    @Override
    public @Nullable PixelmonPokeBall pokeBall(String name) {
        return PokeBallRegistry.getPokeBall(name).getValue().map(PixelmonPokeBall::new).orElse(null);
    }

    @Override
    public List<PixelmonPokeBall> pokeBalls() {
        return PokeBallRegistry.getAll().stream().map(PixelmonPokeBall::new).toList();
    }

    @Override
    public @Nullable PixelmonElementalType elementalType(String name) {
        var key = Type.parseOrNull(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).getHolder(key).map(PixelmonElementalType::new).orElse(null);
    }

    @Override
    public List<PixelmonElementalType> elementalTypes() {
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).holders().map(PixelmonElementalType::new).toList();
    }

    @Override
    public @Nullable PixelmonStat stat(String name) {
        return PixelmonStat.of(name);
    }

    @Override
    public List<PixelmonStat> stats() {
        return PixelmonStat.all();
    }

    @Override
    public List<PixelmonStat> permanentStats() {
        return PixelmonStat.all().stream().filter(PixelmonStat::isPermanent).toList();
    }

    @Override
    public @Nullable PixelmonNature nature(String name) {
        return Nature.hasNature(name) ? new PixelmonNature(Nature.natureFromString(name)) : null;
    }

    @Override
    public List<PixelmonNature> natures() {
        return Arrays.stream(Nature.values()).map(PixelmonNature::new).toList();
    }

    @Override
    public PixelmonNature randomNature() {
        return new PixelmonNature(Nature.getRandomNature());
    }

    @Override
    public @Nullable PixelmonGender gender(String name) {
        for (var gender : this.genders()) {
            if (gender.name().equalsIgnoreCase(name)) {
                return gender;
            }
        }
        return null;
    }

    @Override
    public List<PixelmonGender> genders() {
        return Arrays.stream(Gender.values()).map(PixelmonGender::new).toList();
    }

    @Override
    public @Nullable PixelmonStatus status(String name) {
        for (var status : PixelmonStatus.all()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public List<PixelmonStatus> statuses() {
        return PixelmonStatus.all();
    }

    @Override
    public @Nullable PixelmonMoveCategory moveCategory(String name) {
        var category = AttackCategory.parseOrNull(name);
        return category == null ? null : new PixelmonMoveCategory(category);
    }

    @Override
    public List<PixelmonMoveCategory> moveCategories() {
        return Arrays.stream(AttackCategory.values()).map(PixelmonMoveCategory::new).toList();
    }

    @Override
    public @Nullable PixelmonPokemonEntity pokemonEntity(Entity entity) {
        return entity instanceof PixelmonEntity pixelmon ? new PixelmonPokemonEntity(pixelmon) : null;
    }

    @Override
    public @Nullable PixelmonBattle battle(ServerPlayer player) {
        var battle = BattleRegistry.getBattle(player);
        return battle == null ? null : new PixelmonBattle(battle);
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        return this.start(BattleBuilder.builder().registryAccess(ServerUtils.registryAccess()).teamOne(new PlayerParticipant(player)).teamTwo(new WildPixelmonParticipant((PixelmonEntity) wild.dereference())));
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startPlayerBattle(ServerPlayer player1, @Nullable List<? extends UPokemon<?>> team1, ServerPlayer player2, @Nullable List<? extends UPokemon<?>> team2, BattleSettings settings) {
        var builder = this.battleBuilder(settings)
                .teamOne(new PlayerParticipant(player1, this.battleTeam(player1, team1, settings)))
                .teamTwo(new PlayerParticipant(player2, this.battleTeam(player2, team2, settings)));
        if (settings.teamSelectTime() > 0 && team1 == null && team2 == null) {
            builder.teamSelection(true).teamSelectionBuilder(TeamSelectionRegistry.builder().notCloseable());
        }
        return this.start(builder);
    }

    @Override
    public CompletableFuture<@Nullable UBattle<?>> startTrainerBattle(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, UTrainer<?> trainer, TrainerSettings trainerSettings, BattleSettings settings) {
        if (!(trainer instanceof PixelmonTrainer pixelmonTrainer) || trainer.isInBattle()) {
            return CompletableFuture.completedFuture(null);
        }
        var npc = pixelmonTrainer.dereference();
        var trainerTeam = new ArrayList<Pokemon>();
        for (var pokemon : npc.getParty().getAllNonNull()) {
            if (trainerTeam.size() < settings.teamSize()) {
                var copy = PokemonFactory.copy(pokemon);
                copy.heal();
                trainerTeam.add(copy);
            }
        }
        if (trainerTeam.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var gimmick = this.gimmick(settings.gimmick());
        var participant = EntityParticipant.builder()
                .entity(npc)
                .storage(npc.getParty())
                .pokemon(trainerTeam.toArray(Pokemon[]::new))
                .bossTier(BossTierRegistry.getBossTierOrNotBoss(trainerSettings.bossTier() == null ? BossTiers.NOT_BOSS : trainerSettings.bossTier()))
                .aiMode(this.battleAI(trainerSettings.ai()))
                .canMega(gimmick == EnumOldGenMode.Mega || gimmick == EnumOldGenMode.Both)
                .canDynamax(gimmick == EnumOldGenMode.Dynamax || gimmick == EnumOldGenMode.Both)
                .build();
        return this.start(this.battleBuilder(settings)
                .teamOne(new PlayerParticipant(player, this.battleTeam(player, team, settings)))
                .teamTwo(participant));
    }

    @Override
    public PixelmonTrainer spawnTrainer(ServerLevel level, Vec3 position, float yaw, Component name) {
        var npc = NPC.builder()
                .name(name)
                .renderingHandler(new PlayerRenderingHandler(false, new PlayerSkinURLTextureProvider("", ResourceLocation.fromNamespaceAndPath(Pixelmon.MODID, ""))))
                .properties(EntityProperties.builder().invulnerable().immovable().nameplate().notPushable())
                .noInteractions()
                .noTracking()
                .position(position)
                .build(level);
        npc.moveTo(position.x, position.y, position.z, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        return new PixelmonTrainer(npc);
    }

    @Override
    public @Nullable PixelmonTrainer trainer(Entity entity) {
        return entity instanceof NPC npc ? new PixelmonTrainer(npc) : null;
    }

    @Override
    public boolean supports(PokemonFeature feature) {
        return true;
    }

    @Override
    public List<PixelmonBattleClause> battleClauses() {
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.BATTLE_RULE_REGISTRY).holders().map(PixelmonBattleClause::new).toList();
    }

    @Override
    public List<String> trainerAIs() {
        return ServerUtils.registryAccess().registryOrThrow(BattleAI.REGISTRY).holders().map(holder -> this.shortId(holder.key().location())).toList();
    }

    @Override
    public List<String> bossTiers() {
        return BossTierRegistry.getBossTiers().stream().map(BossTier::getID).toList();
    }

    @Override
    public List<String> battleGimmicks() {
        return Arrays.stream(EnumOldGenMode.values()).map(mode -> mode.name().toLowerCase(Locale.ROOT)).toList();
    }

    @Override
    public ItemStack showdownItem(String name) {
        return ShowdownFormat.item(name, Pixelmon.MODID, "minecraft");
    }

    @Override
    public List<ItemStack> containedItems(ItemStack stack) {
        var items = PokemonDelegate.super.containedItems(stack);
        if (stack.getItem() instanceof PokeBagItem) {
            var tag = ItemHelper.getTag(stack);
            if (tag != null) {
                var bag = PokeBagItem.PokeBag.fromTag(tag, ServerUtils.registryAccess());
                for (var slot = 0; slot < bag.getContainerSize(); slot++) {
                    var item = bag.getItem(slot);
                    if (!item.isEmpty()) {
                        items.add(item);
                    }
                }
            }
        }
        return items;
    }

    private String shortId(ResourceLocation location) {
        return location.getNamespace().equals(Pixelmon.MODID) ? location.getPath() : location.toString();
    }

    private ResourceKey<BattleAI> battleAI(@Nullable String ai) {
        if (ai == null || ai.isBlank()) {
            return BattleAI.RANDOM;
        }
        var location = ai.contains(":") ? ResourceLocation.tryParse(ai) : ResourceLocation.tryBuild(Pixelmon.MODID, ai.toLowerCase(Locale.ROOT));
        if (location == null || !ServerUtils.registryAccess().registryOrThrow(BattleAI.REGISTRY).containsKey(location)) {
            return BattleAI.RANDOM;
        }
        return ResourceKey.create(BattleAI.REGISTRY, location);
    }

    private @Nullable EnumOldGenMode gimmick(@Nullable String gimmick) {
        if (gimmick == null) {
            return null;
        }
        for (var mode : EnumOldGenMode.values()) {
            if (mode.name().equalsIgnoreCase(gimmick)) {
                return mode;
            }
        }
        return null;
    }

    private List<Pokemon> battleTeam(ServerPlayer player, @Nullable List<? extends UPokemon<?>> team, BattleSettings settings) {
        var members = team == null ? StorageProxy.getPartyNow(player).getTeam() : team.stream().map(PixelmonPokemon::unwrap).toList();
        var result = new ArrayList<Pokemon>();
        for (var pokemon : members) {
            if (result.size() < settings.teamSize()) {
                result.add(settings.cloneTeams() ? PokemonFactory.copy(pokemon) : pokemon);
            }
        }
        return result;
    }

    private BattleBuilder battleBuilder(BattleSettings settings) {
        var builder = BattleBuilder.builder()
                .registryAccess(ServerUtils.registryAccess())
                .startSync()
                .set(ContextKeys.FULL_HEAL, settings.fullHeal())
                .set(ContextKeys.TEAM_PREVIEW, settings.teamPreview())
                .setBattleType(switch (settings.format()) {
                    case SINGLES -> BattleType.SINGLE;
                    case DOUBLES -> BattleType.DOUBLE;
                    case TRIPLES -> BattleType.TRIPLE;
                });
        if (settings.hasLevelCap()) {
            builder.set(ContextKeys.LEVEL_CAP, settings.levelCap()).set(ContextKeys.RAISE_TO_CAP, settings.raiseToCap());
        }
        if (settings.turnTime() > 0) {
            builder.set(ContextKeys.TURN_TIME, settings.turnTime());
        }
        var gimmick = this.gimmick(settings.gimmick());
        if (gimmick != null) {
            builder.set(ContextKeys.GEN_MODE, gimmick);
        }
        if (!settings.experience()) {
            builder.disableExp();
        }
        for (var id : settings.clauses()) {
            if (this.battleClause(id) instanceof PixelmonBattleClause clause) {
                builder.withRule(clause.dereference().value());
            }
        }
        return builder;
    }

    private CompletableFuture<@Nullable UBattle<?>> start(BattleBuilder builder) {
        try {
            return builder.start()
                    .<@Nullable UBattle<?>>thenApply(battle -> battle == null ? null : new PixelmonBattle(battle))
                    .exceptionally(e -> null);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to start Pixelmon battle: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<PlayerPartyStorage> account(PlayerReference player) {
        return StorageProxy.getParty(player.uuid());
    }

    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.account(player).thenApply(PlayerPartyStorage::getBalance);
    }

    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.hasBalance(amount)));
    }

    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.take(amount), player, amount.negate()));
    }

    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.add(amount), player, amount));
    }
}
