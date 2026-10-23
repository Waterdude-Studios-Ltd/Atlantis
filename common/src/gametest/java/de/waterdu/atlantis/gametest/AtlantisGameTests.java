package de.waterdu.atlantis.gametest;

import de.waterdu.atlantis.gametest.command.AtlantisCommandGameTest;
import de.waterdu.atlantis.gametest.event.ActuallyHurtGameTest;
import de.waterdu.atlantis.gametest.event.VillagerTradeGameTest;
import de.waterdu.atlantis.gametest.event.ItemNameGameTest;
import de.waterdu.atlantis.gametest.event.LootTableGameTest;
import de.waterdu.atlantis.gametest.event.PacketInterceptGameTest;
import de.waterdu.atlantis.gametest.event.PlayerConnectionGameTest;
import de.waterdu.atlantis.gametest.event.RegistryInjectGameTest;
import de.waterdu.atlantis.gametest.event.ServerTickGameTest;
import de.waterdu.atlantis.gametest.ext.PlayerExtGameTest;
import de.waterdu.atlantis.gametest.file.PlayerDataGameTest;
import de.waterdu.atlantis.gametest.level.ChunkLoaderGameTest;
import de.waterdu.atlantis.gametest.level.SchematicGameTest;
import de.waterdu.atlantis.gametest.loader.ModLoaderDelegateGameTest;
import de.waterdu.atlantis.gametest.occasion.OccasionGameTest;
import de.waterdu.atlantis.gametest.permission.PermissionsGameTest;
import de.waterdu.atlantis.gametest.registry.RegistryExtGameTest;
import de.waterdu.atlantis.gametest.ui.AtlantisUIGameTest;
import de.waterdu.atlantis.gametest.util.ItemUtilsGameTest;
import de.waterdu.atlantis.gametest.util.LevelUtilsGameTest;
import de.waterdu.atlantis.gametest.util.ServerUtilsGameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@link TestFunction}s for every {@link AtlantisGameTest} in {@link #TEST_CLASSES}. Each loader's test
 * mod returns these from a {@code @GameTestGenerator} method.
 */
public final class AtlantisGameTests {

    public static final String NAMESPACE = "atlantis_test";

    private static final List<Class<?>> TEST_CLASSES = List.of(
            SmokeGameTest.class,
            ActuallyHurtGameTest.class,
            VillagerTradeGameTest.class,
            PlayerExtGameTest.class,
            LootTableGameTest.class,
            ItemNameGameTest.class,
            PacketInterceptGameTest.class,
            RegistryInjectGameTest.class,
            ServerTickGameTest.class,
            PlayerConnectionGameTest.class,
            ModLoaderDelegateGameTest.class,
            PermissionsGameTest.class,
            AtlantisCommandGameTest.class,
            RegistryExtGameTest.class,
            AtlantisUIGameTest.class,
            ChunkLoaderGameTest.class,
            SchematicGameTest.class,
            OccasionGameTest.class,
            ItemUtilsGameTest.class,
            ServerUtilsGameTest.class,
            LevelUtilsGameTest.class,
            PlayerDataGameTest.class
    );

    private AtlantisGameTests() {}

    public static List<TestFunction> collect() {
        GameTestLevels.registerOnce();
        var functions = new ArrayList<TestFunction>();
        for (var testClass : TEST_CLASSES) {
            var methods = new ArrayList<>(List.of(testClass.getDeclaredMethods()));
            methods.sort(Comparator.comparing(Method::getName));
            for (var method : methods) {
                var test = method.getAnnotation(AtlantisGameTest.class);
                if (test != null) {
                    functions.add(toTestFunction(testClass, method, test));
                }
            }
        }
        return functions;
    }

    private static TestFunction toTestFunction(Class<?> testClass, Method method, AtlantisGameTest test) {
        if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1 || method.getParameterTypes()[0] != GameTestHelper.class) {
            throw new IllegalStateException("@AtlantisGameTest method " + testClass.getName() + "#" + method.getName() + " must be static and take only a GameTestHelper");
        }
        method.setAccessible(true);
        var name = (testClass.getSimpleName() + "." + method.getName()).toLowerCase(Locale.ROOT);
        return new TestFunction(
                test.batch(),
                name,
                NAMESPACE + ":" + test.template(),
                Rotation.NONE,
                test.timeoutTicks(),
                test.setupTicks(),
                test.required(),
                false,
                1,
                1,
                false,
                helper -> invoke(method, helper)
        );
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
