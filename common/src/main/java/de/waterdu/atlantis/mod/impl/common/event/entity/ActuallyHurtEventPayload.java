package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Holds the data of {@link UEvent#ACTUALLY_HURT}, which fires when a living entity is about to lose health from damage.
 */
public class ActuallyHurtEventPayload implements EventPayload, Cancellable {

    private final LivingEntity entity;
    private final DamageSource source;
    private float amount;
    private boolean cancelled = false;

    /**
     * Creates the payload for a hurt living entity.
     *
     * @param entity the entity that takes the damage
     * @param source the source of the damage
     * @param amount the damage that remains after armor, effects and absorption
     */
    public ActuallyHurtEventPayload(LivingEntity entity, DamageSource source, float amount) {
        this.entity = entity;
        this.source = source;
        this.amount = amount;
    }

    /**
     * @return {@link UEvent#ACTUALLY_HURT}
     */
    @Override
    public Key<?> type() {
        return UEvent.ACTUALLY_HURT;
    }

    /**
     * @return the entity that takes the damage
     */
    public LivingEntity entity() {
        return this.entity;
    }

    /**
     * @return the source of the damage
     */
    public DamageSource source() {
        return this.source;
    }

    /**
     * The damage that remains after armor, effects and absorption. The entity loses this much health. If you cancel the
     * event, the entity takes no damage.
     */
    public float amount() {
        return this.amount;
    }

    /**
     * Sets the damage that the entity takes.
     *
     * @param amount the new damage
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Cancels the event. The entity then takes no damage.
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
