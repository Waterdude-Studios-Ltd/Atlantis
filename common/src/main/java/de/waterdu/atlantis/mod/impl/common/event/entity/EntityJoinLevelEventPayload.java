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

    /**
     * Creates the payload for an entity that joins a level.
     *
     * @param entity the entity that joins the level
     * @param level the level that the entity joins
     * @param loadedFromDisk {@code true} if the entity was loaded from disk, {@code false} if it is new
     */
    public EntityJoinLevelEventPayload(Entity entity, ServerLevel level, boolean loadedFromDisk) {
        this.entity = entity;
        this.level = level;
        this.loadedFromDisk = loadedFromDisk;
    }

    /**
     * @return {@link UEvent#ENTITY_JOIN_LEVEL}
     */
    @Override
    public Key<?> type() {
        return UEvent.ENTITY_JOIN_LEVEL;
    }

    /**
     * @return the entity that joins the level
     */
    public Entity entity() {
        return this.entity;
    }

    /**
     * @return the level that the entity joins
     */
    public ServerLevel level() {
        return this.level;
    }

    /**
     * @return {@code true} if the entity was loaded from disk, {@code false} if it is new
     */
    public boolean loadedFromDisk() {
        return this.loadedFromDisk;
    }

    /**
     * Cancels the event. The entity then stays out of the level.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @return {@code true} if the event is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
