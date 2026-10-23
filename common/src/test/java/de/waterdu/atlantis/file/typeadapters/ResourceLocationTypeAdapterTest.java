package de.waterdu.atlantis.file.typeadapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class ResourceLocationTypeAdapterTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationTypeAdapter())
            .create();

    @Test
    void roundTrips() {
        var location = ResourceLocation.fromNamespaceAndPath("atlantis", "test/path");

        assertThat(this.gson.toJson(location)).isEqualTo("\"atlantis:test/path\"");
        assertThat(this.gson.fromJson("\"atlantis:test/path\"", ResourceLocation.class)).isEqualTo(location);
    }

    @Test
    void lowercasesAndDefaultsToMinecraftNamespace() {
        var location = this.gson.fromJson("\"Diamond_Sword\"", ResourceLocation.class);

        assertThat(location).isEqualTo(ResourceLocation.withDefaultNamespace("diamond_sword"));
        assertThat(BuiltInRegistries.ITEM.get(location)).isSameAs(Items.DIAMOND_SWORD);
    }

    @Test
    void invalidLocationDeserializesToNull() {
        assertThat(this.gson.fromJson("\"not a location\"", ResourceLocation.class)).isNull();
    }
}
