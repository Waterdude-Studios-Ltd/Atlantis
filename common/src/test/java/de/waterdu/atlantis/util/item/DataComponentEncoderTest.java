package de.waterdu.atlantis.util.item;

import com.google.gson.JsonObject;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class DataComponentEncoderTest {

    private static DataComponentMap components() {
        return DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 16)
                .set(DataComponents.DAMAGE, 3)
                .set(DataComponents.RARITY, Rarity.EPIC)
                .set(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .set(DataComponents.CUSTOM_NAME, Component.literal("Name"))
                .build();
    }

    private static void assertSameComponents(DataComponentMap decoded) {
        var expected = components();
        assertThat(decoded.size()).isEqualTo(expected.size());
        for (var component : expected) {
            assertThat(decoded.get(component.type())).as(component.type().toString()).isEqualTo(component.value());
        }
    }

    @Test
    void tagRoundTrip() {
        CompoundTag tag = DataComponentEncoder.TAG.encode(components());

        assertThat(tag.getAllKeys()).contains("minecraft:max_stack_size", "minecraft:damage", "minecraft:rarity", "minecraft:unbreakable", "minecraft:custom_name");
        assertThat(tag.getInt("minecraft:max_stack_size")).isEqualTo(16);
        assertSameComponents(DataComponentEncoder.TAG.decode(tag));
    }

    @Test
    void jsonRoundTrip() {
        JsonObject json = DataComponentEncoder.JSON.encode(components());

        assertThat(json.get("minecraft:damage").getAsInt()).isEqualTo(3);
        assertThat(json.get("minecraft:rarity").getAsString()).isEqualTo("epic");
        assertSameComponents(DataComponentEncoder.JSON.decode(json));
    }

    @Test
    void decodeSkipsUnknownAndInvalidEntries() {
        var tag = new CompoundTag();
        tag.putInt("minecraft:damage", 3);
        tag.putInt("atlantis:not_a_component", 1);
        tag.put("minecraft:max_stack_size", StringTag.valueOf("not a number"));

        var decoded = DataComponentEncoder.TAG.decode(tag);

        assertThat(decoded.size()).isEqualTo(1);
        assertThat(decoded.get(DataComponents.DAMAGE)).isEqualTo(3);
    }

    @Test
    void emptyMapEncodesToEmptyStore() {
        assertThat(DataComponentEncoder.TAG.encode(DataComponentMap.EMPTY).isEmpty()).isTrue();
        assertThat(DataComponentEncoder.JSON.encode(DataComponentMap.EMPTY).size()).isZero();
    }
}
