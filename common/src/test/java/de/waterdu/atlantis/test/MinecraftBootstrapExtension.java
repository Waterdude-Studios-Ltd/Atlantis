package de.waterdu.atlantis.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Bootstraps the Minecraft registries once before the tests of a class run.
 */
final class MinecraftBootstrapExtension implements BeforeAllCallback {

    /**
     * Detects the game version and runs the vanilla {@link Bootstrap}.
     *
     * @param context the context of the test class
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
