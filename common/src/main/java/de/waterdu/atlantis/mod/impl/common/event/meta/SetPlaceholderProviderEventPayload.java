package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.placeholder.impl.NoProvider;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;

/**
 * Lets listeners choose the placeholder provider that Atlantis uses.
 */
public class SetPlaceholderProviderEventPayload implements EventPayload, Cancellable {

    private PlaceholderProvider provider;

    private boolean cancelled = false;

    /**
     * Creates a payload for {@link UEvent#SET_PLACEHOLDER_PROVIDER}.
     *
     * @param provider the provider that Atlantis sets if no listener changes it
     */
    public SetPlaceholderProviderEventPayload(PlaceholderProvider provider) {
        this.provider = provider;
    }

    /**
     * @return {@link UEvent#SET_PLACEHOLDER_PROVIDER}
     */
    @Override
    public Key<?> type() {
        return UEvent.SET_PLACEHOLDER_PROVIDER;
    }

    /**
     * The provider that Atlantis uses after the event. If you cancel the event, this is {@link NoProvider#INSTANCE}.
     */
    public PlaceholderProvider provider() {
        return this.provider;
    }

    /**
     * Sets the provider that Atlantis uses after the event.
     *
     * @param provider the new provider
     */
    public void setProvider(PlaceholderProvider provider) {
        this.provider = provider;
    }

    /**
     * Marks this payload as cancelled and sets the provider to {@link NoProvider#INSTANCE}.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
        this.provider = NoProvider.INSTANCE;
    }

    /**
     * Checks if a listener cancelled this payload.
     *
     * @return {@code true} if the payload is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
