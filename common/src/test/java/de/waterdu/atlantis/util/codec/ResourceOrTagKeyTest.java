package de.waterdu.atlantis.util.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class ResourceOrTagKeyTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(ResourceOrTagKey.class, new ResourceOrTagKey.TypeAdapter())
            .create();

    @Test
    void parsesResourceKeys() {
        var key = ResourceOrTagKey.create("Minecraft:Diamond", Registries.ITEM);

        assertThat(key.asResourceKey()).isEqualTo(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("diamond")));
        assertThat(key.asTagKey()).isNull();
        assertThat(key.asEither().left()).contains(key.asResourceKey());
    }

    @Test
    void parsesTagKeys() {
        var key = ResourceOrTagKey.create("#minecraft:logs", Registries.ITEM);

        assertThat(key.asTagKey()).isEqualTo(ItemTags.LOGS);
        assertThat(key.asResourceKey()).isNull();
        assertThat(key.asEither().right()).contains(ItemTags.LOGS);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void contextIsOnlyProvidedOnce() {
        ResourceOrTagKey<Item> key = ResourceOrTagKey.create("minecraft:diamond");

        key.provideContext(Registries.ITEM).provideContext((ResourceKey) Registries.BLOCK);

        assertThat(key.asResourceKey()).isEqualTo(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("diamond")));
    }

    @Test
    void createFromKeys() {
        var resource = ResourceOrTagKey.create(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("diamond")));
        var tag = ResourceOrTagKey.create(ItemTags.LOGS);

        assertThat(resource.asEither().left()).isPresent();
        assertThat(tag.asEither().right()).contains(ItemTags.LOGS);
    }

    @Test
    void matchesReferenceHolders() {
        var diamond = ResourceOrTagKey.create("minecraft:diamond", Registries.ITEM);

        assertThat(diamond.is(BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND))).isTrue();
        assertThat(diamond.is(BuiltInRegistries.ITEM.wrapAsHolder(Items.EMERALD))).isFalse();
    }

    @Test
    void typeAdapterRoundTripsResourceKeys() {
        var json = this.gson.toJson(ResourceOrTagKey.create("minecraft:diamond", Registries.ITEM));
        ResourceOrTagKey<Item> parsed = this.gson.fromJson(json, ResourceOrTagKey.class);

        assertThat(json).isEqualTo("\"minecraft:diamond\"");
        assertThat(parsed.provideContext(Registries.ITEM).asResourceKey().location()).isEqualTo(ResourceLocation.withDefaultNamespace("diamond"));
    }

    @Test
    void typeAdapterRoundTripsTagKeys() {
        var fromString = this.gson.toJson(ResourceOrTagKey.create("#minecraft:logs", Registries.ITEM));
        var fromTagKey = this.gson.toJson(ResourceOrTagKey.create(ItemTags.LOGS));
        ResourceOrTagKey<Item> parsed = this.gson.fromJson(fromTagKey, ResourceOrTagKey.class);

        assertThat(fromString).isEqualTo("\"#minecraft:logs\"");
        assertThat(fromTagKey).isEqualTo("\"#minecraft:logs\"");
        assertThat(parsed.provideContext(Registries.ITEM).asTagKey()).isEqualTo(ItemTags.LOGS);
    }
}
