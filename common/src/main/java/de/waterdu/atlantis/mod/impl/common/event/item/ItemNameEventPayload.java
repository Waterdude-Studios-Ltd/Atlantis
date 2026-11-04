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
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the name of an item stack while its tooltip is made. Listeners can change the name.
 *
 * @param stack the item stack
 * @param player the player who sees the name, or {@code null} if there is none
 * @param context the tooltip context
 * @param name holds the name. Set it to change the name.
 * @param tooltipFlag the tooltip flag
 */
public record ItemNameEventPayload(
        ItemStack stack, 
        @Nullable Player player,
        Item.TooltipContext context,
        AtomicReference<Component> name,
        TooltipFlag tooltipFlag
) implements EventPayload {

    /**
     * @return {@link UEvent#ITEM_NAME}
     */
    @Override
    public Key<?> type() {
        return UEvent.ITEM_NAME;
    }

    /**
     * Lets the value of the component on the stack change {@link #name()}. Does nothing if the stack does not have the
     * component.
     */
    public <T extends NameProvider> void addToName(DataComponentType<T> component) {
        this.stack().addToName(component, this.context, this.name, this.tooltipFlag);
    }
}
