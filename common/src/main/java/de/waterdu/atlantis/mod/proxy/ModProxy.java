package de.waterdu.atlantis.mod.proxy;

import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;

import java.util.function.Consumer;

/**
 * Connects Atlantis to a mod extension. Each method has a default that does nothing.
 */
public interface ModProxy {

    /**
     * Sets up the proxy when its extension activates. Does nothing by default.
     */
    default void setup() {}

    /**
     * Registers Gson type adapters for the configs of every mod. Does nothing by default.
     *
     * @param gsonBuilder the builder to register the type adapters with
     */
    default void registerTypeAdapters(GsonBuilder gsonBuilder) {}

    /**
     * Registers {@link de.waterdu.atlantis.util.config.Configurable} adapters with Gson. Does nothing by default.
     *
     * @param gson the builder to register the adapters with
     */
    default void registerConfigurableContexts(GsonBuilder gson) {}

    /**
     * Gives the event buses of the mod to the consumer. Does nothing by default.
     *
     * @param consumer accepts each event bus
     */
    default void registerEventBus(Consumer<UEventBus<?, ?>> consumer) {}

    /**
     * Creates the preprocessors that make the item stack of a {@link ConfigurableItemStack} from a directive, such as
     * {@code #sprite}.
     *
     * @return the preprocessors. By default, this is empty.
     */
    default ConfigurableItemStack.Preprocessor[] makeItemPreprocessors() {
        return new ConfigurableItemStack.Preprocessor[0];
    }

    /**
     * Creates the processors that change the item stack of a {@link ConfigurableItemStack} from a key-value
     * argument.
     *
     * @return the processors. By default, this is empty.
     */
    default ConfigurableItemStack.Processor[] makeItemProcessors() {
        return new ConfigurableItemStack.Processor[0];
    }
}
