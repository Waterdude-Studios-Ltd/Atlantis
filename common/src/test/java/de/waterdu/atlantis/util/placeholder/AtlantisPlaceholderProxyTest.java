package de.waterdu.atlantis.util.placeholder;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.placeholder.impl.NoProvider;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisPlaceholderProxyTest {

    @Test
    void defaultsToNoProvider() {
        var text = Text.of("Hello %player%");

        assertThat((Object) AtlantisPlaceholderProxy.replace(UUID.randomUUID(), text)).isSameAs(text);
        assertThat((Object) AtlantisPlaceholderProxy.replace("context", text)).isSameAs(text);
    }

    @Test
    void setProviderEmitsTheEventAndUsesTheProvider() {
        var provider = new Upper();
        var seen = new ArrayList<PlaceholderProvider>();
        Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).listen(payload -> {
            seen.add(payload.provider());
            return InteractionResult.PASS;
        });

        AtlantisPlaceholderProxy.setProvider(provider);

        assertThat(seen).containsExactly(provider);
        assertThat(AtlantisPlaceholderProxy.replace(UUID.randomUUID(), Text.of("abc")).get()).isEqualTo("ABC");
    }

    @Test
    void listenerCanSubstituteTheProvider() {
        var substitute = new Upper();
        Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).listen(payload -> {
            payload.setProvider(substitute);
            return InteractionResult.PASS;
        });

        AtlantisPlaceholderProxy.setProvider(NoProvider.INSTANCE);

        assertThat(AtlantisPlaceholderProxy.replace("context", Text.of("abc")).get()).isEqualTo("ABC");
    }

    @Test
    void cancellingFallsBackToNoProvider() {
        AtlantisPlaceholderProxy.setProvider(new Upper());
        Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).listen(__ -> InteractionResult.FAIL);

        AtlantisPlaceholderProxy.setProvider(new Upper());

        var text = Text.of("abc");
        assertThat((Object) AtlantisPlaceholderProxy.replace("context", text)).isSameAs(text);
    }

    @Test
    void registerIsForwardedToTheProvider() {
        var provider = new Upper();
        AtlantisPlaceholderProxy.setProvider(provider);

        AtlantisPlaceholderProxy.register("atlantis_test", "name", null, String.class);

        assertThat(provider.registered).containsExactly("atlantis_test:name");
    }

    @Test
    void bootstrapUsesTheFirstAddedProvider() throws ReflectiveOperationException {
        var first = new Upper();
        AtlantisPlaceholderProxy.addPlaceholderProvider(first);
        AtlantisPlaceholderProxy.addPlaceholderProvider(new Upper());
        var seen = new ArrayList<PlaceholderProvider>();
        Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).listen(payload -> {
            seen.add(payload.provider());
            return InteractionResult.PASS;
        });

        var bootstrap = AtlantisPlaceholderProxy.class.getDeclaredMethod("bootstrap");
        bootstrap.setAccessible(true);
        bootstrap.invoke(null);

        assertThat(seen).containsExactly(first);
    }

    private static final class Upper implements PlaceholderProvider {

        private final ArrayList<String> registered = new ArrayList<>();

        @Override
        public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
            this.registered.add(id + ":" + name);
        }

        @Override
        public <T> Text replace(T object, Text text) {
            return Text.of(text.get().toUpperCase());
        }

        @Override
        public Text replace(UUID uuid, Text text) {
            return Text.of(text.get().toUpperCase());
        }
    }
}
