package de.waterdu.atlantis.mod.impl.common.event.item;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.item.NameProvider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public record ItemNameEventPayload(
        ItemStack stack, 
        @Nullable Player player,
        Item.TooltipContext context,
        AtomicReference<Component> name,
        TooltipFlag tooltipFlag
) implements EventPayload {

    @Override
    public Key<?> type() {
        return UEvent.ITEM_NAME;
    }

    public <T extends NameProvider> void addToName(DataComponentType<T> component) {
        this.stack().addToName(component, this.context, this.name, this.tooltipFlag);
    }
}
