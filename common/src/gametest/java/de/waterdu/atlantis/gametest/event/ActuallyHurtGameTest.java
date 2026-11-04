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

/**
 * Tests the {@link UEvent#ACTUALLY_HURT} event.
 */
public final class ActuallyHurtGameTest {

    /**
     * Prevents instantiation.
     */
    private ActuallyHurtGameTest() {}

    /**
     * Checks that the event fires with the damage amount and that the entity loses that much health.
     *
     * @param helper the helper of the running test
     */
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

    /**
     * Checks that the entity loses the damage amount that a listener sets.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void listenerCanChangeTheDamage(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, 2, 2, 2);
        listen(pig, payload -> payload.setAmount(5F));

        pig.hurt(helper.getLevel().damageSources().generic(), 1F);

        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth() - 5F, "health");
        helper.succeed();
    }

    /**
     * Checks that the entity keeps its full health when a listener cancels the event.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingKeepsHealth(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, 2, 2, 2);
        listen(pig, ActuallyHurtEventPayload::cancel);

        pig.hurt(helper.getLevel().damageSources().generic(), 4F);

        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "health");
        helper.succeed();
    }

    /**
     * Checks that the event also fires for a survival player and that a changed amount applies to it.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void appliesToPlayers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        player.setGameMode(GameType.SURVIVAL);
        listen(player, payload -> payload.setAmount(payload.amount() * 2F));

        player.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 2F);

        helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 4F, "health");
        helper.succeed();
    }

    /**
     * Listens for {@link UEvent#ACTUALLY_HURT} and gives the payloads of the given entity to the listener. The handler
     * always returns {@link InteractionResult#PASS}.
     *
     * @param entity the entity to listen for
     * @param listener receives each payload of the entity
     */
    private static void listen(LivingEntity entity, Consumer<ActuallyHurtEventPayload> listener) {
        Mods.modLoader().event(UEvent.ACTUALLY_HURT).listen(payload -> {
            if (payload.entity() == entity) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }
}
