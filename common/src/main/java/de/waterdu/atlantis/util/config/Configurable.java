package de.waterdu.atlantis.util.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.level.schematic.Schematic;
import de.waterdu.atlantis.level.schematic.Schematics;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.java.interfaces.EqualityTest;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;
import de.waterdu.atlantis.util.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public record Configurable<T>(ResettableLazy<T> value) {

    private Configurable(T value) {
        this(ResettableLazy.of(() -> value));
    }

    private Configurable(Supplier<T> value) {
        this(ResettableLazy.of(value));
    }

    public static <T> Configurable<T> of(T value) {
        return new Configurable<>(value);
    }

    public static <T> Configurable<T> of(Supplier<T> value) {
        return new Configurable<>(value);
    }

    public static <T> Configurable<T> of(ResettableLazy<T> value) {
        return new Configurable<>(value);
    }

    @SafeVarargs
    public static <T> List<Configurable<T>> ofList(T... values) {
        var configurables = new ArrayList<Configurable<T>>();
        for (var value : values) {
            configurables.add(Configurable.of(value));
        }
        return configurables;
    }

    @SafeVarargs
    public static <T> List<Configurable<T>> ofList(Supplier<T>... values) {
        var configurables = new ArrayList<Configurable<T>>();
        for (var value : values) {
            configurables.add(Configurable.of(value));
        }
        return configurables;
    }

    @SafeVarargs
    public static <T> List<Configurable<T>> ofList(ResettableLazy<T>... values) {
        var configurables = new ArrayList<Configurable<T>>();
        for (var value : values) {
            configurables.add(Configurable.of(value));
        }
        return configurables;
    }

    public static <T> void registerContext(GsonBuilder gson, Class<T> type) {
        Configurable.registerContext(
                gson,
                type,
                (json, context) -> context.deserialize(json, type),
                (src, context) -> context.serialize(src)
        );
    }

    public static <T> void registerContext(GsonBuilder gson,
                                           Class<T> type,
                                           BiFunction<JsonElement, JsonDeserializationContext, T> deserializer,
                                           BiFunction<T, JsonSerializationContext, JsonElement> serializer) {
        gson.registerTypeAdapter(Configurable.typeOf(type), new TypeAdapter<>(deserializer, serializer));
    }

    private static <T> Type typeOf(Class<T> type) {
        return TypeToken.getParameterized(Configurable.class, type).getType();
    }

    public static void registerDefaultContexts(GsonBuilder gson) {
        Configurable.registerContext(gson, SoundEvent.class,
                (json, context) -> Registries.SOUND_EVENT.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(src.getLocation().toString())
        );

        Configurable.registerContext(gson, Item.class,
                (json, context) -> Registries.ITEM.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(src.id().toString())
        );

        Configurable.registerContext(gson, Block.class,
                (json, context) -> Registries.BLOCK.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(src.id().toString())
        );

        Configurable.registerContext(gson, Biome.class,
                (json, context) -> Registries.BIOME.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(Registries.BIOME.registryFromRegistryKey().getKey(src).toString())
        );

        Configurable.registerContext(gson, MobEffect.class,
                (json, context) -> Registries.MOB_EFFECT.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(Registries.MOB_EFFECT.registryFromRegistryKey().getKey(src).toString())
        );

        Configurable.registerContext(gson, Potion.class,
                (json, context) -> Registries.POTION.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(Registries.POTION.registryFromRegistryKey().getKey(src).toString())
        );

        Configurable.registerContext(gson, Color.class,
                (json, context) -> {
                    JsonObject object = json.getAsJsonObject();
                    int r = object.get("red").getAsInt();
                    int g = object.get("green").getAsInt();
                    int b = object.get("blue").getAsInt();
                    int a = 255;
                    if (object.has("alpha")) {
                        a = object.get("alpha").getAsInt();
                    }
                    return new Color(r, g, b, a);
                },
                (src, context) -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("red", src.getRed());
                    object.addProperty("green", src.getGreen());
                    object.addProperty("blue", src.getBlue());
                    object.addProperty("alpha", src.getAlpha());
                    return object;
                }
        );

        Configurable.registerContext(gson, Schematic.class,
                (json, context) -> Schematics.get(json.getAsString()),
                (src, context) -> new JsonPrimitive(src.name())
        );

        Configurable.registerContext(gson, ParticleType.class,
                (json, context) -> Registries.PARTICLE_TYPE.registryFromRegistryKey().get(ResourceLocation.parse(json.getAsString())),
                (src, context) -> new JsonPrimitive(Registries.PARTICLE_TYPE.registryFromRegistryKey().getKey(src).toString())
        );

        Configurable.registerContext(gson, Particle.class,
                (json, context) -> {
                    if (json.isJsonPrimitive()) {
                        return Particle.of(json.getAsString());
                    } else {
                        var object = json.getAsJsonObject();
                        var deserializer = object.has("particle") ? object.get("particle").getAsString() : "";
                        if (deserializer.isEmpty()) {
                            deserializer = object.has("type") ? object.get("type").getAsString() : "";
                        }
                        var particle = Particle.of(deserializer);
                        if (object.has("parameters")) {
                            Map<String, Double> parameters = context.deserialize(object.get("parameters"), Particle.PARAMETERS_MAP_TYPE);
                            particle.withParameters(parameters);
                        }
                        return particle;
                    }
                },
                (src, context) -> {
                    var particle = src.particle();
                    var parameters = src.parameters();
                    if (parameters.isEmpty()) {
                        return new JsonPrimitive(particle);
                    } else {
                        var object = new JsonObject();
                        object.addProperty("particle", particle);
                        object.add("parameters", context.serialize(parameters, Particle.PARAMETERS_MAP_TYPE));
                        return object;
                    }
                }
        );

        Configurable.registerContext(gson, FlatLayerInfo.class,
                (json, context) -> {
                    var object = json.getAsJsonObject();
                    var height = object.get("height").getAsInt();
                    ConfigurableItemStack block = context.deserialize(object.get("block"), ConfigurableItemStack.class);
                    return new FlatLayerInfo(height, block.block());
                },
                (src, context) -> {
                    var object = new JsonObject();
                    object.addProperty("height", src.getHeight());
                    object.addProperty("block", src.getBlockState().getBlock().id().toString());
                    return object;
                }
        );

        Configurable.registerContext(gson, Pattern.class,
                (json, context) -> {
                    if (json.isJsonPrimitive()) {
                        return Pattern.compile(json.getAsString());
                    } else {
                        var object = json.getAsJsonObject();
                        return Pattern.compile(object.get("pattern").getAsString(), object.has("flags") ? object.get("flags").getAsInt() : 0);
                    }
                },
                (src, context) -> {
                    if (src.flags() > 0) {
                        var object = new JsonObject();
                        object.addProperty("pattern", src.pattern());
                        object.addProperty("flags", src.flags());
                        return object;
                    } else {
                        return new JsonPrimitive(src.pattern());
                    }
                }
        );

        Mods.forEachProxy(proxy -> proxy.registerConfigurableContexts(gson));
    }

    public static <T> boolean contains(T object, Collection<Configurable<T>> configurables) {
        return Configurable.contains(object, Object::equals, configurables);
    }

    public static <T> boolean contains(T object, EqualityTest<T> equality, Collection<Configurable<T>> configurables) {
        for (Configurable<T> configurable : configurables) {
            if (equality.equals(object, configurable.get())) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean contains(T object1, T object2, EqualityTest<T> equality, Collection<Configurable<T>> configurables) {
        for (Configurable<T> configurable : configurables) {
            if (equality.equals(object1, configurable.get()) || equality.equals(object2, configurable.get())) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    public static <T> boolean contains(T object, Configurable<T>... configurables) {
        return Configurable.contains(object, Object::equals, configurables);
    }

    @SafeVarargs
    public static <T> boolean contains(T object, EqualityTest<T> equality, Configurable<T>... configurables) {
        for (Configurable<T> configurable : configurables) {
            if (equality.equals(object, configurable.get())) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    public static <T> boolean contains(T object1, T object2, EqualityTest<T> equality, Configurable<T>... configurables) {
        for (Configurable<T> configurable : configurables) {
            if (equality.equals(object1, configurable.get()) || equality.equals(object2, configurable.get())) {
                return true;
            }
        }
        return false;
    }

    public T get() {
        return this.value.get();
    }

    public void reset() {
        this.value.reset();
    }

    public record TypeAdapter<T>(
            BiFunction<JsonElement, JsonDeserializationContext, T> deserializer,
            BiFunction<T, JsonSerializationContext, JsonElement> serializer
    ) implements JsonSerializer<Configurable<T>>, JsonDeserializer<Configurable<T>> {

        @Override
        public Configurable<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Configurable.of(() -> this.deserializer.apply(json, context));
        }

        @Override
        public JsonElement serialize(Configurable<T> src, Type typeOfSrc, JsonSerializationContext context) {
            return this.serializer.apply(src.get(), context);
        }
    }
}
