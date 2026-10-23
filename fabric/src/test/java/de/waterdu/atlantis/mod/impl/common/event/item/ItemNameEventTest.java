package de.waterdu.atlantis.mod.impl.common.event.item;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AtlantisTestEnvironment.class)
class ItemNameEventTest {

    private static DataComponentType<Suffix> SUFFIX;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SUFFIX = DataComponentType.<Suffix>builder().persistent(com.mojang.serialization.Codec.STRING.xmap(Suffix::new, Suffix::suffix)).build();
    }

    @Test
    void itemNameListenerChangesTheTooltipName() {
        Mods.modLoader().event(UEvent.ITEM_NAME).listen(payload -> {
            payload.name().set(Component.literal("Renamed"));
            return InteractionResult.PASS;
        });

        var lines = new ItemStack(Items.DIAMOND).getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);

        assertThat(lines.getFirst().getString()).isEqualTo("Renamed");
    }

    @Test
    void itemNameEventCarriesTheTooltipArguments() {
        var payloads = new ArrayList<ItemNameEventPayload>();
        Mods.modLoader().event(UEvent.ITEM_NAME).listen(payload -> {
            payloads.add(payload);
            return InteractionResult.PASS;
        });
        var stack = new ItemStack(Items.DIAMOND);

        var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);

        assertThat(payloads).hasSize(1);
        var payload = payloads.getFirst();
        assertThat(payload.stack()).isSameAs(stack);
        assertThat(payload.player()).isNull();
        assertThat(payload.tooltipFlag()).isSameAs(TooltipFlag.ADVANCED);
        assertThat(lines.getFirst().getString()).isEqualTo(stack.getHoverName().getString());
    }

    @Test
    void payloadAddToNameAppliesANameProvider() {
        Mods.modLoader().event(UEvent.ITEM_NAME).listen(payload -> {
            payload.addToName(SUFFIX);
            return InteractionResult.PASS;
        });
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(SUFFIX, new Suffix(" [x]"));

        var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);

        assertThat(lines.getFirst().getString()).isEqualTo("Diamond [x]");
    }

    private record Suffix(String suffix) implements de.waterdu.atlantis.util.item.NameProvider {

        @Override
        public void addToName(Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
            name.set(name.get().copy().append(this.suffix));
        }
    }
}
