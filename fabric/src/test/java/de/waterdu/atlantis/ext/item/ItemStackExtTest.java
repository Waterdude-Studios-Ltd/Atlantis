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

/**
 * Tests the methods that {@link ItemStackExt} adds to {@link ItemStack}.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class ItemStackExtTest {

    private static DataComponentType<Suffix> SUFFIX;

    /**
     * Bootstraps Minecraft once before the tests and creates the {@link Suffix} component type.
     */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SUFFIX = DataComponentType.<Suffix>builder().persistent(com.mojang.serialization.Codec.STRING.xmap(Suffix::new, Suffix::suffix)).build();
    }

    /**
     * Checks that the key and the ID of a stack are those of its item.
     */
    @Test
    void keyAndIdAreThoseOfTheItem() {
        var stack = new ItemStack(Items.DIAMOND, 3);

        assertThat(stack.key()).isEqualTo(Items.DIAMOND.key());
        assertThat(stack.id()).isEqualTo(Items.DIAMOND.id());
    }

    /**
     * Checks that {@code setDisplayName} sets the item name from a string, a {@link Text} or a {@link Component}.
     */
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

    /**
     * Checks that {@code setDisplayName} changes {@code &} color codes to {@code §} codes.
     */
    @Test
    void setDisplayNameTranslatesColourCodes() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.setDisplayName("&aGreen");

        assertThat(stack.get(DataComponents.ITEM_NAME).getString()).isEqualTo("§aGreen");
    }

    /**
     * Checks that both forms of {@code setDisplayNameWithPlaceholders} keep the placeholders as text when no
     * placeholder provider is set.
     */
    @Test
    void setDisplayNameWithPlaceholdersLeavesTextWhenNoProviderIsSet() {
        var varargs = new ItemStack(Items.DIAMOND);
        var collection = new ItemStack(Items.DIAMOND);

        varargs.setDisplayNameWithPlaceholders("Hello %player_name%", "context");
        collection.setDisplayNameWithPlaceholders("Hello %player_name%", List.of((Object) "context"));

        assertThat(varargs.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Hello %player_name%");
        assertThat(collection.get(DataComponents.ITEM_NAME).getString()).isEqualTo("Hello %player_name%");
    }

    /**
     * Checks that {@code computeComponentIfAbsent} calls the supplier only when the component is absent, and stores
     * its value.
     */
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

    /**
     * Checks that {@code componentScope} replaces a present component with the result of the function.
     */
    @Test
    void componentScopeUpdatesPresentComponents() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, data -> new CustomModelData(data.value() + 1));

        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(2));
    }

    /**
     * Checks that {@code componentScope} without a default does not call the function when the component is absent.
     */
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

    /**
     * Checks that {@code componentScope} with a default passes the default to the function when the component is
     * absent.
     */
    @Test
    void componentScopeWithDefaultStartsFromTheDefault() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> new CustomModelData(10), data -> new CustomModelData(data.value() * 2));

        assertThat(stack.get(DataComponents.CUSTOM_MODEL_DATA)).isEqualTo(new CustomModelData(20));
    }

    /**
     * Checks that {@code componentScope} removes a present component when the function returns {@code null}.
     */
    @Test
    void componentScopeCanRemoveAComponent() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> null, __ -> null);

        assertThat(stack.has(DataComponents.CUSTOM_MODEL_DATA)).isFalse();
    }

    /**
     * Checks that {@code componentScope} leaves the component patch empty when the default and the result are
     * {@code null}.
     */
    @Test
    void componentScopeWithNullDefaultAndNullResultChangesNothing() {
        var stack = new ItemStack(Items.DIAMOND);

        stack.componentScope(DataComponents.CUSTOM_MODEL_DATA, () -> null, __ -> null);

        assertThat(stack.has(DataComponents.CUSTOM_MODEL_DATA)).isFalse();
        assertThat(stack.getComponentsPatch().isEmpty()).isTrue();
    }

    /**
     * Checks that {@code addToName} lets the {@link NameProvider} component of the stack change the name.
     */
    @Test
    void addToNameAppliesTheNameProviderComponent() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(SUFFIX, new Suffix(" +1"));
        var name = new AtomicReference<Component>(Component.literal("Diamond"));

        stack.addToName(SUFFIX, Item.TooltipContext.EMPTY, name, TooltipFlag.NORMAL);

        assertThat(name.get().getString()).isEqualTo("Diamond +1");
    }

    /**
     * Checks that {@code addToName} keeps the same name when the stack does not have the component.
     */
    @Test
    void addToNameWithoutTheComponentKeepsTheName() {
        var stack = new ItemStack(Items.DIAMOND);
        var original = Component.literal("Diamond");
        var name = new AtomicReference<Component>(original);

        stack.addToName(SUFFIX, Item.TooltipContext.EMPTY, name, TooltipFlag.NORMAL);

        assertThat(name.get()).isSameAs(original);
    }

    /**
     * Checks that {@code sanitizeComponents} removes a patch entry with a {@code null} key and keeps the other
     * components.
     *
     * @throws ReflectiveOperationException if the patch field of {@link PatchedDataComponentMap} cannot be read
     */
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

    /**
     * Checks that {@code configurable} writes the count and the damage of the stack after the item ID.
     */
    @Test
    void configurableKeepsCountAndDamage() {
        var stack = new ItemStack(Items.DIAMOND_SWORD, 2);
        stack.setDamageValue(5);

        var configurable = stack.configurable().originalString();

        assertThat(configurable).startsWith("minecraft:diamond_sword count:2 damage:5");
    }

    /**
     * Adds a fixed suffix to the name of an item.
     *
     * @param suffix the text to append to the name
     */
    private record Suffix(String suffix) implements NameProvider {

        /**
         * Appends the suffix to the name.
         *
         * @param context the tooltip context
         * @param name the name to change
         * @param tooltipFlag the tooltip flag
         */
        @Override
        public void addToName(Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
            name.set(name.get().copy().append(this.suffix));
        }
    }
}
