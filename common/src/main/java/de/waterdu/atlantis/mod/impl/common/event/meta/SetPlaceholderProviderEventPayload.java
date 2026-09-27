package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.placeholder.impl.NoProvider;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;

public class SetPlaceholderProviderEventPayload implements EventPayload, Cancellable {

    private PlaceholderProvider provider;

    private boolean cancelled = false;

    public SetPlaceholderProviderEventPayload(PlaceholderProvider provider) {
        this.provider = provider;
    }

    @Override
    public Key<?> type() {
        return UEvent.SET_PLACEHOLDER_PROVIDER;
    }

    public PlaceholderProvider provider() {
        return this.provider;
    }

    public void setProvider(PlaceholderProvider provider) {
        this.provider = provider;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
        this.provider = NoProvider.INSTANCE;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
