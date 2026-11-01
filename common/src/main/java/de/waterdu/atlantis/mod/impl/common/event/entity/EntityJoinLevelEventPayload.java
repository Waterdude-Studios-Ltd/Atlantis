package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Fires when an entity is added to a server level. The entity can be new or loaded from disk. If you cancel the event,
 * the entity stays out of the level. If the entity was loaded from disk, it is then dropped when its chunk is saved.
 */
public class EntityJoinLevelEventPayload implements EventPayload, Cancellable {

    private final Entity entity;
    private final ServerLevel level;
    private final boolean loadedFromDisk;
    private boolean cancelled = false;

    public EntityJoinLevelEventPayload(Entity entity, ServerLevel level, boolean loadedFromDisk) {
        this.entity = entity;
        this.level = level;
        this.loadedFromDisk = loadedFromDisk;
    }

    @Override
    public Key<?> type() {
        return UEvent.ENTITY_JOIN_LEVEL;
    }

    public Entity entity() {
        return this.entity;
    }

    public ServerLevel level() {
        return this.level;
    }

    public boolean loadedFromDisk() {
        return this.loadedFromDisk;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
