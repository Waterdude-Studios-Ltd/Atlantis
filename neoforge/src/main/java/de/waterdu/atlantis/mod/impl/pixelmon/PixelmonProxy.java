package de.waterdu.atlantis.mod.impl.pixelmon;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.enums.TextJustification;
import com.pixelmonmod.pixelmon.items.UIElementItem;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.FileExtensions;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.event.NeoForgeEventBus;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.parsing.PixelmonPokemonSpec;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage.PixelmonPC;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage.PixelmonParty;
import de.waterdu.atlantis.mod.impl.util.ConfigurablePokemon;
import de.waterdu.atlantis.mod.impl.util.PokemonTypeAdapter;
import de.waterdu.atlantis.mod.impl.util.StoredPokemon;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemUtils;
import de.waterdu.atlantis.util.item.ProcessorUtils;
import de.waterdu.atlantis.util.java.collection.ArrayUtils;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.neoforged.bus.api.IEventBus;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PixelmonProxy implements PokemonDelegate, EconomyDelegate<PlayerPartyStorage> {

    @Override
    public void setup() {

    }

    @Override
    public void registerTypeAdapters(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(StoredPokemon.class, new StoredPokemon.TypeAdapter())
                .registerTypeAdapter(ConfigurablePokemon.class, new ConfigurablePokemon.TypeAdapter())
                .registerTypeAdapter(Pokemon.class, new PokemonTypeAdapter());
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
        return new ConfigurableItemStack.Preprocessor[]{
                new ConfigurableItemStack.Preprocessor((full, args) -> {
                    var specBuilder = new StringBuilder();
                    var argsBuilder = new StringBuilder();
                    var currentBuilder = specBuilder;
                    for (var i = 1; i < args.length; i++) {
                        if (!currentBuilder.isEmpty()) {
                            currentBuilder.append(" ");
                        }
                        var arg = args[i];
                        if (arg.equalsIgnoreCase(ConfigurableItemStack.Preprocessor.END_DIRECTIVE)) {
                            currentBuilder = argsBuilder;
                        } else {
                            currentBuilder.append(arg);
                        }
                    }
                    return Pair.of(
                            SpriteItemHelper.getPhoto(PokemonSpecificationProxy.create(specBuilder.toString().trim()).get().create()),
                            argsBuilder.toString().split(" ")
                    );
                }),
                new ConfigurableItemStack.Preprocessor((full, args) -> Pair.of(
                        PokeBallRegistry.getPokeBall(args[1]).orElse(PokeBallRegistry.POKE_BALL).getBallItem(),
                        ArrayUtils.coerceTail(args, new String[args.length - (args.length >= 3 && args[2].equalsIgnoreCase(ConfigurableItemStack.Preprocessor.END_DIRECTIVE) ? 3 : 2)])
                )),
                new ConfigurableItemStack.Preprocessor((full, args) -> Pair.of(
                        PokeBallRegistry.getPokeBall(args[1]).orElse(PokeBallRegistry.POKE_BALL).getLidItem(),
                        ArrayUtils.coerceTail(args, new String[args.length - (args.length >= 3 && args[2].equalsIgnoreCase(ConfigurableItemStack.Preprocessor.END_DIRECTIVE) ? 3 : 2)])
                ))
        };
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
        return new PixelmonParty(StorageProxy.getParty(player.uuid()));
    }

    @Override
    public PixelmonPC pc(PlayerReference player) {
        return new PixelmonPC(StorageProxy.getPCForPlayer(player.uuid()));
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
