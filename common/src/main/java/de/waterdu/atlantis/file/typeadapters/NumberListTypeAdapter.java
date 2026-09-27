package de.waterdu.atlantis.file.typeadapters;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.util.java.JsonUtils;
import de.waterdu.atlantis.util.java.collection.CompactJsonArrayList;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class NumberListTypeAdapter<T extends Number> implements JsonSerializer<List<T>>, JsonDeserializer<List<T>> {

    protected static final Set<Class<?>> CARDINALS = Sets.newHashSet(
            Integer.class, Short.class, Byte.class, Long.class, BigInteger.class, AtomicInteger.class, AtomicLong.class
    );

    public static final NumberListTypeAdapter<Integer> INTEGER = new NumberListTypeAdapter<>(Integer.class, Integer::parseInt, Long::intValue, String::valueOf);
    public static final NumberListTypeAdapter<Long> LONG = new NumberListTypeAdapter<>(Long.class, Long::parseLong, ___ -> ___, String::valueOf);
    public static final NumberListTypeAdapter<Short> SHORT = new NumberListTypeAdapter<>(Short.class, Short::parseShort, Long::shortValue, String::valueOf);
    public static final NumberListTypeAdapter<Byte> BYTE = new NumberListTypeAdapter<>(Byte.class, Byte::parseByte, Long::byteValue, String::valueOf);
    public static final NumberListTypeAdapter<BigInteger> BIG_INTEGER = new NumberListTypeAdapter<>(BigInteger.class, s -> BigInteger.valueOf(Long.parseLong(s)), BigInteger::valueOf, String::valueOf);
    public static final NumberListTypeAdapter<AtomicInteger> ATOMIC_INTEGER = new NumberListTypeAdapter<>(AtomicInteger.class, s -> new AtomicInteger(Integer.parseInt(s)), l -> new AtomicInteger(l.intValue()), a -> String.valueOf(a.get()));
    public static final NumberListTypeAdapter<AtomicLong> ATOMIC_LONG = new NumberListTypeAdapter<>(AtomicLong.class, s -> new AtomicLong(Long.parseLong(s)), AtomicLong::new, a -> String.valueOf(a.get()));

    public static final List<NumberListTypeAdapter<?>> ADAPTERS = List.of(INTEGER, LONG, SHORT, BYTE, BIG_INTEGER, ATOMIC_INTEGER, ATOMIC_LONG);

    private final Class<T> type;
    private final Function<String, T> decoder;
    private final Function<Long, T> converter;
    private final Function<T, String> encoder;

    private final boolean cardinal;
    private final TypeToken<?> typeToken;

    public NumberListTypeAdapter(
            Class<T> type, Function<String, T> decoder, Function<Long, T> converter, Function<T, String> encoder
    ) {
        this.type = type;
        this.decoder = decoder;
        this.converter = converter;
        this.encoder = encoder;
        this.cardinal = NumberListTypeAdapter.CARDINALS.contains(type);
        this.typeToken = TypeToken.getParameterized(List.class, type);
    }

    public TypeToken<?> typeToken() {
        return this.typeToken;
    }

    public Type type() {
        return this.typeToken().getType();
    }

    @Override
    public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            return JsonUtils.deserializeList(json.getAsJsonArray(), element -> context.deserialize(element, this.type));
        } else if (json.isJsonPrimitive()) {
            var value = json.getAsString();
            var array = value.split(",");
            var values = new CompactJsonArrayList<T>(array.length);
            for (var s : array) {
                try {
                    if (this.cardinal && s.contains("-")) {
                        var r = s.split("-");
                        for (var i = this.decoder.apply(r[0]).longValue(); i <= this.decoder.apply(r[1]).longValue(); i++) {
                            values.add(this.converter.apply(i));
                        }
                    } else {
                        values.add(this.decoder.apply(s));
                    }
                } catch (Exception ignored) {}
            }
            return values;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public JsonElement serialize(List<T> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof CompactJsonArrayList) {
            src.sort(null);
            var builder = new StringBuilder();
            var check = 0L;
            var range = false;
            for (var i : src) {
                if (builder.isEmpty()) {
                    check = i.longValue();
                    builder.append(this.encoder.apply(i));
                } else {
                    if (this.cardinal && i.longValue() == check + 1) {
                        range = true;
                        check = i.longValue();
                    } else {
                        if (range) {
                            builder.append("-").append(check);
                            range = false;
                        }
                        builder.append(",").append(this.encoder.apply(i));
                    }
                }
            }
            if (range) {
                builder.append("-").append(check);
            }
            return new JsonPrimitive(builder.toString());
        } else {
            return JsonUtils.serializeList(src, JsonPrimitive::new);
        }
    }
}
