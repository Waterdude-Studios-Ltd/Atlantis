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
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonTeraType;
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

/**
 * Implements {@link PokemonDelegate} and {@link EconomyDelegate} with Pixelmon. The economy accounts are the party
 * storages of the players.
 */
public class PixelmonProxy implements PokemonDelegate, EconomyDelegate<PlayerPartyStorage> {

    /**
     * Registers the Pixelmon event handlers on the Pixelmon event bus with {@link PixelmonEventBridge#register}.
     */
    @Override
    public void setup() {
        PixelmonEventBridge.register(Pixelmon.EVENT_BUS);
    }

    /**
     * Registers config serializers for Pixelmon species, moves, abilities and Poké Balls. A species is read from its
     * name or its national Pokédex number and is written as its name.
     *
     * @param gson the Gson builder to register the serializers with
     */
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

    /**
     * Gives the Pixelmon event bus, wrapped in a {@link NeoForgeEventBus}, to the consumer.
     *
     * @param consumer the consumer that gets the event bus
     */
    @Override
    public void registerEventBus(Consumer<UEventBus<?, ?>> consumer) {
        consumer.accept(new NeoForgeEventBus(Pixelmon.EVENT_BUS));
    }

    /**
     * Creates the default Pokémon item preprocessors, plus one that gets the lid item of a Poké Ball by name. An
     * unknown name gives the lid of the standard Poké Ball.
     *
     * @return the item preprocessors
     */
    @Override
    public ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return ArrayUtils.expand(
                PokemonDelegate.super.makeItemPreprocessors(),
                ConfigurableItemStack.Preprocessor.single(name ->
                        PokeBallRegistry.getPokeBall(name).orElse(PokeBallRegistry.POKE_BALL).getLidItem()
                )
        );
    }

    /**
     * Creates the item processors that write the {@link UIElementItem} tags, such as the image, text, offsets, size,
     * scale, rotation and colors. Image names get the {@code .png} extension if they do not have it.
     *
     * @return the item processors
     */
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

    /**
     * Gets the party of the player from the Pixelmon storage.
     *
     * @param player the player
     * @return the wrapped party
     */
    @Override
    public PixelmonParty party(PlayerReference player) {
        return new PixelmonParty(player.uuid(), StorageProxy.getParty(player.uuid()));
    }

    /**
     * Gets the PC of the player from the Pixelmon storage.
     *
     * @param player the player
     * @return the wrapped PC
     */
    @Override
    public PixelmonPC pc(PlayerReference player) {
        return new PixelmonPC(player.uuid(), StorageProxy.getPCForPlayer(player.uuid()));
    }

    /**
     * Gets the Pokédex of the player from the Pixelmon Pokédex storage.
     *
     * @param player the player
     * @return the wrapped Pokédex
     */
    @Override
    public PixelmonPokedex pokedex(PlayerReference player) {
        return new PixelmonPokedex(PokeDexStorageProxy.getStorage(player.uuid()));
    }

    /**
     * Checks if the player picked a starter, from the Pixelmon party of the player.
     *
     * @param player the player
     * @return {@code true} if the player picked a starter
     */
    @Override
    public boolean hasChosenStarter(PlayerReference player) {
        return StorageProxy.getPartyNow(player.uuid()).starterPicked;
    }

    /**
     * Sets if the player picked a starter, in the Pixelmon party of the player. Marks the party to be saved.
     *
     * @param player the player
     * @param chosen {@code true} to mark the starter as picked
     */
    @Override
    public void setChosenStarter(PlayerReference player, boolean chosen) {
        var party = StorageProxy.getPartyNow(player.uuid());
        party.starterPicked = chosen;
        party.setNeedsSaving();
    }

    /**
     * Parses the specs as a Pixelmon Pokémon specification.
     *
     * @param specs the parts of the spec
     * @return the wrapped spec
     */
    @Override
    public PixelmonPokemonSpec spec(String... specs) {
        return new PixelmonPokemonSpec(PokemonSpecificationProxy.create(specs).get());
    }

    /**
     * @return {@code :}, the separator between a key and its value in a Pixelmon spec
     */
    @Override
    public String specDelimiter() {
        return ":";
    }

    /**
     * Gets a Pixelmon species by name.
     *
     * @param name the name of the species
     * @return the species, or MissingNo. if no species has the name
     */
    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies species(String name) {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(
                PixelmonSpecies.fromName(name).orElse(PixelmonSpecies.MISSINGNO.getValueUnsafe())
        );
    }

    /**
     * Gets a Pixelmon species by its Pokédex number.
     *
     * @param dex the Pokédex number
     * @return the species, or MissingNo. if no species has the number
     */
    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies species(int dex) {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(
                PixelmonSpecies.fromDex(dex).orElse(PixelmonSpecies.MISSINGNO.getValueUnsafe())
        );
    }

    /**
     * Loads a Pokémon from NBT. If the tag is not valid, logs an error and creates a MissingNo.
     *
     * @param tag the NBT of the Pokémon
     * @return the loaded Pokémon, or a MissingNo. Pokémon
     */
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

    /**
     * Loads a Pokémon from a JSON string that holds SNBT. If the SNBT is not valid, logs an error and creates a
     * MissingNo.
     *
     * @param json the JSON string with the SNBT of the Pokémon
     * @return the loaded Pokémon, or a MissingNo. Pokémon
     */
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

    /**
     * Checks if a Pixelmon species has the name.
     *
     * @param name the name of the species
     * @return {@code true} if a species has the name
     */
    @Override
    public boolean hasSpecies(String name) {
        return PixelmonSpecies.has(name);
    }

    /**
     * Gets every Pixelmon species.
     *
     * @return the species
     */
    @Override
    public List<de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies> allSpecies() {
        return PixelmonSpecies.getAll().stream().map(de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies::new).toList();
    }

    /**
     * Gets a random species from Pixelmon.
     *
     * @return the random species
     */
    @Override
    public de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies randomSpecies() {
        return new de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies(PixelmonSpecies.getRandomSpecies());
    }

    /**
     * Gets an ability by name.
     *
     * @param name the name of the ability
     * @return the ability, or {@code null} if no ability has the name
     */
    @Override
    public @Nullable PixelmonAbility ability(String name) {
        return AbilityRegistry.getAbility(name).map(PixelmonAbility::new).orElse(null);
    }

    /**
     * Gets every Pixelmon ability.
     *
     * @return the abilities
     */
    @Override
    public List<PixelmonAbility> abilities() {
        return AbilityRegistry.getAllAbilities().stream().map(PixelmonAbility::new).toList();
    }

    /**
     * Gets a move by name.
     *
     * @param name the name of the move
     * @return the move, or {@code null} if no move has the name
     */
    @Override
    public @Nullable PixelmonMoveTemplate move(String name) {
        return AttackRegistry.getAttackBase(name).map(PixelmonMoveTemplate::new).orElse(null);
    }

    /**
     * Gets every Pixelmon move.
     *
     * @return the moves
     */
    @Override
    public List<PixelmonMoveTemplate> moves() {
        return AttackRegistry.getAllAttacks().stream().map(PixelmonMoveTemplate::new).toList();
    }

    /**
     * Gets a Poké Ball by name.
     *
     * @param name the name of the Poké Ball
     * @return the Poké Ball, or {@code null} if no Poké Ball has the name
     */
    @Override
    public @Nullable PixelmonPokeBall pokeBall(String name) {
        return PokeBallRegistry.getPokeBall(name).getValue().map(PixelmonPokeBall::new).orElse(null);
    }

    /**
     * Gets every Pixelmon Poké Ball.
     *
     * @return the Poké Balls
     */
    @Override
    public List<PixelmonPokeBall> pokeBalls() {
        return PokeBallRegistry.getAll().stream().map(PixelmonPokeBall::new).toList();
    }

    /**
     * Gets an elemental type by name from the Pixelmon type registry. The name is trimmed and put in lower case.
     *
     * @param name the name of the type
     * @return the type, or {@code null} if no type has the name
     */
    @Override
    public @Nullable PixelmonElementalType elementalType(String name) {
        var key = Type.parseOrNull(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).getHolder(key).map(PixelmonElementalType::new).orElse(null);
    }

    /**
     * Gets every type in the Pixelmon type registry.
     *
     * @return the elemental types
     */
    @Override
    public List<PixelmonElementalType> elementalTypes() {
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).holders().map(PixelmonElementalType::new).toList();
    }

    /**
     * Gets a Tera type by name from the Pixelmon type registry. The name is trimmed and put in lower case.
     *
     * @param name the name of the Tera type
     * @return the Tera type, or {@code null} if no type has the name
     */
    @Override
    public @Nullable PixelmonTeraType teraType(String name) {
        var key = Type.parseOrNull(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).getHolder(key).map(PixelmonTeraType::new).orElse(null);
    }

    /**
     * Gets a Tera type for every type in the Pixelmon type registry.
     *
     * @return the Tera types
     */
    @Override
    public List<PixelmonTeraType> teraTypes() {
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.TYPE_REGISTRY).holders().map(PixelmonTeraType::new).toList();
    }

    /**
     * Gets a stat by name with {@link PixelmonStat#of}.
     *
     * @param name the name of the stat
     * @return the stat, or {@code null} if no stat has the name
     */
    @Override
    public @Nullable PixelmonStat stat(String name) {
        return PixelmonStat.of(name);
    }

    /**
     * Gets every Pixelmon stat.
     *
     * @return the stats
     */
    @Override
    public List<PixelmonStat> stats() {
        return PixelmonStat.all();
    }

    /**
     * Gets the stats that are permanent.
     *
     * @return the stats for which {@link PixelmonStat#isPermanent} is {@code true}
     */
    @Override
    public List<PixelmonStat> permanentStats() {
        return PixelmonStat.all().stream().filter(PixelmonStat::isPermanent).toList();
    }

    /**
     * Gets a nature by name.
     *
     * @param name the name of the nature
     * @return the nature, or {@code null} if no nature has the name
     */
    @Override
    public @Nullable PixelmonNature nature(String name) {
        return Nature.hasNature(name) ? new PixelmonNature(Nature.natureFromString(name)) : null;
    }

    /**
     * Gets every Pixelmon nature.
     *
     * @return the natures
     */
    @Override
    public List<PixelmonNature> natures() {
        return Arrays.stream(Nature.values()).map(PixelmonNature::new).toList();
    }

    /**
     * Gets a random nature from Pixelmon.
     *
     * @return the random nature
     */
    @Override
    public PixelmonNature randomNature() {
        return new PixelmonNature(Nature.getRandomNature());
    }

    /**
     * Gets a gender by name, ignoring case.
     *
     * @param name the name of the gender
     * @return the gender, or {@code null} if no gender has the name
     */
    @Override
    public @Nullable PixelmonGender gender(String name) {
        for (var gender : this.genders()) {
            if (gender.name().equalsIgnoreCase(name)) {
                return gender;
            }
        }
        return null;
    }

    /**
     * Gets every Pixelmon gender.
     *
     * @return the genders
     */
    @Override
    public List<PixelmonGender> genders() {
        return Arrays.stream(Gender.values()).map(PixelmonGender::new).toList();
    }

    /**
     * Gets a status by name, ignoring case.
     *
     * @param name the name of the status
     * @return the status, or {@code null} if no status has the name
     */
    @Override
    public @Nullable PixelmonStatus status(String name) {
        for (var status : PixelmonStatus.all()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Gets every status from {@link PixelmonStatus#all()}.
     *
     * @return the statuses
     */
    @Override
    public List<PixelmonStatus> statuses() {
        return PixelmonStatus.all();
    }

    /**
     * Gets a move category by name.
     *
     * @param name the name of the category
     * @return the category, or {@code null} if no category has the name
     */
    @Override
    public @Nullable PixelmonMoveCategory moveCategory(String name) {
        var category = AttackCategory.parseOrNull(name);
        return category == null ? null : new PixelmonMoveCategory(category);
    }

    /**
     * Gets every Pixelmon move category.
     *
     * @return the move categories
     */
    @Override
    public List<PixelmonMoveCategory> moveCategories() {
        return Arrays.stream(AttackCategory.values()).map(PixelmonMoveCategory::new).toList();
    }

    /**
     * Wraps the entity if it is a Pixelmon entity.
     *
     * @param entity the entity
     * @return the wrapped entity, or {@code null} if the entity is not a Pokémon
     */
    @Override
    public @Nullable PixelmonPokemonEntity pokemonEntity(Entity entity) {
        return entity instanceof PixelmonEntity pixelmon ? new PixelmonPokemonEntity(pixelmon) : null;
    }

    /**
     * Gets the battle that the player takes part in, from the Pixelmon battle registry.
     *
     * @param player the player
     * @return the battle, or {@code null} if the player is not in a battle
     */
    @Override
    public @Nullable PixelmonBattle battle(ServerPlayer player) {
        var battle = BattleRegistry.getBattle(player);
        return battle == null ? null : new PixelmonBattle(battle);
    }

    /**
     * Starts a battle between the player and the wild Pokémon.
     *
     * @param player the player
     * @param wild the wild Pokémon entity. It must be a Pixelmon entity.
     * @return the battle, or {@code null} if the battle did not start
     */
    @Override
    public CompletableFuture<@Nullable UBattle<?>> startWildBattle(ServerPlayer player, UPokemonEntity<?> wild) {
        return this.start(BattleBuilder.builder().registryAccess(ServerUtils.registryAccess()).teamOne(new PlayerParticipant(player)).teamTwo(new WildPixelmonParticipant((PixelmonEntity) wild.dereference())));
    }

    /**
     * Starts a battle between two players. If the settings have a team select time and neither team is given, the
     * players select their teams first, and they cannot close the selection.
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
        var builder = this.battleBuilder(settings)
                .teamOne(new PlayerParticipant(player1, this.battleTeam(player1, team1, settings)))
                .teamTwo(new PlayerParticipant(player2, this.battleTeam(player2, team2, settings)));
        if (settings.teamSelectTime() > 0 && team1 == null && team2 == null) {
            builder.teamSelection(true).teamSelectionBuilder(TeamSelectionRegistry.builder().notCloseable());
        }
        return this.start(builder);
    }

    /**
     * Starts a battle between the player and the trainer. The trainer battles with healed copies of its party, trimmed
     * to the team size. Sets the boss tier, the AI and the Mega and Dynamax gimmicks of the trainer from the settings.
     *
     * @param player the player
     * @param team the team of the player, or {@code null} for their party
     * @param trainer the trainer
     * @param trainerSettings the trainer settings
     * @param settings the battle settings
     * @return the battle, or {@code null} if the trainer is not a Pixelmon trainer, is in a battle, has no Pokémon, or
     *     the battle did not start
     */
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

    /**
     * Spawns a persistent NPC with a player model. The NPC cannot move, be pushed or take damage, shows its name, and
     * has no interactions or tracking.
     *
     * @param level the level to spawn in
     * @param position the position of the NPC
     * @param yaw the yaw of the body and the head
     * @param name the name of the NPC
     * @return the spawned trainer
     */
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

    /**
     * Wraps the entity if it is a Pixelmon NPC.
     *
     * @param entity the entity
     * @return the wrapped trainer, or {@code null} if the entity is not an NPC
     */
    @Override
    public @Nullable PixelmonTrainer trainer(Entity entity) {
        return entity instanceof NPC npc ? new PixelmonTrainer(npc) : null;
    }

    /**
     * Checks if Pixelmon supports the feature. Pixelmon supports every feature.
     *
     * @param feature the feature to check
     * @return {@code true}
     */
    @Override
    public boolean supports(PokemonFeature feature) {
        return true;
    }

    /**
     * @return a clause for every rule in the Pixelmon battle rule registry
     */
    @Override
    public List<PixelmonBattleClause> battleClauses() {
        return ServerUtils.registryAccess().registryOrThrow(PixelmonRegistry.BATTLE_RULE_REGISTRY).holders().map(PixelmonBattleClause::new).toList();
    }

    /**
     * @return the IDs of the battle AIs in the registry. IDs in the Pixelmon namespace have no namespace.
     */
    @Override
    public List<String> trainerAIs() {
        return ServerUtils.registryAccess().registryOrThrow(BattleAI.REGISTRY).holders().map(holder -> this.shortId(holder.key().location())).toList();
    }

    /**
     * @return the IDs of the Pixelmon boss tiers
     */
    @Override
    public List<String> bossTiers() {
        return BossTierRegistry.getBossTiers().stream().map(BossTier::getID).toList();
    }

    /**
     * @return the names of the {@link EnumOldGenMode} values, in lower case
     */
    @Override
    public List<String> battleGimmicks() {
        return Arrays.stream(EnumOldGenMode.values()).map(mode -> mode.name().toLowerCase(Locale.ROOT)).toList();
    }

    /**
     * Finds a held item by its Showdown name, first in the Pixelmon namespace, then in the Minecraft namespace.
     *
     * @param name the Showdown name of the item
     * @return the item
     */
    @Override
    public ItemStack showdownItem(String name) {
        return ShowdownFormat.item(name, Pixelmon.MODID, "minecraft");
    }

    /**
     * Gets the items stored inside an item. Adds the items in a Pixelmon Poké Bag to the default items.
     *
     * @param stack the item that holds the items
     * @return the items that are not empty
     */
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

    /**
     * Gets the ID of the location without the namespace if the namespace is Pixelmon.
     *
     * @param location the resource location
     * @return the path for the Pixelmon namespace, otherwise the full location
     */
    private String shortId(ResourceLocation location) {
        return location.getNamespace().equals(Pixelmon.MODID) ? location.getPath() : location.toString();
    }

    /**
     * Gets the registry key of a battle AI. An ID without a namespace uses the Pixelmon namespace and is put in lower
     * case.
     *
     * @param ai the ID of the battle AI, or {@code null} for none
     * @return the key, or {@link BattleAI#RANDOM} if the ID is {@code null}, blank, not valid or not registered
     */
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

    /**
     * Gets the {@link EnumOldGenMode} with the name, ignoring case.
     *
     * @param gimmick the name of the gimmick, or {@code null} for none
     * @return the mode, or {@code null} if the name is {@code null} or no mode has the name
     */
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

    /**
     * Gets the Pokémon that a player battles with, trimmed to the team size. If the settings clone the teams, the
     * Pokémon are copies.
     *
     * @param player the player
     * @param team the team of the player, or {@code null} for the team of their party
     * @param settings the battle settings
     * @return the Pokémon of the team
     */
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

    /**
     * Creates a battle builder that starts in sync and applies the settings. These are the full heal, team preview,
     * format, level cap, turn time, gimmick, experience and battle clauses.
     *
     * @param settings the battle settings
     * @return the battle builder, without participants
     */
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

    /**
     * Starts the battle and wraps it. If the start throws, logs an error.
     *
     * @param builder the battle builder
     * @return the battle, or {@code null} if the battle did not start
     */
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

    /**
     * Gets the Pixelmon party of the player, which holds the balance.
     *
     * @param player the player to get the account of
     * @return the party storage
     */
    @Override
    public CompletableFuture<PlayerPartyStorage> account(PlayerReference player) {
        return StorageProxy.getParty(player.uuid());
    }

    /**
     * Gets the balance of the player from their Pixelmon party.
     *
     * @param player the player to get the balance of
     * @return the balance
     */
    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.account(player).thenApply(PlayerPartyStorage::getBalance);
    }

    /**
     * Checks if the Pixelmon party of the player has at least the amount.
     *
     * @param player the player
     * @param amount the amount to check
     * @return a successful result if the player has at least the amount. The result records no transactions.
     */
    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.hasBalance(amount)));
    }

    /**
     * Takes the amount from the Pixelmon party of the player.
     *
     * @param player the player to withdraw from
     * @param amount the amount to withdraw
     * @return on success, the result records the withdrawal as a negative change
     */
    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.take(amount), player, amount.negate()));
    }

    /**
     * Adds the amount to the Pixelmon party of the player.
     *
     * @param player the player to deposit to
     * @param amount the amount to deposit
     * @return on success, the result records the deposit as a positive change
     */
    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.add(amount), player, amount));
    }
}
