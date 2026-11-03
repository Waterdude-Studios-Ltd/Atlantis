package de.waterdu.atlantis.util.placeholder;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.ReflectionUtils;
import de.waterdu.atlantis.util.placeholder.impl.FPAPI;
import de.waterdu.atlantis.util.placeholder.impl.NoProvider;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;
import de.waterdu.atlantis.util.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Replaces placeholders in text through the selected {@link PlaceholderProvider}. If FPAPI is present, it is added as
 * a provider.
 */
@Bootstrapped
public final class AtlantisPlaceholderProxy {

    private static final AtomicReference<PlaceholderProvider> PROVIDER = new AtomicReference<>(NoProvider.INSTANCE);
    private static final List<PlaceholderProvider> PROVIDERS = new ArrayList<>();

    static {
        if (ReflectionUtils.doesClassExist("com.envyful.papi.forge.ForgePlaceholderAPI")) {
            AtlantisPlaceholderProxy.addPlaceholderProvider(new FPAPI());
        }
    }

    /**
     * Always throws, because this class only has static members.
     */
    private AtlantisPlaceholderProxy() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Sets the first added provider as the current provider. If no provider was added, the current provider does not
     * change.
     */
    @Entrypoint
    @SuppressWarnings("unused")
    private static void bootstrap() {
        for (var provider : AtlantisPlaceholderProxy.PROVIDERS) {
            AtlantisPlaceholderProxy.setProvider(provider);
            break;
        }
    }

    /**
     * Sets the current provider back to {@link NoProvider} and removes all added providers.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        AtlantisPlaceholderProxy.PROVIDER.set(NoProvider.INSTANCE);
        AtlantisPlaceholderProxy.PROVIDERS.clear();
    }

    /**
     * Atlantis uses only the first provider. It selects the provider at bootstrap, so add providers before that.
     */
    public static void addPlaceholderProvider(PlaceholderProvider provider) {
        AtlantisPlaceholderProxy.PROVIDERS.add(provider);
    }

    /**
     * Listeners of the placeholder provider event can replace the provider.
     * Extensions that are registered with the old provider are not moved.
     */
    public static void setProvider(PlaceholderProvider provider) {
        AtlantisPlaceholderProxy.PROVIDER.set(Mods.events().emitSetPlaceholderProvider(provider).provider());
    }

    /**
     * Registers the extension with the current provider. Without a placeholder mod, this does nothing.
     * Register extensions after bootstrap, because Atlantis selects the provider then.
     */
    public static <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        AtlantisPlaceholderProxy.PROVIDER.get().register(id, name, extension, type);
    }

    /**
     * Replaces the placeholders in the text with the current provider.
     *
     * @param <T> the type of the context object
     * @param context the context object that the placeholders read from
     * @param text the text that contains the placeholders
     * @return the text with the placeholders replaced
     */
    public static <T> Text replace(T context, Text text) {
        return AtlantisPlaceholderProxy.PROVIDER.get().replace(context, text);
    }

    /**
     * Replaces the placeholders in the text for a player with the current provider.
     *
     * @param uuid the UUID of the player
     * @param text the text that contains the placeholders
     * @return the text with the placeholders replaced
     */
    public static Text replace(UUID uuid, Text text) {
        return AtlantisPlaceholderProxy.PROVIDER.get().replace(uuid, text);
    }
}
