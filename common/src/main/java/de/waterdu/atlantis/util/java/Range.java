package de.waterdu.atlantis.util.java;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Range<N extends Number> {

    private final N min;
    private final N max;

    private final Function<Long, N> fromOrdinalFunction;
    private final BiFunction<Number, N, Boolean> lessThanOrEqualToFunction;
    private final BiFunction<Number, N, Boolean> greaterThanOrEqualToFunction;

    private final Map<Class<? extends Number>, List<? extends Number>> lists = new HashMap<>();

    public Range(N min, N max, Function<Long, N> fromOrdinalFunction, BiFunction<Number, N, Boolean> lessThanOrEqualToFunction, BiFunction<Number, N, Boolean> greaterThanOrEqualToFunction) {
        if (max == null || min == null) {
            throw new IllegalArgumentException("Range bounds cannot be null");
        }
        this.min = min;
        this.max = max;
        this.fromOrdinalFunction = fromOrdinalFunction;
        this.lessThanOrEqualToFunction = lessThanOrEqualToFunction;
        this.greaterThanOrEqualToFunction = greaterThanOrEqualToFunction;
        if (this.min.doubleValue() > this.max.doubleValue()) {
            throw new IllegalArgumentException("Range min must be smaller than range max");
        }
    }

    public static Range<Integer> intRange(int min, int max) {
        return new Range<>(min, max, Long::intValue, (a, b) -> a.intValue() >= b, (a, b) -> a.intValue() <= b);
    }

    public static Range<Short> shortRange(short min, short max) {
        return new Range<>(min, max, Long::shortValue, (a, b) -> a.shortValue() >= b, (a, b) -> a.shortValue() <= b);
    }

    public static Range<Byte> byteRange(byte min, byte max) {
        return new Range<>(min, max, Long::byteValue, (a, b) -> a.byteValue() >= b, (a, b) -> a.byteValue() <= b);
    }

    public static Range<Long> longRange(long min, long max) {
        return new Range<>(min, max, l -> l, (a, b) -> a.longValue() >= b, (a, b) -> a.longValue() <= b);
    }

    public static Range<Double> doubleRange(double min, double max) {
        return new Range<>(min, max, Long::doubleValue, (a, b) -> a.doubleValue() >= b, (a, b) -> a.doubleValue() <= b);
    }

    public static Range<Float> floatRange(float min, float max) {
        return new Range<>(min, max, Long::floatValue, (a, b) -> a.floatValue() >= b, (a, b) -> a.floatValue() <= b);
    }

    public N min() {
        return this.min;
    }

    public N max() {
        return this.max;
    }

    public boolean contains(Number value) {
        return this.contains(value.doubleValue());
    }

    public boolean contains(int value) {
        return this.contains((double) value);
    }

    public boolean contains(long value) {
        return this.contains((double) value);
    }

    public boolean contains(float value) {
        return this.contains((double) value);
    }

    public boolean contains(double value) {
        return this.lessThanOrEqualToFunction.apply(value, this.min) && this.greaterThanOrEqualToFunction.apply(value, this.max);
    }

    public void forEach(Consumer<N> forEach) {
        var min = this.min.longValue();
        var max = this.max.longValue();
        for (var i = min; i <= max; i++) {
            forEach.accept(this.fromOrdinalFunction.apply(i));
        }
    }

    @SuppressWarnings("unchecked")
    public <M extends Number> List<M> toList(Class<M> type, Function<N, M> mapper) {
        return (List<M>) this.lists.computeIfAbsent(type, ___ -> {
            var builder = ImmutableList.<M>builder();
            this.forEach(n -> builder.add(mapper.apply(n)));
            return builder.build();
        });
    }

    public static class TypeAdapter<N extends Number> implements JsonSerializer<Range<N>>, JsonDeserializer<Range<N>> {

        public static final TypeAdapter<Integer> INTEGER = new TypeAdapter<>(Integer.class, Integer::parseInt, Range::intRange);
        public static final TypeAdapter<Short> SHORT = new TypeAdapter<>(Short.class, Short::parseShort, Range::shortRange);
        public static final TypeAdapter<Byte> BYTE = new TypeAdapter<>(Byte.class, Byte::parseByte, Range::byteRange);
        public static final TypeAdapter<Long> LONG = new TypeAdapter<>(Long.class, Long::parseLong, Range::longRange);
        public static final TypeAdapter<Double> DOUBLE = new TypeAdapter<>(Double.class, Double::parseDouble, Range::doubleRange);
        public static final TypeAdapter<Float> FLOAT = new TypeAdapter<>(Float.class, Float::parseFloat, Range::floatRange);

        public static final List<TypeAdapter<?>> ADAPTERS = List.of(INTEGER, SHORT, BYTE, LONG, DOUBLE, FLOAT);

        private final TypeToken<Range<N>> typeToken;
        private final Function<String, N> parser;
        private final BiFunction<N, N, Range<N>> range;

        @SuppressWarnings("unchecked")
        public TypeAdapter(Class<N> type, Function<String, N> parser, BiFunction<N, N, Range<N>> range) {
            this.typeToken = (TypeToken<Range<N>>) TypeToken.getParameterized(Range.class, type);
            this.parser = parser;
            this.range = range;
        }

        public TypeToken<Range<N>> typeToken() {
            return this.typeToken;
        }

        public Type type() {
            return this.typeToken().getType();
        }

        @Override
        public Range<N> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var split = json.getAsString().split("-");
            if (split.length != 2) {
                throw new JsonParseException("Invalid range: " + json);
            }
            return this.range.apply(this.parser.apply(split[0]), this.parser.apply(split[1]));
        }

        @Override
        public JsonElement serialize(Range<N> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.min + "-" + src.max);
        }
    }
}
