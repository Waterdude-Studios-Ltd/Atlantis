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

    public static void addDefaultAction(ActionKey key, boolean sync, BiConsumer<Page, Click> action) {
        DEFAULT_ACTIONS.add(new Action<>(key, sync, action));
    }

    public Layout() {
        this.rows = 3;
        this.title = Text.empty();
        this.data = new ArrayList<>();
        this.slotRanges = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    protected Layout(int rows, Text title, List<Data> data) {
        this.rows = rows;
        this.title = title;
        this.data = data;
        this.slotRanges = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public static <P extends Page> Layout<P> of(int rows, Text title, Data... data) {
        return new Layout<>(rows, title, Lists.newArrayList(data));
    }

    public static <P extends Page> Layout<P> of(int rows, String title, Data... data) {
        return new Layout<>(rows, Text.of(title), Lists.newArrayList(data));
    }

    public static <P extends Page> Layout<P> scope(int rows, String title, Consumer<Layout<P>> data) {
        var layout = new Layout<P>(rows, Text.of(title), Lists.newArrayList());
        data.accept(layout);
        return layout;
    }

    public Layout<P> withSlotRange(int min, int max) {
        return this.withSlotRange(NumberUtils.arrayOfRange(min, max));
    }

    public Layout<P> withSlotRange(int... values) {
        List<Integer> list = new CompactJsonArrayList<>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return this.withSlotRange(list);
    }

    public Layout<P> withSlotRange(List<Integer> values) {
        this.slotRanges.add(values);
        return this;
    }

    public Layout<P> withItems(ConfigurableItemStack... items) {
        return this.withItems(Lists.newArrayList(items));
    }

    public Layout<P> withItems(Collection<ConfigurableItemStack> items) {
        this.items.addAll(items);
        return this;
    }

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

    public Layout<P> resetActions() {
        this.actions.clear();
        return this;
    }

    public Component title(Object... args) {
        return TextUtils.format(this.title, args).compile();
    }

    public PageOptions.Builder options() {
        return PageOptions.builder().hideInventory().rows(this.rows).title(this.title);
    }

    public Data dataForOrdinal(int ordinal) {
        return this.data.get(ordinal);
    }

    public void mutateData(int ordinal, UnaryOperator<Data> mutator) {
        var data = this.data.get(ordinal);
        if (data == null) {
            return;
        }
        this.data.set(ordinal, mutator.apply(data));
    }

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

    public int buttonCount() {
        return this.data.size();
    }

    public int rows() {
        return this.rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public Text title() {
        return this.title;
    }

    public void setTitle(Text title) {
        this.title = title;
    }

    public List<Data> data() {
        return this.data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public void addData(Data... data) {
        this.data.addAll(List.of(data));
    }

    public List<List<Integer>> slotRanges() {
        return this.slotRanges;
    }

    public void setSlotRanges(List<List<Integer>> slotRanges) {
        this.slotRanges = slotRanges;
    }

    public List<Integer> slotRange(int index) {
        if (index < 0 || index >= this.slotRanges.size()) {
            return new ArrayList<>();
        }
        return this.slotRanges.get(index);
    }

    public List<ConfigurableItemStack> items() {
        return this.items;
    }

    public void setItems(List<ConfigurableItemStack> items) {
        this.items = items;
    }

    public ConfigurableItemStack itemAt(int index) {
        if (index < 0 || index >= this.items.size()) {
            return ConfigurableItemStack.empty();
        }
        return this.items.get(index);
    }

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

    public void openSimplePage(Object playerLike, Object... args) {
        PlayerReference.getSafely(playerLike).ifPresent(reference -> reference.open(this.makeSimplePage(args)));
    }

    public record Action<P extends Page>(ActionKey key, boolean sync, BiConsumer<P, Click> action) {}

    public record ActionKey(ResourceLocation key) {

        public static final ActionKey NONE = ActionKey.of(Atlantis.ID, "none");

        public static final ActionKey PREVIOUS_PAGE = ActionKey.of(Atlantis.ID, "previous_page");
        public static final ActionKey NEXT_PAGE = ActionKey.of(Atlantis.ID, "next_page");
        public static final ActionKey CLOSE_PAGE = ActionKey.of(Atlantis.ID, "close_page");

        public static ActionKey of(String namespace, String path) {
            return new ActionKey(ResourceLocation.fromNamespaceAndPath(namespace, path));
        }

        public static ActionKey parse(String key) {
            return new ActionKey(ResourceLocation.parse(key));
        }

        public <P extends Page> Action<P> bind(BiConsumer<P, Click> action) {
            return this.bind(false, action);
        }

        public <P extends Page> Action<P> bind(boolean sync, BiConsumer<P, Click> action) {
            return new Action<>(this, sync, action);
        }

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

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public @NonNull String toString() {
            return this.key.toString();
        }
    }

    public static class TypeAdapter implements JsonSerializer<Layout<? extends Page>>, JsonDeserializer<Layout<? extends Page>> {

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

    public record Data(int index, int page, ActionKey action, ConfigurableItemStack item, Text name, Text... lore) {

        public static Data of(int index, int page, ActionKey action, ConfigurableItemStack item, Text name, Text... lore) {
            return new Data(index, page, action, item, name, lore);
        }

        public static Data of(int index, int page, ActionKey action, String item, String name, String... lore) {
            return new Data(index, page, action, ConfigurableItemStack.of(item), Text.of(name), TextUtils.toArray(lore));
        }

        public Data index(int index) {
            return new Data(index, this.page, this.action, this.item, this.name, this.lore);
        }

        public Data page(int page) {
            return new Data(this.index, page, this.action, this.item, this.name, this.lore);
        }

        public Data action(ActionKey action) {
            return new Data(this.index, this.page, this.action, this.item, this.name, this.lore);
        }

        public Data item(ConfigurableItemStack item) {
            return new Data(this.index, this.page, this.action, this.item, this.name, this.lore);
        }

        public Data name(Text name) {
            return new Data(this.index, this.page, this.action, this.item, name, this.lore);
        }

        public Data lore(Text... lore) {
            return new Data(this.index, this.page, this.action, this.item, this.name, lore);
        }

        public static class TypeAdapter implements JsonSerializer<Data>, JsonDeserializer<Data> {

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
                    lore = JsonUtils.deserializeArray(object.getAsJsonArray("lore"), e -> context.deserialize(e, Text.class));
                } else {
                    lore = new Text[0];
                }
                return new Data(index, page, ActionKey.parse(action), item, name, lore);
            }

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
