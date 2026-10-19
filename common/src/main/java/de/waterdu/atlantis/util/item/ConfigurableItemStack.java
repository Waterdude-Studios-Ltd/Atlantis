package de.waterdu.atlantis.util.item;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Valid examples:
 * - minecraft:diamond
 * - minecraft:diamond count:3 damage:1 enchantment:knockback-3 glow {components}
 * - minecraft:diamond x3 #1 {components}
 * - minecraft:diamond 5
 * - minecraft:diamond {components}
 * - #sprite Pikachu gender:male shiny #end glow {components}
 * - #pokeball master_ball #end 10
 */
public class ConfigurableItemStack {

    private final String string;

    private ItemStack stack = null;
    private Item item = null;
    private Block block = null;

    protected ConfigurableItemStack(String string) {
        this.string = string;
    }

    public static ConfigurableItemStack empty() {
        return ConfigurableItemStack.of(Items.AIR);
    }

    public static ConfigurableItemStack of(String string) {
        return new ConfigurableItemStack(string);
    }

    public static ConfigurableItemStack of(ResourceLocation resourceLocation) {
        return new ConfigurableItemStack(resourceLocation.toString());
    }

    public static ConfigurableItemStack of(BlockState blockState) {
        return ConfigurableItemStack.of(blockState.getBlock());
    }

    public static ConfigurableItemStack of(Block block) {
        return ConfigurableItemStack.of(block.asItem());
    }

    public static ConfigurableItemStack of(Item item) {
        return ConfigurableItemStack.of(item.id().toString());
    }

    public static ConfigurableItemStack of(ItemLike like) {
        return ConfigurableItemStack.of(like.asItem());
    }

    public static ConfigurableItemStack of(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        builder.append(stack.getItem().id());
        if (stack.getCount() > 1) {
            builder.append(" ").append("count:").append(stack.getCount());
        }
        if (stack.isDamaged()) {
            builder.append(" ").append("damage:").append(stack.getDamageValue());
        }
        if (!stack.getComponents().isEmpty()) {
            builder.append(" ").append(DataComponentEncoder.TAG.encode(stack.getComponents()).toString());
        }
        return ConfigurableItemStack.of(builder.toString());
    }

    public static ConfigurableItemStack of(ConfigurableItemStack stack) {
        return ConfigurableItemStack.of(stack.string);
    }

    public String originalString() {
        return this.string;
    }

    public ConfigurableItemStack copy() {
        return ConfigurableItemStack.of(this);
    }

    public ItemStack stack() {
        if (this.stack == null) {
            try {
                var full = Settings.get().getParsedItemStack(this.string);
                var args = this.prepareArgs(full);
                var preprocessor = Preprocessor.get(args);
                if (preprocessor != null) {
                    var result = preprocessor.execute(full, args);
                    this.stack = result.getA();
                    this.parse(0, result.getB());
                } else {
                    this.stack = new ItemStack(ItemUtils.item(args[0]).orElse(Items.AIR));
                    this.parse(1, args);
                }
            } catch (CommandSyntaxException e) {
                AtlantisLogger.error("Failed to parse ConfigurableItemStack: {}", this.string);
            }
        }
        return this.stack;
    }

    private String[] prepareArgs(String string) {
        var brace = string.indexOf('{');
        if (brace < 0) {
            return string.split(" ");
        }
        return (string.substring(0, brace) + " " + string.substring(brace)).split(" ");
    }

    private void parse(int start, String... args) throws CommandSyntaxException {
        var curlyCount = 0;
        var tag = new StringBuilder();

        for (var i = start; i < args.length; i++) {
            var arg = args[i];
            if (arg == null || arg.isEmpty()) {
                continue;
            }
            if (arg.startsWith("{")) {
                curlyCount = 1;
            }
            if (curlyCount > 0) {
                if (!tag.isEmpty()) {
                    tag.append(" ");
                }
                tag.append(arg);
                curlyCount += TextUtils.countCharOccurrenceDifference(arg, '{', '}');
                if (curlyCount == 1) {
                    this.stack.applyComponents(DataComponentEncoder.TAG.decode(TagParser.parseTag(tag.toString())));
                    curlyCount = 0;
                }
            } else {
                this.parseProcessor(arg);
            }
        }
    }

    private void parseProcessor(String arg) {
        if (arg.startsWith("x")) {
            Processor.COUNT.execute(new String[]{Processor.COUNT_IDENTIFIER, arg.substring(1)}, this.stack);
        } else if (TextUtils.isNumber(arg)) {
            Processor.COUNT.execute(new String[]{Processor.COUNT_IDENTIFIER, arg}, this.stack);
        } else if (arg.startsWith("#")) {
            Processor.DAMAGE.execute(new String[]{Processor.DAMAGE_IDENTIFIER, arg.substring(1)}, this.stack);
        } else {
            Processor.getAndExecute(arg.split(":"), this.stack);
        }
    }

    public Item item() {
        if (this.item == null) {
            this.item = this.stack().getItem();
        }
        return this.item;
    }

    public Block block() {
        if (this.block == null) {
            this.block = Block.byItem(this.item());
        }
        return this.block;
    }

    public ItemStack newStack() {
        return this.stack().copy();
    }

    public ItemStack newStack(int count) {
        ItemStack newStack = this.stack().copy();
        newStack.setCount(count);
        return newStack;
    }

    public boolean giveOrElse(Player player, Consumer<ItemStack> orElse) {
        return ItemUtils.giveOrElse(player, this, orElse);
    }

    public Optional<ItemEntity> drop(LevelPosition position, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(this, position, modifier);
    }

    public Optional<ItemEntity> drop(LevelPosition position, @Nullable Player owner, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(this, position, owner, modifier);
    }

    public boolean giveOrDrop(Player player) {
        return ItemUtils.giveOrDrop(player, this);
    }

    public boolean giveOrDrop(Player player, Consumer<ItemStack> onGive, Consumer<ItemEntity> onDrop) {
        return ItemUtils.giveOrDrop(player, this, onGive, onDrop);
    }

    public boolean matches(Object obj) {
        if (this.equals(obj)) {
            return true;
        } else if (obj instanceof Item) {
            return this.item().equals(obj);
        } else if ((obj instanceof ItemStack other)) {
            return ItemStack.matches(this.stack(), other);
        } else if (obj instanceof String str) {
            return this.string.equalsIgnoreCase(str) || this.toString().equalsIgnoreCase(str);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.string.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigurableItemStack)) {
            return false;
        }
        return this.string.equalsIgnoreCase(((ConfigurableItemStack) obj).string);
    }

    @Override
    public String toString() {
        return "ConfigurableItemStack(" + this.string + ")";
    }

    public record Preprocessor(BiFunction<String, String[], Pair<ItemStack, String[]>> process) {

        private static final Map<String, Preprocessor> PREPROCESSORS = new HashMap<>();

        public static final String SPRITE_DIRECTIVE = "#sprite";
        public static final String POKEBALL_DIRECTIVE = "#pokeball";
        public static final String POKEBALL_LID_DIRECTIVE = "#pokeball_lid";
        public static final String END_DIRECTIVE = "#end";

        public static final Preprocessor SPRITE;
        public static final Preprocessor POKEBALL;
        public static final Preprocessor POKEBALL_LID;

        static {
            var preprocessors = Mods.isPresent(PokemonDelegate.class) ? Mods.pokemon().makeItemPreprocessors() : new Preprocessor[0];
            if (preprocessors.length > 0) {
                SPRITE = register(SPRITE_DIRECTIVE, preprocessors[0]);
                POKEBALL = register(POKEBALL_DIRECTIVE, preprocessors[1]);
                POKEBALL_LID = register(POKEBALL_LID_DIRECTIVE, preprocessors[2]);
            } else {
                SPRITE = null;
                POKEBALL = null;
                POKEBALL_LID = null;
            }
        }

        private static Preprocessor register(String directive, Preprocessor preprocessor) {
            PREPROCESSORS.put(directive, preprocessor);
            return preprocessor;
        }

        public static Preprocessor register(String directive, BiFunction<String, String[], Pair<ItemStack, String[]>> process) {
            return register(directive, new Preprocessor(process));
        }

        private Pair<ItemStack, String[]> execute(String full, String[] args) {
            return this.process.apply(full, args);
        }

        private static Preprocessor get(String[] args) {
            return PREPROCESSORS.get(args[0].toLowerCase(Locale.ROOT));
        }
    }

    public record Processor(BiConsumer<String[], ItemStack> process) {

        private static final Map<String, Processor> PROCESSORS = new HashMap<>();

        public static final String COUNT_IDENTIFIER = "count";
        public static final String DAMAGE_IDENTIFIER = "damage";
        public static final String ENCHANTMENT_IDENTIFIER = "enchantment";
        public static final String GLOW_IDENTIFIER = "glow";
        public static final String TOOLTIP_IDENTIFIER = "tooltip";
        public static final String NAME_IDENTIFIER = "name";
        public static final String MODEL_IDENTIFIER = "model";
        public static final String LORE_IDENTIFIER = "lore";

        public static final String UI_IMAGE_IDENTIFIER = "image";
        public static final String UI_HOVER_IMAGE_IDENTIFIER = "hover_image";
        public static final String UI_TEXT_IDENTIFIER = "text";
        public static final String UI_HOVER_TEXT_IDENTIFIER = "hover_text";
        public static final String UI_TEXT_JUSTIFICATION_IDENTIFIER = "text_justification";
        public static final String UI_TEXT_SCALE_IDENTIFIER = "text_scale";
        public static final String UI_X_OFFSET_IDENTIFIER = "x_offset";
        public static final String UI_Y_OFFSET_IDENTIFIER = "y_offset";
        public static final String UI_OFFSET_IDENTIFIER = "offset";
        public static final String UI_X_OVERRIDE_IDENTIFIER = "x_override";
        public static final String UI_Y_OVERRIDE_IDENTIFIER = "y_override";
        public static final String UI_OVERRIDE_IDENTIFIER = "override";
        public static final String UI_Z_LEVEL_IDENTIFIER = "z_level";
        public static final String UI_IMAGE_WIDTH_IDENTIFIER = "image_width";
        public static final String UI_IMAGE_HEIGHT_IDENTIFIER = "image_height";
        public static final String UI_IMAGE_SIZE_IDENTIFIER = "image_size";
        public static final String UI_IMAGE_U_IDENTIFIER = "u";
        public static final String UI_IMAGE_V_IDENTIFIER = "v";
        public static final String UI_IMAGE_UV_IDENTIFIER = "uv";
        public static final String UI_SCALE_X_IDENTIFIER = "scale_x";
        public static final String UI_SCALE_Y_IDENTIFIER = "scale_y";
        public static final String UI_SCALE_IDENTIFIER = "scale";
        public static final String UI_ROTATION_IDENTIFIER = "rotation";
        public static final String UI_RED_IDENTIFIER = "red";
        public static final String UI_GREEN_IDENTIFIER = "green";
        public static final String UI_BLUE_IDENTIFIER = "blue";
        public static final String UI_ALPHA_IDENTIFIER = "alpha";
        public static final String UI_COLOR_IDENTIFIER = "color";
        public static final String UI_HOVER_RED_IDENTIFIER = "hover_red";
        public static final String UI_HOVER_GREEN_IDENTIFIER = "hover_green";
        public static final String UI_HOVER_BLUE_IDENTIFIER = "hover_blue";
        public static final String UI_HOVER_ALPHA_IDENTIFIER = "hover_alpha";
        public static final String UI_HOVER_COLOR_IDENTIFIER = "hover_color";

        public static final Processor COUNT;
        public static final Processor DAMAGE;
        public static final Processor ENCHANTMENT;
        public static final Processor GLOW;
        public static final Processor TOOLTIP;
        public static final Processor NAME;
        public static final Processor MODEL;
        public static final Processor LORE;

        public static final Processor UI_IMAGE;
        public static final Processor UI_HOVER_IMAGE;
        public static final Processor UI_TEXT;
        public static final Processor UI_HOVER_TEXT;
        public static final Processor UI_TEXT_JUSTIFICATION;
        public static final Processor UI_TEXT_SCALE;
        public static final Processor UI_X_OFFSET;
        public static final Processor UI_Y_OFFSET;
        public static final Processor UI_OFFSET;
        public static final Processor UI_X_OVERRIDE;
        public static final Processor UI_Y_OVERRIDE;
        public static final Processor UI_OVERRIDE;
        public static final Processor UI_Z_LEVEL;
        public static final Processor UI_IMAGE_WIDTH;
        public static final Processor UI_IMAGE_HEIGHT;
        public static final Processor UI_IMAGE_SIZE;
        public static final Processor UI_IMAGE_U;
        public static final Processor UI_IMAGE_V;
        public static final Processor UI_IMAGE_UV;
        public static final Processor UI_SCALE_X;
        public static final Processor UI_SCALE_Y;
        public static final Processor UI_SCALE;
        public static final Processor UI_ROTATION;
        public static final Processor UI_RED;
        public static final Processor UI_GREEN;
        public static final Processor UI_BLUE;
        public static final Processor UI_ALPHA;
        public static final Processor UI_COLOR;
        public static final Processor UI_HOVER_RED;
        public static final Processor UI_HOVER_GREEN;
        public static final Processor UI_HOVER_BLUE;
        public static final Processor UI_HOVER_ALPHA;
        public static final Processor UI_HOVER_COLOR;

        static {
            COUNT = register(COUNT_IDENTIFIER, (kv, s) -> s.setCount(Integer.parseInt(kv[1])));
            DAMAGE = register(DAMAGE_IDENTIFIER, (kv, s) -> s.setDamageValue(Integer.parseInt(kv[1])));
            ENCHANTMENT = register(ENCHANTMENT_IDENTIFIER, (kv, s) -> {
                var value = String.join(":", Arrays.copyOfRange(kv, 1, kv.length)).toLowerCase(Locale.ROOT);
                var id = value;
                var level = 1;
                var separator = value.lastIndexOf('-');
                if (separator > 0 && TextUtils.isNumber(value.substring(separator + 1))) {
                    id = value.substring(0, separator);
                    level = Integer.parseInt(value.substring(separator + 1));
                }
                var location = ResourceLocation.tryParse(id);
                if (location != null) {
                    var tier = level;
                    Registries.ENCHANTMENT.registryFromRegistryKey().getHolder(location).ifPresent(holder -> ItemUtils.addEnchantment(s, holder, tier));
                }
            });
            GLOW = register(GLOW_IDENTIFIER, (kv, s) -> ItemUtils.applyGlow(s));
            TOOLTIP = register(TOOLTIP_IDENTIFIER, (kv, s) -> ItemUtils.setPixelmonTooltip(s, kv[1].replace("_", " ")));
            NAME = register(NAME_IDENTIFIER, (kv, s) -> ItemUtils.setDisplayName(s, kv[1].replace("_", " ")));
            MODEL = register(MODEL_IDENTIFIER, (kv, s) -> ItemUtils.setCustomModelData(s, Integer.parseInt(kv[1])));
            LORE = register(LORE_IDENTIFIER, (kv, s) -> ItemUtils.addDisplayLore(s, kv[1]));

            var processors = Mods.isPresent(PokemonDelegate.class) ? Mods.pokemon().makeItemProcessors() : new Processor[0];
            if (processors.length > 0) {
                UI_IMAGE = register(UI_IMAGE_IDENTIFIER, processors[0]);
                UI_HOVER_IMAGE = register(UI_HOVER_IMAGE_IDENTIFIER, processors[1]);
                UI_TEXT = register(UI_TEXT_IDENTIFIER, processors[2]);
                UI_HOVER_TEXT = register(UI_HOVER_TEXT_IDENTIFIER, processors[3]);
                UI_TEXT_JUSTIFICATION = register(UI_TEXT_JUSTIFICATION_IDENTIFIER, processors[4]);
                UI_TEXT_SCALE = register(UI_TEXT_SCALE_IDENTIFIER, processors[5]);
                UI_X_OFFSET = register(UI_X_OFFSET_IDENTIFIER, processors[6]);
                UI_Y_OFFSET = register(UI_Y_OFFSET_IDENTIFIER, processors[7]);
                UI_OFFSET = register(UI_OFFSET_IDENTIFIER, processors[8]);
                UI_X_OVERRIDE = register(UI_X_OVERRIDE_IDENTIFIER, processors[9]);
                UI_Y_OVERRIDE = register(UI_Y_OVERRIDE_IDENTIFIER, processors[10]);
                UI_OVERRIDE = register(UI_OVERRIDE_IDENTIFIER, processors[11]);
                UI_Z_LEVEL = register(UI_Z_LEVEL_IDENTIFIER, processors[12]);
                UI_IMAGE_WIDTH = register(UI_IMAGE_WIDTH_IDENTIFIER, processors[13]);
                UI_IMAGE_HEIGHT = register(UI_IMAGE_HEIGHT_IDENTIFIER, processors[14]);
                UI_IMAGE_SIZE = register(UI_IMAGE_SIZE_IDENTIFIER, processors[15]);
                UI_IMAGE_U = register(UI_IMAGE_U_IDENTIFIER, processors[16]);
                UI_IMAGE_V = register(UI_IMAGE_V_IDENTIFIER, processors[17]);
                UI_IMAGE_UV = register(UI_IMAGE_UV_IDENTIFIER, processors[18]);
                UI_SCALE_X = register(UI_SCALE_X_IDENTIFIER, processors[19]);
                UI_SCALE_Y = register(UI_SCALE_Y_IDENTIFIER, processors[20]);
                UI_SCALE = register(UI_SCALE_IDENTIFIER, processors[21]);
                UI_ROTATION = register(UI_ROTATION_IDENTIFIER, processors[22]);
                UI_RED = register(UI_RED_IDENTIFIER, processors[23]);
                UI_GREEN = register(UI_GREEN_IDENTIFIER, processors[24]);
                UI_BLUE = register(UI_BLUE_IDENTIFIER, processors[25]);
                UI_ALPHA = register(UI_ALPHA_IDENTIFIER, processors[26]);
                UI_COLOR = register(UI_COLOR_IDENTIFIER, processors[27]);
                UI_HOVER_RED = register(UI_HOVER_RED_IDENTIFIER, processors[28]);
                UI_HOVER_GREEN = register(UI_HOVER_GREEN_IDENTIFIER, processors[29]);
                UI_HOVER_BLUE = register(UI_HOVER_BLUE_IDENTIFIER, processors[30]);
                UI_HOVER_ALPHA = register(UI_HOVER_ALPHA_IDENTIFIER, processors[31]);
                UI_HOVER_COLOR = register(UI_HOVER_COLOR_IDENTIFIER, processors[32]);
            } else {
                UI_IMAGE = null;
                UI_HOVER_IMAGE = null;
                UI_TEXT = null;
                UI_HOVER_TEXT = null;
                UI_TEXT_JUSTIFICATION = null;
                UI_TEXT_SCALE = null;
                UI_X_OFFSET = null;
                UI_Y_OFFSET = null;
                UI_OFFSET = null;
                UI_X_OVERRIDE = null;
                UI_Y_OVERRIDE = null;
                UI_OVERRIDE = null;
                UI_Z_LEVEL = null;
                UI_IMAGE_WIDTH = null;
                UI_IMAGE_HEIGHT = null;
                UI_IMAGE_SIZE = null;
                UI_IMAGE_U = null;
                UI_IMAGE_V = null;
                UI_IMAGE_UV = null;
                UI_SCALE_X = null;
                UI_SCALE_Y = null;
                UI_SCALE = null;
                UI_ROTATION = null;
                UI_RED = null;
                UI_GREEN = null;
                UI_BLUE = null;
                UI_ALPHA = null;
                UI_COLOR = null;
                UI_HOVER_RED = null;
                UI_HOVER_GREEN = null;
                UI_HOVER_BLUE = null;
                UI_HOVER_ALPHA = null;
                UI_HOVER_COLOR = null;
            }
        }

        private static Processor register(String identifier, Processor processor) {
            PROCESSORS.put(identifier, processor);
            return processor;
        }

        public static Processor register(String identifier, BiConsumer<String[], ItemStack> process) {
            return register(identifier, new Processor(process));
        }

        private void execute(String[] kv, ItemStack stack) {
            this.process.accept(kv, stack);
        }

        private static void getAndExecute(String[] kv, ItemStack stack) {
            if (kv.length == 1) {
                kv = new String[]{kv[0], ""};
            }
            var processor = Processor.get(kv);
            if (processor != null) {
                processor.execute(kv, stack);
            }
        }

        private static Processor get(String[] kv) {
            return PROCESSORS.get(kv[0].toLowerCase(Locale.ROOT));
        }
    }

    public static class TypeAdapter implements JsonSerializer<ConfigurableItemStack>, JsonDeserializer<ConfigurableItemStack> {

        @Override
        public ConfigurableItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ConfigurableItemStack.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(ConfigurableItemStack src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.originalString());
        }
    }
}
