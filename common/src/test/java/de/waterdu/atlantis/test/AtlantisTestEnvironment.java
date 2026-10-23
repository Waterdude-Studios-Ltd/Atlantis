package de.waterdu.atlantis.test;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigFinder;
import de.waterdu.atlantis.file.ConfigurationHolder;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.mod.ModExtension;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.ListenerEvent;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import de.waterdu.atlantis.util.config.Leaderboard;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderProxy;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Installs a fresh {@link TestModLoader} as {@code Mods.modLoader()} before each test, with its own temporary config
 * directory, and resets Atlantis' global state afterwards. Tests can take the {@link TestModLoader} as a parameter.
 */
public final class AtlantisTestEnvironment implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AtlantisTestEnvironment.class);

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        var configDir = Files.createTempDirectory("atlantis-test");
        var loader = new TestModLoader(configDir);
        var extension = ModExtension.builder()
                .modId("atlantis_test_loader")
                .mainClass(AtlantisTestEnvironment.class)
                .proxy(ModExtension.Loader.ALL, () -> loader)
                .delegates(ModLoaderDelegate.class)
                .modLoader()
                .language(ModExtension.Language.JAVA)
                .priority(ModExtension.Priority.HIGHEST)
                .build();
        Mods.activateForTesting(extension);
        context.getStore(NAMESPACE).put(TestModLoader.class, loader);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Mods.resetForTesting();
        Bootstrapper.resetForTesting();
        ListenerEvent.clearForTesting();
        Leaderboard.resetForTesting();
        AtlantisPlaceholderProxy.resetForTesting();
        AtlantisConfig.resetForTesting();
        AtlantisConfigFinder.resetForTesting();
        ConfigurationHolder.resetForTesting();
        Storage.SERVER_STARTED.set(false);
        var loader = context.getStore(NAMESPACE).remove(TestModLoader.class, TestModLoader.class);
        if (loader != null) {
            deleteRecursively(loader.configDir());
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == TestModLoader.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(TestModLoader.class, TestModLoader.class);
    }

    private static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException | UncheckedIOException ignored) {
        }
    }
}
