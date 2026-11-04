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

/**
 * Tests {@link AtlantisPlaceholderProxy}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisPlaceholderProxyTest {

    /**
     * Checks that with no provider set, both {@code replace} methods give back the same text.
     */
    @Test
    void defaultsToNoProvider() {
        var text = Text.of("Hello %player%");

        assertThat((Object) AtlantisPlaceholderProxy.replace(UUID.randomUUID(), text)).isSameAs(text);
        assertThat((Object) AtlantisPlaceholderProxy.replace("context", text)).isSameAs(text);
    }

    /**
     * Checks that {@link AtlantisPlaceholderProxy#setProvider} fires {@link UEvent#SET_PLACEHOLDER_PROVIDER} with the
     * provider, and that replacements then use it.
     */
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

    /**
     * Checks that a listener of {@link UEvent#SET_PLACEHOLDER_PROVIDER} can replace the provider that is set.
     */
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

    /**
     * Checks that when a listener cancels the provider change, the previous provider is not kept and text comes
     * back unchanged.
     */
    @Test
    void cancellingFallsBackToNoProvider() {
        AtlantisPlaceholderProxy.setProvider(new Upper());
        Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).listen(__ -> InteractionResult.FAIL);

        AtlantisPlaceholderProxy.setProvider(new Upper());

        var text = Text.of("abc");
        assertThat((Object) AtlantisPlaceholderProxy.replace("context", text)).isSameAs(text);
    }

    /**
     * Checks that {@link AtlantisPlaceholderProxy#register} passes the placeholder to the current provider.
     */
    @Test
    void registerIsForwardedToTheProvider() {
        var provider = new Upper();
        AtlantisPlaceholderProxy.setProvider(provider);

        AtlantisPlaceholderProxy.register("atlantis_test", "name", null, String.class);

        assertThat(provider.registered).containsExactly("atlantis_test:name");
    }

    /**
     * Checks that the private {@code bootstrap} method sets the first provider that was added, and only that one.
     *
     * @throws ReflectiveOperationException if {@code bootstrap} cannot be found or called
     */
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

    /**
     * Changes text to upper case instead of replacing placeholders, and records each registration.
     */
    private static final class Upper implements PlaceholderProvider {

        private final ArrayList<String> registered = new ArrayList<>();

        /**
         * Records the ID and the name of the placeholder group as {@code id:name}.
         *
         * @param <T> the type of the context object
         * @param id the ID of the placeholder group
         * @param name the name of the placeholder group
         * @param extension the extension to register, which is not used
         * @param type the class of the context object, which is not used
         */
        @Override
        public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
            this.registered.add(id + ":" + name);
        }

        /**
         * Changes the text to upper case.
         *
         * @param <T> the type of the context object
         * @param object the context object, which is not used
         * @param text the text to change
         * @return a new text in upper case
         */
        @Override
        public <T> Text replace(T object, Text text) {
            return Text.of(text.get().toUpperCase());
        }

        /**
         * Changes the text to upper case.
         *
         * @param uuid the UUID of the player, which is not used
         * @param text the text to change
         * @return a new text in upper case
         */
        @Override
        public Text replace(UUID uuid, Text text) {
            return Text.of(text.get().toUpperCase());
        }
    }
}
