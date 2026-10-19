package de.waterdu.atlantis.mod.proxy;

import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;

import java.util.function.Consumer;

public interface ModProxy {

    default void setup() {}

    default void registerTypeAdapters(GsonBuilder gsonBuilder) {}

    default void registerConfigurableContexts(GsonBuilder gson) {}

    default void registerEventBus(Consumer<UEventBus<?, ?>> consumer) {}

    default ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return new ConfigurableItemStack.Preprocessor[0];
    }

    default ConfigurableItemStack.Processor[] makeItemProcessors() {
        return new ConfigurableItemStack.Processor[0];
    }
}
