package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public final class ItemNameGameTest {

    private ItemNameGameTest() {}

    @AtlantisGameTest
    public static void listenerRenamesTheTooltip(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var stack = new ItemStack(Items.DIAMOND);
        Mods.modLoader().event(UEvent.ITEM_NAME).listen(payload -> {
            if (payload.stack() == stack) {
                payload.name().set(Component.literal("Renamed"));
            }
            return InteractionResult.PASS;
        });

        var lines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL);

        helper.assertValueEqual(lines.getFirst().getString(), "Renamed", "tooltip name");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void payloadCarriesThePlayer(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var stack = new ItemStack(Items.EMERALD);
        var seen = new Object[1];
        Mods.modLoader().event(UEvent.ITEM_NAME).listen(payload -> {
            if (payload.stack() == stack) {
                seen[0] = payload.player();
            }
            return InteractionResult.PASS;
        });

        var lines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL);

        helper.assertTrue(seen[0] == player, "payload player is not the tooltip player");
        helper.assertValueEqual(lines.getFirst().getString(), stack.getHoverName().getString(), "tooltip name");
        helper.succeed();
    }
}
