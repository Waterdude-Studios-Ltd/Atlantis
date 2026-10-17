package de.waterdu.atlantis.util.placeholder;

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

@Bootstrapped
public final class AtlantisPlaceholderProxy {

    private static final AtomicReference<PlaceholderProvider> PROVIDER = new AtomicReference<>(NoProvider.INSTANCE);
    private static final List<PlaceholderProvider> PROVIDERS = new ArrayList<>();

    static {
        if (ReflectionUtils.doesClassExist("com.envyful.papi.forge.ForgePlaceholderAPI")) {
            AtlantisPlaceholderProxy.addPlaceholderProvider(new FPAPI());
        }
    }

    private AtlantisPlaceholderProxy() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    @Entrypoint
    @SuppressWarnings("unused")
    private static void bootstrap() {
        for (var provider : AtlantisPlaceholderProxy.PROVIDERS) {
            AtlantisPlaceholderProxy.setProvider(provider);
            break;
        }
    }

    public static void addPlaceholderProvider(PlaceholderProvider provider) {
        AtlantisPlaceholderProxy.PROVIDERS.add(provider);
    }

    public static void setProvider(PlaceholderProvider provider) {
        AtlantisPlaceholderProxy.PROVIDER.set(Mods.events().emitSetPlaceholderProvider(provider).provider());
    }

    public static <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        AtlantisPlaceholderProxy.PROVIDER.get().register(id, name, extension, type);
    }

    public static <T> Text replace(T context, Text text) {
        return AtlantisPlaceholderProxy.PROVIDER.get().replace(context, text);
    }

    public static Text replace(UUID uuid, Text text) {
        return AtlantisPlaceholderProxy.PROVIDER.get().replace(uuid, text);
    }
}
