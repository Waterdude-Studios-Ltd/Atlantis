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

/**
 * Holds an inclusive range of numbers. Use the factory methods, such as {@link #intRange(int, int)}, to create one.
 *
 * @param <N> the type of the bounds
 */
public class Range<N extends Number> {

    private final N min;
    private final N max;

    private final Function<Long, N> fromOrdinalFunction;
    private final BiFunction<Number, N, Boolean> lessThanOrEqualToFunction;
    private final BiFunction<Number, N, Boolean> greaterThanOrEqualToFunction;

    private final Map<Class<? extends Number>, List<? extends Number>> lists = new HashMap<>();

    /**
     * Creates a range.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @param fromOrdinalFunction converts a step of {@link #forEach(Consumer)} to the type of the bounds
     * @param lessThanOrEqualToFunction checks if the value is at least the lower bound
     * @param greaterThanOrEqualToFunction checks if the value is at most the upper bound
     * @throws IllegalArgumentException if a bound is {@code null} or {@code min} is greater than {@code max}
     */
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

    /**
     * Creates a range of ints.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Integer> intRange(int min, int max) {
        return new Range<>(min, max, Long::intValue, (a, b) -> a.intValue() >= b, (a, b) -> a.intValue() <= b);
    }

    /**
     * Creates a range of shorts.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Short> shortRange(short min, short max) {
        return new Range<>(min, max, Long::shortValue, (a, b) -> a.shortValue() >= b, (a, b) -> a.shortValue() <= b);
    }

    /**
     * Creates a range of bytes.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Byte> byteRange(byte min, byte max) {
        return new Range<>(min, max, Long::byteValue, (a, b) -> a.byteValue() >= b, (a, b) -> a.byteValue() <= b);
    }

    /**
     * Creates a range of longs.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Long> longRange(long min, long max) {
        return new Range<>(min, max, l -> l, (a, b) -> a.longValue() >= b, (a, b) -> a.longValue() <= b);
    }

    /**
     * Creates a range of doubles.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Double> doubleRange(double min, double max) {
        return new Range<>(min, max, Long::doubleValue, (a, b) -> a.doubleValue() >= b, (a, b) -> a.doubleValue() <= b);
    }

    /**
     * Creates a range of floats.
     *
     * @param min the lower bound
     * @param max the upper bound
     * @return the new range
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static Range<Float> floatRange(float min, float max) {
        return new Range<>(min, max, Long::floatValue, (a, b) -> a.floatValue() >= b, (a, b) -> a.floatValue() <= b);
    }

    /**
     * @return the lower bound
     */
    public N min() {
        return this.min;
    }

    /**
     * @return the upper bound
     */
    public N max() {
        return this.max;
    }

    /**
     * The bounds are inclusive. An integer range truncates the value first, so the range 1 to 5 contains 5.5.
     */
    public boolean contains(Number value) {
        return this.contains(value.doubleValue());
    }

    /**
     * Checks if the value is in this range. The bounds are inclusive.
     *
     * @param value the value to check
     * @return {@code true} if the value is in this range
     */
    public boolean contains(int value) {
        return this.contains((double) value);
    }

    /**
     * Checks if the value is in this range. The bounds are inclusive. The value is converted to a double first.
     *
     * @param value the value to check
     * @return {@code true} if the value is in this range
     */
    public boolean contains(long value) {
        return this.contains((double) value);
    }

    /**
     * Checks if the value is in this range. The bounds are inclusive. An integer range truncates the value first.
     *
     * @param value the value to check
     * @return {@code true} if the value is in this range
     */
    public boolean contains(float value) {
        return this.contains((double) value);
    }

    /**
     * The bounds are inclusive. An integer range truncates the value first, so the range 1 to 5 contains 5.5.
     */
    public boolean contains(double value) {
        return this.lessThanOrEqualToFunction.apply(value, this.min) && this.greaterThanOrEqualToFunction.apply(value, this.max);
    }

    /**
     * Steps by 1 from {@code min} to {@code max}, both inclusive. Decimal bounds are truncated first.
     */
    public void forEach(Consumer<N> forEach) {
        var min = this.min.longValue();
        var max = this.max.longValue();
        for (var i = min; i <= max; i++) {
            forEach.accept(this.fromOrdinalFunction.apply(i));
        }
    }

    /**
     * Caches the list for each type. Later calls with the same type ignore the mapper.
     */
    @SuppressWarnings("unchecked")
    public <M extends Number> List<M> toList(Class<M> type, Function<N, M> mapper) {
        return (List<M>) this.lists.computeIfAbsent(type, ___ -> {
            var builder = ImmutableList.<M>builder();
            this.forEach(n -> builder.add(mapper.apply(n)));
            return builder.build();
        });
    }

    /**
     * Serializes a {@link Range} as text in the form {@code min-max}, for example {@code 1-5} or {@code -2.5-3}.
     *
     * @param <N> the type of the bounds
     */
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

        /**
         * Creates an adapter for ranges of one number type.
         *
         * @param type the class of the bounds
         * @param parser parses one bound from text
         * @param range creates the range from the lower and upper bounds
         */
        @SuppressWarnings("unchecked")
        public TypeAdapter(Class<N> type, Function<String, N> parser, BiFunction<N, N, Range<N>> range) {
            this.typeToken = (TypeToken<Range<N>>) TypeToken.getParameterized(Range.class, type);
            this.parser = parser;
            this.range = range;
        }

        /**
         * @return the type token of {@code Range<N>}
         */
        public TypeToken<Range<N>> typeToken() {
            return this.typeToken;
        }

        /**
         * @return the type of {@code Range<N>}, for registration with Gson
         */
        public Type type() {
            return this.typeToken().getType();
        }

        /**
         * Splits the JSON string at the {@code -} that follows a digit or a dot and parses both bounds.
         *
         * @param json the JSON string that holds the range
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the range
         * @throws JsonParseException if the text does not split into exactly two bounds
         */
        @Override
        public Range<N> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var split = json.getAsString().split("(?<=[0-9.])-");
            if (split.length != 2) {
                throw new JsonParseException("Invalid range: " + json);
            }
            return this.range.apply(this.parser.apply(split[0]), this.parser.apply(split[1]));
        }

        /**
         * Writes the range as a JSON string in the form {@code min-max}.
         *
         * @param src the range to serialize
         * @param typeOfSrc the type of the range
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(Range<N> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.min + "-" + src.max);
        }
    }
}
