package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.ActuallyHurtEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ActuallyHurtGameTest {

    private ActuallyHurtGameTest() {}

    @AtlantisGameTest
    public static void firesWithTheDamageTakenFromHealth(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, 2, 2, 2);
        var seen = new AtomicReference<Float>();
        listen(pig, payload -> seen.set(payload.amount()));

        pig.hurt(helper.getLevel().damageSources().generic(), 3F);

        helper.assertValueEqual(seen.get(), 3F, "ACTUALLY_HURT amount");
        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth() - 3F, "health");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void listenerCanChangeTheDamage(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, 2, 2, 2);
        listen(pig, payload -> payload.setAmount(5F));

        pig.hurt(helper.getLevel().damageSources().generic(), 1F);

        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth() - 5F, "health");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void cancellingKeepsHealth(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, 2, 2, 2);
        listen(pig, ActuallyHurtEventPayload::cancel);

        pig.hurt(helper.getLevel().damageSources().generic(), 4F);

        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "health");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void appliesToPlayers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        player.setGameMode(GameType.SURVIVAL);
        listen(player, payload -> payload.setAmount(payload.amount() * 2F));

        player.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 2F);

        helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 4F, "health");
        helper.succeed();
    }

    private static void listen(LivingEntity entity, Consumer<ActuallyHurtEventPayload> listener) {
        Mods.modLoader().event(UEvent.ACTUALLY_HURT).listen(payload -> {
            if (payload.entity() == entity) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }
}
