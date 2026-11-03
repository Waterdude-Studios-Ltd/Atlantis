package de.waterdu.atlantis.ui.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.typeadapters.NumberListTypeAdapter;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.java.JsonUtils;
import de.waterdu.atlantis.util.java.NumberUtils;
import de.waterdu.atlantis.util.java.collection.CompactJsonArrayList;
import de.waterdu.atlantis.util.text.Text;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Describes a page that can be configured: its rows, title, button data, slot ranges and items. Button data can
 * refer to an {@link Action} by its {@link ActionKey}.
 *
 * @param <P> the type of the page that the actions receive
 */
public class Layout<P extends Page> {

    private int rows;
    private Text title;
    private List<Data> data;
    private List<List<Integer>> slotRanges;
    private List<ConfigurableItemStack> items;

    private final Map<ActionKey, Action<P>> actions = new HashMap<>();

    private static final Set<Action<Page>> DEFAULT_ACTIONS = new HashSet<>();

    static {
        Layout.addDefaultAction(ActionKey.PREVIOUS_PAGE, false, ((page, click) -> click.previous()));
        Layout.addDefaultAction(ActionKey.NEXT_PAGE, false, ((page, click) -> click.next()));
        Layout.addDefaultAction(ActionKey.CLOSE_PAGE, false, ((page, click) -> click.closePage()));
    }

    /**
     * Adds an action to each layout that gets its actions after this call.
     * A default action replaces a layout action that has the same key.
     *
     * @param sync makes the button {@link Button.Builder#sync()} and lets it be pressed only once
     */
    public static void addDefaultAction(ActionKey key, boolean sync, BiConsumer<Page, Click> action) {
        DEFAULT_ACTIONS.add(new Action<>(key, sync, action));
    }

    /**
     * Creates a layout with 3 rows, an empty title and no data, slot ranges or items.
     */
    public Layout() {
        this.rows = 3;
        this.title = Text.empty();
        this.data = new ArrayList<>();
        this.slotRanges = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    /**
     * Creates a layout with no slot ranges or items.
     *
     * @param rows the number of rows of the page
     * @param title the title of the page
     * @param data the button data
     */
    protected Layout(int rows, Text title, List<Data> data) {
        this.rows = rows;
        this.title = title;
        this.data = data;
        this.slotRanges = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    /**
     * Creates a layout with no slot ranges or items.
     *
     * @param rows the number of rows of the page
     * @param title the title of the page
     * @param data the button data
     * @param <P> the type of the page that the actions receive
     * @return the new layout
     */
    public static <P extends Page> Layout<P> of(int rows, Text title, Data... data) {
        return new Layout<>(rows, title, Lists.newArrayList(data));
    }

    /**
     * Creates a layout with no slot ranges or items.
     *
     * @param rows the number of rows of the page
     * @param title the title of the page
     * @param data the button data
     * @param <P> the type of the page that the actions receive
     * @return the new layout
     */
    public static <P extends Page> Layout<P> of(int rows, String title, Data... data) {
        return new Layout<>(rows, Text.of(title), Lists.newArrayList(data));
    }

    /**
     * Creates a layout with no data, then lets the consumer configure it.
     *
     * @param rows the number of rows of the page
     * @param title the title of the page
     * @param data configures the new layout
     * @param <P> the type of the page that the actions receive
     * @return the new layout
     */
    public static <P extends Page> Layout<P> scope(int rows, String title, Consumer<Layout<P>> data) {
        var layout = new Layout<P>(rows, Text.of(title), Lists.newArrayList());
        data.accept(layout);
        return layout;
    }

    /**
     * Both ends are inclusive. If {@code min} is larger than {@code max}, the range is descending.
     */
    public Layout<P> withSlotRange(int min, int max) {
        return this.withSlotRange(NumberUtils.arrayOfRange(min, max));
    }

    /**
     * Adds a slot range with the given slots.
     *
     * @param values the slot indexes, in order
     * @return this layout
     */
    public Layout<P> withSlotRange(int... values) {
        List<Integer> list = new CompactJsonArrayList<>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return this.withSlotRange(list);
    }

    /**
     * Adds a slot range. The list is not copied.
     *
     * @param values the slot indexes, in order
     * @return this layout
     */
    public Layout<P> withSlotRange(List<Integer> values) {
        this.slotRanges.add(values);
        return this;
    }

    /**
     * Adds items to the end of the item list.
     *
     * @param items the items to add
     * @return this layout
     */
    public Layout<P> withItems(ConfigurableItemStack... items) {
        return this.withItems(Lists.newArrayList(items));
    }

    /**
     * Adds items to the end of the item list.
     *
     * @param items the items to add
     * @return this layout
     */
    public Layout<P> withItems(Collection<ConfigurableItemStack> items) {
        this.items.addAll(items);
        return this;
    }

    /**
     * Has no effect if the layout already has actions. Call {@link #resetActions()} first to replace them.
     * Also adds the default actions. These replace given actions that have the same key.
     * See {@link #addDefaultAction(ActionKey, boolean, BiConsumer)}.
     */
    @SafeVarargs
    public final Layout<P> withActions(Action<P>... actions) {
        if (this.actions.isEmpty()) {
            for (var action : actions) {
                this.actions.put(action.key, action);
            }
            for (var action : DEFAULT_ACTIONS) {
                this.actions.put(action.key, new Action<>(action.key, action.sync, action.action::accept));
            }
        }
        return this;
    }

    /**
     * Removes all actions, so {@link #withActions(Action...)} can add new ones.
     *
     * @return this layout
     */
    public Layout<P> resetActions() {
        this.actions.clear();
        return this;
    }

    /**
     * @param args the values for the {@code %1}, {@code %2} and further placeholders
     */
    public Component title(Object... args) {
        return TextUtils.format(this.title, args).compile();
    }

    /**
     * Creates page options with the rows and the unformatted title of this layout. The player inventory is hidden.
     *
     * @return a new options builder
     */
    public PageOptions.Builder options() {
        return PageOptions.builder().hideInventory().rows(this.rows).title(this.title);
    }

    /**
     * @param ordinal the position in {@link #data()}, not the slot index
     */
    public Data dataForOrdinal(int ordinal) {
        return this.data.get(ordinal);
    }

    /**
     * Replaces the button data at a position with the result of the mutator.
     *
     * @param ordinal the position in {@link #data()}, not the slot index
     * @param mutator gives the new data from the old data
     */
    public void mutateData(int ordinal, UnaryOperator<Data> mutator) {
        var data = this.data.get(ordinal);
        if (data == null) {
            return;
        }
        this.data.set(ordinal, mutator.apply(data));
    }

    /**
     * @param ordinal the position in {@link #data()}, not the slot index
     * @param args the values for the {@code %1}, {@code %2} and further placeholders in the name and lore
     */
    @SuppressWarnings("unchecked")
    public Button.Builder buttonAt(int ordinal, Object... args) {
        var data = this.dataForOrdinal(ordinal);
        var builder = Button.builder()
                .index(data.index)
                .item(data.item, 1)
                .name(data.name.format(args));

        for (var lore : data.lore) {
            builder.loreLine(lore.format(args));
        }
        if (!data.action.equals(ActionKey.NONE)) {
            var action = this.actions.getOrDefault(data.action, null);
            if (action != null) {
                if (action.sync) {
                    builder.sync().singlePress();
                }
                builder.onClick((BiConsumer<Page, Click>) action.action);
            }
        }
        return builder;
    }

    /**
     * @return the number of button data entries
     */
    public int buttonCount() {
        return this.data.size();
    }

    /**
     * @return the number of rows of the page
     */
    public int rows() {
        return this.rows;
    }

    /**
     * Sets the number of rows of the page.
     *
     * @param rows the number of rows
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * @return the unformatted title of the page
     */
    public Text title() {
        return this.title;
    }

    /**
     * Sets the title of the page.
     *
     * @param title the title, which can have {@code %1}, {@code %2} and further placeholders
     */
    public void setTitle(Text title) {
        this.title = title;
    }

    /**
     * @return the button data
     */
    public List<Data> data() {
        return this.data;
    }

    /**
     * Replaces the button data.
     *
     * @param data the button data
     */
    public void setData(List<Data> data) {
        this.data = data;
    }

    /**
     * Adds button data to the end of the list.
     *
     * @param data the button data to add
     */
    public void addData(Data... data) {
        this.data.addAll(List.of(data));
    }

    /**
     * @return the slot ranges
     */
    public List<List<Integer>> slotRanges() {
        return this.slotRanges;
    }

    /**
     * Replaces the slot ranges.
     *
     * @param slotRanges the slot ranges
     */
    public void setSlotRanges(List<List<Integer>> slotRanges) {
        this.slotRanges = slotRanges;
    }

    /**
     * @return the slot range, or a new empty list if the index is out of range
     */
    public List<Integer> slotRange(int index) {
        if (index < 0 || index >= this.slotRanges.size()) {
            return new ArrayList<>();
        }
        return this.slotRanges.get(index);
    }

    /**
     * @return the items
     */
    public List<ConfigurableItemStack> items() {
        return this.items;
    }

    /**
     * Replaces the items.
     *
     * @param items the items
     */
    public void setItems(List<ConfigurableItemStack> items) {
        this.items = items;
    }

    /**
     * @return the item, or an empty item if the index is out of range
     */
    public ConfigurableItemStack itemAt(int index) {
        if (index < 0 || index >= this.items.size()) {
            return ConfigurableItemStack.empty();
        }
        return this.items.get(index);
    }

    /**
     * @param args the values for the {@code %1}, {@code %2} and further placeholders in the title, names and lore
     */
    public Page makeSimplePage(Object... args) {
        return new Page() {
            @Override
            public PageOptions options(PlayerReference player) {
                return Layout.this.options().title(Layout.this.title().copy().format(args)).build();
            }

            @Override
            public void addButtons(PlayerReference player, ButtonCollector buttons) {
                for (var i = 0; i < Layout.this.buttonCount(); i++) {
                    buttons.collect(Layout.this.buttonAt(i, args));
                }
            }
        };
    }

    /**
     * @param playerLike a UUID, name, Player, PlayerReference or PlayerData. Other values do nothing.
     * @param args the values for the {@code %1}, {@code %2} and further placeholders in the title, names and lore
     */
    public void openSimplePage(Object playerLike, Object... args) {
        PlayerReference.getSafely(playerLike).ifPresent(reference -> reference.open(this.makeSimplePage(args)));
    }

    /**
     * Binds a click handler to an action key.
     *
     * @param key the key that button data uses to refer to this action
     * @param sync makes the button {@link Button.Builder#sync()} and lets it be pressed only once
     * @param action receives the page and the click
     * @param <P> the type of the page that the action receives
     */
    public record Action<P extends Page>(ActionKey key, boolean sync, BiConsumer<P, Click> action) {}

    /**
     * Identifies an {@link Action} by a resource location.
     *
     * @param key the resource location
     */
    public record ActionKey(ResourceLocation key) {

        public static final ActionKey NONE = ActionKey.of(Atlantis.ID, "none");

        public static final ActionKey PREVIOUS_PAGE = ActionKey.of(Atlantis.ID, "previous_page");
        public static final ActionKey NEXT_PAGE = ActionKey.of(Atlantis.ID, "next_page");
        public static final ActionKey CLOSE_PAGE = ActionKey.of(Atlantis.ID, "close_page");

        /**
         * Creates a key from a namespace and a path.
         *
         * @param namespace the namespace of the resource location
         * @param path the path of the resource location
         * @return the new key
         */
        public static ActionKey of(String namespace, String path) {
            return new ActionKey(ResourceLocation.fromNamespaceAndPath(namespace, path));
        }

        /**
         * @param key a resource location. A key without a namespace uses the {@code minecraft} namespace.
         */
        public static ActionKey parse(String key) {
            return new ActionKey(ResourceLocation.parse(key));
        }

        /**
         * Creates an action with this key that is not sync.
         *
         * @param action receives the page and the click
         * @param <P> the type of the page that the action receives
         * @return the new action
         */
        public <P extends Page> Action<P> bind(BiConsumer<P, Click> action) {
            return this.bind(false, action);
        }

        /**
         * @param sync makes the button {@link Button.Builder#sync()} and lets it be pressed only once
         */
        public <P extends Page> Action<P> bind(boolean sync, BiConsumer<P, Click> action) {
            return new Action<>(this, sync, action);
        }

        /**
         * Compares by resource location.
         *
         * @param o the object to compare
         * @return {@code true} if the object is an action key with the same resource location
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ActionKey(ResourceLocation otherKey))) {
                return false;
            }
            return this.key.equals(otherKey);
        }

        /**
         * @return the hash code of the resource location
         */
        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        /**
         * @return the resource location, as {@code namespace:path}
         */
        @Override
        public @NonNull String toString() {
            return this.key.toString();
        }
    }

    /**
     * Converts a {@link Layout} to and from JSON. It writes the rows, title, data and slot ranges, but not the
     * items.
     */
    public static class TypeAdapter implements JsonSerializer<Layout<? extends Page>>, JsonDeserializer<Layout<? extends Page>> {

        /**
         * Reads a layout from a JSON object. The {@code lists} field with the slot ranges is optional.
         *
         * @param json the JSON object
         * @param typeOfT the type to read
         * @param context reads the title, the data and the slot ranges
         * @return the new layout
         */
        @Override
        public Layout<? extends Page> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();
            var rows = object.getAsJsonPrimitive("rows").getAsInt();
            Text title = context.deserialize(object.get("title"), Text.class);
            var data = object.getAsJsonArray("data");
            var dataArray = new Data[data.size()];
            var i = 0;
            for (var e : data) {
                dataArray[i++] = context.deserialize(e, Data.class);
            }
            var def = Layout.of(rows, title, dataArray);
            if (object.has("lists")) {
                List<List<Integer>> lists = JsonUtils.deserializeList(object.getAsJsonArray("lists"), e -> context.deserialize(e, NumberListTypeAdapter.INTEGER.type()));
                for (var list : lists) {
                    def.withSlotRange(new CompactJsonArrayList<>(list));
                }
            }
            return def;
        }

        /**
         * Writes the rows, title, data and slot ranges of a layout to a JSON object.
         *
         * @param src the layout to write
         * @param typeOfSrc the type of the layout
         * @param context writes the title, the data and the slot ranges
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(Layout<? extends Page> src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            object.addProperty("rows", src.rows());
            object.add("title", context.serialize(src.title()));
            var array = new JsonArray();
            for (var data : src.data()) {
                array.add(context.serialize(data));
            }
            object.add("data", array);
            object.add("lists", JsonUtils.serializeList(src.slotRanges(), list -> context.serialize(list, NumberListTypeAdapter.INTEGER.type())));
            return object;
        }
    }

    /**
     * Describes one button of a layout.
     *
     * @param index the slot index
     * @param page the page of the button
     * @param action the key of the action that runs on click, or {@link ActionKey#NONE}
     * @param item the item of the button
     * @param name the name of the button, which can have {@code %1}, {@code %2} and further placeholders
     * @param lore the lore lines, which can have {@code %1}, {@code %2} and further placeholders
     */
    public record Data(int index, int page, ActionKey action, ConfigurableItemStack item, Text name, Text... lore) {

        /**
         * Creates button data.
         *
         * @param index the slot index
         * @param page the page of the button
         * @param action the key of the action that runs on click, or {@link ActionKey#NONE}
         * @param item the item of the button
         * @param name the name of the button
         * @param lore the lore lines
         * @return the new data
         */
        public static Data of(int index, int page, ActionKey action, ConfigurableItemStack item, Text name, Text... lore) {
            return new Data(index, page, action, item, name, lore);
        }

        /**
         * Creates button data from strings.
         *
         * @param index the slot index
         * @param page the page of the button
         * @param action the key of the action that runs on click, or {@link ActionKey#NONE}
         * @param item the item of the button, as read by {@link ConfigurableItemStack#of(String)}
         * @param name the name of the button
         * @param lore the lore lines
         * @return the new data
         */
        public static Data of(int index, int page, ActionKey action, String item, String name, String... lore) {
            return new Data(index, page, action, ConfigurableItemStack.of(item), Text.of(name), TextUtils.toArray(lore));
        }

        /**
         * Copies this data with a different slot index.
         *
         * @param index the slot index
         * @return the new data
         */
        public Data index(int index) {
            return new Data(index, this.page, this.action, this.item, this.name, this.lore);
        }

        /**
         * Copies this data with a different page.
         *
         * @param page the page of the button
         * @return the new data
         */
        public Data page(int page) {
            return new Data(this.index, page, this.action, this.item, this.name, this.lore);
        }

        /**
         * Copies this data. The copy keeps the current action, because the parameter is not used.
         *
         * @param action the action key, which is ignored
         * @return the new data
         */
        public Data action(ActionKey action) {
            return new Data(this.index, this.page, this.action, this.item, this.name, this.lore);
        }

        /**
         * Copies this data. The copy keeps the current item, because the parameter is not used.
         *
         * @param item the item, which is ignored
         * @return the new data
         */
        public Data item(ConfigurableItemStack item) {
            return new Data(this.index, this.page, this.action, this.item, this.name, this.lore);
        }

        /**
         * Copies this data with a different name.
         *
         * @param name the name of the button
         * @return the new data
         */
        public Data name(Text name) {
            return new Data(this.index, this.page, this.action, this.item, name, this.lore);
        }

        /**
         * Copies this data with different lore lines.
         *
         * @param lore the lore lines
         * @return the new data
         */
        public Data lore(Text... lore) {
            return new Data(this.index, this.page, this.action, this.item, this.name, lore);
        }

        /**
         * Converts {@link Data} to and from JSON.
         */
        public static class TypeAdapter implements JsonSerializer<Data>, JsonDeserializer<Data> {

            /**
             * Reads button data from a JSON object. Missing fields get defaults: index 0, page -1, no action, air, an
             * empty name and no lore.
             *
             * @param json the JSON object
             * @param typeOfT the type to read
             * @param context reads the item, the name and the lore
             * @return the new data
             */
            @Override
            public Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject object = json.getAsJsonObject();
                int index = JsonUtils.getOrDefault(object, "index", int.class, 0, context);
                int page = JsonUtils.getOrDefault(object, "page", int.class, -1, context);
                var action = JsonUtils.getOrDefault(object, "action", String.class, ActionKey.NONE.toString(), context);
                ConfigurableItemStack item = JsonUtils.getOrDefault(object, "item", ConfigurableItemStack.class, ConfigurableItemStack.of(Items.AIR), context);
                Text name = JsonUtils.getOrDefault(object, "name", Text.class, Text.empty(), context);
                Text[] lore;
                if (object.has("lore")) {
                    lore = JsonUtils.deserializeArray(object.getAsJsonArray("lore"), Text.class, e -> context.deserialize(e, Text.class));
                } else {
                    lore = new Text[0];
                }
                return new Data(index, page, ActionKey.parse(action), item, name, lore);
            }

            /**
             * Writes all fields of button data to a JSON object.
             *
             * @param src the data to write
             * @param typeOfSrc the type of the data
             * @param context writes the item, the name and the lore
             * @return the JSON object
             */
            @Override
            public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject object = new JsonObject();
                object.addProperty("index", src.index);
                object.addProperty("page", src.page);
                object.addProperty("action", src.action.toString());
                object.add("item", context.serialize(src.item));
                object.add("name", context.serialize(src.name));
                var lore = new JsonArray();
                for (var line : src.lore) {
                    lore.add(context.serialize(line));
                }
                object.add("lore", lore);
                return object;
            }
        }
    }
}
