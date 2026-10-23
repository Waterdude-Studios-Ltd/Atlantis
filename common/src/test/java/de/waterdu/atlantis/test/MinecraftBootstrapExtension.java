package de.waterdu.atlantis.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

final class MinecraftBootstrapExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
