package de.waterdu.atlantis.file.typeadapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import de.waterdu.atlantis.util.java.collection.CompactJsonArrayList;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class NumberListTypeAdapterTest {

    private final Gson gson = gson();

    private static Gson gson() {
        var builder = new GsonBuilder();
        for (var adapter : NumberListTypeAdapter.ADAPTERS) {
            builder.registerTypeAdapter(adapter.type(), adapter);
        }
        return builder.create();
    }

    private <T extends Number> List<T> fromJson(NumberListTypeAdapter<T> adapter, String json) {
        return this.gson.fromJson(json, adapter.type());
    }

    private <T extends Number> String toJson(NumberListTypeAdapter<T> adapter, List<T> list) {
        return this.gson.toJson(list, adapter.type());
    }

    @Test
    void parsesCommaSeparatedValuesAndRanges() {
        var list = this.fromJson(NumberListTypeAdapter.INTEGER, "\"1,3-5,9\"");

        assertThat(list).isInstanceOf(CompactJsonArrayList.class).containsExactly(1, 3, 4, 5, 9);
    }

    @Test
    void parsesJsonArrays() {
        var list = this.fromJson(NumberListTypeAdapter.LONG, "[1, 2, 10]");

        assertThat(list).isNotInstanceOf(CompactJsonArrayList.class).containsExactly(1L, 2L, 10L);
    }

    @Test
    void skipsInvalidEntries() {
        assertThat(this.fromJson(NumberListTypeAdapter.INTEGER, "\"1,x,3\"")).containsExactly(1, 3);
    }

    @Test
    void otherJsonIsAnEmptyList() {
        assertThat(this.fromJson(NumberListTypeAdapter.INTEGER, "{}")).isEmpty();
    }

    @Test
    void compactListsSerializeAsSortedRanges() {
        var list = new CompactJsonArrayList<>(new ArrayList<>(List.of(9, 1, 2, 3, 5)));

        assertThat(this.toJson(NumberListTypeAdapter.INTEGER, list)).isEqualTo("\"1-3,5,9\"");
    }

    @Test
    void everyRunIsCompacted() {
        var list = new CompactJsonArrayList<>(new ArrayList<>(List.of(1, 2, 3, 5, 6, 8, 9, 10)));

        assertThat(this.toJson(NumberListTypeAdapter.INTEGER, list)).isEqualTo("\"1-3,5-6,8-10\"");
    }

    @Test
    void compactRoundTrip() {
        var json = "\"1-3,7,10-12\"";

        assertThat(this.toJson(NumberListTypeAdapter.SHORT, this.fromJson(NumberListTypeAdapter.SHORT, json))).isEqualTo(json);
        assertThat(this.toJson(NumberListTypeAdapter.BYTE, this.fromJson(NumberListTypeAdapter.BYTE, json))).isEqualTo(json);
        assertThat(this.toJson(NumberListTypeAdapter.BIG_INTEGER, this.fromJson(NumberListTypeAdapter.BIG_INTEGER, json))).isEqualTo(json);
    }

    @Test
    void atomicCompactListsSerialize() {
        var ints = new CompactJsonArrayList<>(new ArrayList<>(List.of(new AtomicInteger(3), new AtomicInteger(1), new AtomicInteger(2))));
        var longs = new CompactJsonArrayList<>(new ArrayList<>(List.of(new AtomicLong(5), new AtomicLong(1))));

        assertThat(this.toJson(NumberListTypeAdapter.ATOMIC_INTEGER, ints)).isEqualTo("\"1-3\"");
        assertThat(this.toJson(NumberListTypeAdapter.ATOMIC_LONG, longs)).isEqualTo("\"1,5\"");
    }

    @Test
    void atomicRangesParse() {
        assertThat(this.fromJson(NumberListTypeAdapter.ATOMIC_INTEGER, "\"2-4\"")).extracting(AtomicInteger::get).containsExactly(2, 3, 4);
    }

    @Test
    void plainListsSerializeAsArrays() {
        assertThat(this.toJson(NumberListTypeAdapter.INTEGER, List.of(3, 1))).isEqualTo("[3,1]");
        var expected = new JsonArray();
        expected.add(new JsonPrimitive(BigInteger.TEN));
        assertThat(this.gson.toJsonTree(List.of(BigInteger.TEN), NumberListTypeAdapter.BIG_INTEGER.type())).isEqualTo(expected);
    }
}
