package de.waterdu.atlantis.util.placeholder.impl;

import com.envyful.papi.api.PlaceholderFactory;
import com.envyful.papi.api.manager.AbstractPlaceholderManager;
import com.envyful.papi.api.manager.extensions.PlaceholderExtension;
import com.envyful.papi.api.util.UtilPlaceholder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FPAPI implements PlaceholderProvider {

    private final Map<Class<?>, Map<String, Impl<?>>> impl = new HashMap<>();

    public FPAPI() {}

    @SuppressWarnings("unchecked")
    public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        ((Impl<T>) this.impl.computeIfAbsent(type, t -> new HashMap<>()).computeIfAbsent(id, i -> {
            var newImpl = new Impl<>(id, name, type);
            PlaceholderFactory.register(newImpl);
            return newImpl;
        })).registerPlaceholder(new ExtensionImpl<>(extension));
    }

    @Override
    public <T> Text replace(T object, Text text) {
        return Text.of(UtilPlaceholder.replaceIdentifiers(object, text.get()));
    }

    @Override
    public Text replace(UUID uuid, Text text) {
        return Text.of(UtilPlaceholder.replaceIdentifiers(uuid, text.get()));
    }

    private record ExtensionImpl<T>(AtlantisPlaceholderExtension<T> impl) implements PlaceholderExtension<T> {

        @Override
        public String getName() {
            return this.impl.name();
        }

        @Override
        public int getPriority() {
            return this.impl.priority();
        }

        @Override
        public List<String> getDescription() {
            return this.impl.description();
        }

        @Override
        public List<String> getExamples() {
            return this.impl.examples();
        }

        @Override
        public boolean matches(T object, String placeholder) {
            return this.impl.matches(object, placeholder);
        }

        @Override
        public boolean matchesObject(Object object, String placeholder) {
            return this.impl.matchesObject(object, placeholder);
        }

        @Override
        public String parse(T object, String placeholder) {
            return this.impl.parse(object, placeholder);
        }

        @Override
        public String parseObject(Object object, String placeholder) {
            return this.impl.parseObject(object, placeholder);
        }
    }

    private static class Impl<T> extends AbstractPlaceholderManager<T> {

        public Impl(String id, String name, Class<T> type) {
            super(id, Atlantis.WATER_NAME_AS_ARRAY, Atlantis.VERSION, name, type);
        }
    }
}
