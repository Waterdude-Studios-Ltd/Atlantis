package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerTickGameTest {

    private static final int TICKS = 4;

    private ServerTickGameTest() {}

    private static int tick(String event) {
        return Integer.parseInt(event.substring(event.indexOf(':') + 1));
    }

    @AtlantisGameTest
    public static void preAndPostFireOncePerTickInOrder(GameTestHelper helper) {
        var events = new ArrayList<String>();
        var recording = new AtomicBoolean(true);
        Mods.modLoader().event(UEvent.SERVER_TICK_PRE).listen(payload -> {
            if (recording.get()) {
                events.add("pre:" + payload.server().getTickCount());
            }
            return InteractionResult.PASS;
        });
        Mods.modLoader().event(UEvent.SERVER_TICK_POST).listen(payload -> {
            if (recording.get()) {
                events.add("post:" + payload.server().getTickCount());
            }
            return InteractionResult.PASS;
        });

        helper.runAfterDelay(TICKS + 1, () -> {
            recording.set(false);
            var start = events.getFirst().startsWith("post") ? 1 : 0;
            var recorded = events.subList(start, start + TICKS * 2);
            var first = tick(recorded.getFirst());
            for (var i = 0; i < TICKS; i++) {
                var pre = recorded.get(i * 2);
                var post = recorded.get(i * 2 + 1);
                helper.assertTrue(pre.startsWith("pre:") && post.startsWith("post:"), "events are not PRE then POST: " + recorded);
                helper.assertValueEqual(tick(pre), first + i, "tick of PRE " + i + " in " + recorded);
                helper.assertTrue(tick(post) - tick(pre) <= 1 && tick(post) >= tick(pre), "POST is not in the same tick: " + recorded);
            }
            helper.succeed();
        });
    }
}
