package de.waterdu.atlantis.ext.item;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.util.item.NameProvider;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AtlantisTestEnvironment.class)
class ItemStackExtTest {

    private static DataComponentType<Suffix> SUFFIX;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SUFFIX = DataComponentType.<Suffix>builder().persistent(com.mojang.serialization.Codec.STRING.xmap(Suffix::new, Suffix::suffix)).build();
    }

    @Test
    void keyAndIdAreThoseOfTheItem() {
        var stack = new ItemStack(Items.DIAMOND, 3);

        assertThat(stack.key()).isEqualTo(Items.DIAMOND.key());
        assertThat(stack.id()).isEqualTo(Items.DIAMOND.id());
    }

    @Test
    void setDisplayNameAcceptsStringsTextAndComponents() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.setDisplayName("Gem");
        assertThat(stack.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Gem");

        stack.setDisplayName(Text.of("Shiny"));
        assertThat(stack.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Shiny");

        stack.setDisplayName(Component.literal("Rock"));
        assertThat(stack.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Rock");
        assertThat(stack.getHoverName().getString()).isEqualTo("Rock");
    }

    @Test
    void setDisplayNameTranslatesColourCodes() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.setDisplayName("&aGreen");

        assertThat(stack.get(DataComponents.ITEM_NAME).getString()).isEqualTo("§aGreen");
    }

    @Test
    void setDisplayNameWithPlaceholdersLeavesTextWhenNoProviderIsSet() {
        var varargs = new ItemStack(Items.DIAMOND);
        var collection = new ItemStack(Items.DIAMOND);

        varargs.setDisplayNameWithPlaceholders("Hello %player_name%", "context");
        collection.setDisplayNameWithPlaceholders("Hello %player_name%", List.of((Object) "context"));

        assertThat(varargs.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Hello %player_name%");
        assertThat(collection.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Hello %player_name%");
    }

    @Test
    void computeComponentIfAbsentStoresTheDefaultOnce() {
        var stack = new ItemStack(Items.DIAMOND);
        var calls = new AtomicInteger();

        var first = stack.computeComponentIfAbsent(DataComponents.CUSTOM_MODEL_DATA, () -> {
            calls.incrementAndGet();
            return new CustomModelData(7);
        });
        var second = stack.computeComponentIfAbsent(DataComponents.CUSTOM_MODEL_DATA, () -> {
            calls.incrementAndGet();
            return new CustomModelData(8);
        });

        assertThat(first).isEqualTo(new CustomModelData(7));
        assertThat(second).isEqualTo(new CustomModelData(7));
        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(7));
        assertThat(calls).hasValue(1);
    }

    @Test
    void componentScopeUpdatesPresentComponents() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, data -> new CustomModelData(data.value() + 1));

        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(2));
    }

    @Test
    void componentScopeWithoutDefaultLeavesAbsentComponentsAbsent() {
        var stack = new ItemStack(Items.DIAMOND);
        var called = new AtomicInteger();

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, data -> {
            called.incrementAndGet();
            return data;
        });

        assertThat(stack.has(DataComponents.CUSTOM_MODEL_DATA)).isFalse();
        assertThat(called).hasValue(0);
    }

    @Test
    void componentScopeWithDefaultStartsFromTheDefault() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> new CustomModelData(10), data -> new CustomModelData(data.value() * 2));

        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(20));
    }

    @Test
    void componentScopeCanRemoveAComponent() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> null, __ -> null);

        assertThat(stack.has(DataComponents.CUSTOM_MODEL_DATA)).isFalse();
    }

    @Test
    void componentScopeWithNullDefaultAndNullResultChangesNothing() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> null, __ -> null);

        assertThat(stack.has(DataComponents.CUSTOM_MODEL_DATA)).isFalse();
        assertThat(stack.getComponentsPatch().isEmpty()).isTrue();
    }

    @Test
    void addToNameAppliesTheNameProviderComponent() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(SUFFIX, new Suffix(" +1"));
        var name = new AtomicReference<Component>(Component.literal("Diamond"));

        stack.addToName(SUFFIX, Item.TooltipContext.EMPTY, name, TooltipFlag.NORMAL);

        assertThat(name.get().getString()).isEqualTo("Diamond +1");
    }

    @Test
    void addToNameWithoutTheComponentKeepsTheName() {
        var stack = new ItemStack(Items.DIAMOND);
        var original = Component.literal("Diamond");
        var name = new AtomicReference<Component>(original);

        stack.addToName(SUFFIX, Item.TooltipContext.EMPTY, name, TooltipFlag.NORMAL);

        assertThat(name.get()).isSameAs(original);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitizeComponentsDropsANullKeyedPatchEntry() throws ReflectiveOperationException {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(3));
        var patchField = PatchedDataComponentMap.class.getDeclaredField("patch");
        patchField.setAccessible(true);
        var patch = (java.util.Map<Object, Optional<?>>) patchField.get(stack.getComponents());
        patch.put(null, Optional.of("corrupt"));

        stack.sanitizeComponents();

        assertThat(patch).doesNotContainKey(null);
        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(3));
    }

    @Test
    void configurableKeepsCountAndDamage() {
        var stack = new ItemStack(Items.DIAMOND_SWORD, 2);
        stack.setDamageValue(5);

        var configurable = stack.configurable().originalString();

        assertThat(configurable).startsWith("minecraft:diamond_sword count:2 damage:5");
    }

    private record Suffix(String suffix) implements NameProvider {

        @Override
        public void addToName(Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
            name.set(name.get().copy().append(this.suffix));
        }
    }
}
