package de.waterdu.atlantis.util.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeTest {

    private final Gson gson = adapters();

    private static Gson adapters() {
        var builder = new GsonBuilder();
        for (var adapter : Range.TypeAdapter.ADAPTERS) {
            builder.registerTypeAdapter(adapter.type(), adapter);
        }
        return builder.create();
    }

    @ParameterizedTest
    @CsvSource({
            "1,  10, 1,  true",
            "1,  10, 5,  true",
            "1,  10, 10, true",
            "1,  10, 0,  false",
            "1,  10, 11, false",
            "-5, -1, -3, true",
            "-5, -1, 0,  false",
            "3,  3,  3,  true",
    })
    void intContains(int min, int max, int value, boolean expected) {
        assertThat(Range.intRange(min, max).contains(value)).isEqualTo(expected);
    }

    @Test
    void containsWorksForEveryNumberType() {
        assertThat(Range.longRange(0, 5_000_000_000L).contains(4_000_000_000L)).isTrue();
        assertThat(Range.longRange(0, 5).contains(6L)).isFalse();
        assertThat(Range.shortRange((short) 0, (short) 5).contains(5)).isTrue();
        assertThat(Range.byteRange((byte) 0, (byte) 5).contains(6)).isFalse();
        assertThat(Range.doubleRange(0.5, 1.5).contains(1.0)).isTrue();
        assertThat(Range.doubleRange(0.5, 1.5).contains(1.6)).isFalse();
        assertThat(Range.doubleRange(0.5, 1.5).contains(0.4)).isFalse();
        assertThat(Range.floatRange(0.5F, 1.5F).contains(1.5F)).isTrue();
        assertThat(Range.intRange(0, 3).contains(Integer.valueOf(2))).isTrue();
    }

    @Test
    void intRangeTruncatesFractionalValues() {
        assertThat(Range.intRange(0, 10).contains(10.5)).isTrue();
    }

    @Test
    void rejectsInvertedOrNullBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> Range.intRange(5, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new Range<Integer>(null, 1, Long::intValue, (a, b) -> true, (a, b) -> true));
    }

    @Test
    void minAndMax() {
        var range = Range.intRange(2, 8);

        assertThat(range.min()).isEqualTo(2);
        assertThat(range.max()).isEqualTo(8);
    }

    @Test
    void forEachVisitsEveryValue() {
        var values = new ArrayList<Integer>();

        Range.intRange(-1, 2).forEach(values::add);

        assertThat(values).containsExactly(-1, 0, 1, 2);
    }

    @Test
    void toListIsCachedPerType() {
        var range = Range.intRange(1, 3);

        var longs = range.toList(Long.class, Integer::longValue);

        assertThat(longs).containsExactly(1L, 2L, 3L);
        assertThat(range.toList(Long.class, Integer::longValue)).isSameAs(longs);
    }

    @Test
    void typeAdapterRoundTrips() {
        var range = this.gson.<Range<Integer>>fromJson("\"3-7\"", Range.TypeAdapter.INTEGER.type());

        assertThat(range.min()).isEqualTo(3);
        assertThat(range.max()).isEqualTo(7);
        assertThat(this.gson.toJson(range, Range.TypeAdapter.INTEGER.type())).isEqualTo("\"3-7\"");
    }

    @Test
    void typeAdapterHandlesNegativeBounds() {
        var type = Range.TypeAdapter.INTEGER.type();
        var range = Range.intRange(-5, -1);

        var json = this.gson.toJson(range, type);
        Range<Integer> parsed = this.gson.fromJson(json, type);

        assertThat(json).isEqualTo("\"-5--1\"");
        assertThat(parsed.min()).isEqualTo(-5);
        assertThat(parsed.max()).isEqualTo(-1);
        assertThat(this.gson.<Range<Integer>>fromJson("\"-2-2\"", type).contains(0)).isTrue();
    }

    @Test
    void typeAdapterParsesDecimals() {
        Range<Double> range = this.gson.fromJson("\"0.5-2.5\"", Range.TypeAdapter.DOUBLE.type());

        assertThat(range.min()).isEqualTo(0.5);
        assertThat(range.max()).isEqualTo(2.5);
    }

    @Test
    void typeAdapterRejectsMalformedRanges() {
        assertThatThrownBy(() -> this.gson.fromJson("\"5\"", Range.TypeAdapter.INTEGER.type())).isInstanceOf(JsonParseException.class);
        assertThatThrownBy(() -> this.gson.fromJson("\"1-2-3\"", Range.TypeAdapter.INTEGER.type())).isInstanceOf(JsonParseException.class);
    }
}
