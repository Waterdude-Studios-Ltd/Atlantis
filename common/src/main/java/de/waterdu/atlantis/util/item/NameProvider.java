package de.waterdu.atlantis.util.item;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Changes the name of an item that holds it as a data component.
 */
public interface NameProvider {

    /**
     * @param name the current name. Set it to change the name.
     */
    void addToName(Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag);

    /**
     * Lets the name provider in the component of {@code self} change the name. If {@code self} does not have the
     * component, nothing happens.
     *
     * @param <T> the type of the name provider
     * @param self the holder of the component
     * @param component the component type of the name provider
     * @param context the tooltip context
     * @param name the current name. The provider can set it to change the name.
     * @param tooltipFlag the tooltip flag
     */
    static <T extends NameProvider> void addToName(DataComponentHolder self, DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        var t = self.get(component);
        if (t != null) {
            t.addToName(context, name, tooltipFlag);
        }
    }
}
