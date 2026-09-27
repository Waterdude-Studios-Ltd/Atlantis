package de.waterdu.atlantis.util.item;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.concurrent.atomic.AtomicReference;

public interface NameProvider {

    void addToName(Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag);

    static <T extends NameProvider> void addToName(DataComponentHolder self, DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        var t = self.get(component);
        if (t != null) {
            t.addToName(context, name, tooltipFlag);
        }
    }
}
