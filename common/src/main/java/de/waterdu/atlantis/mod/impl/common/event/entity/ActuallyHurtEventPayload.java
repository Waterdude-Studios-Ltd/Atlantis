package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ActuallyHurtEventPayload implements EventPayload, Cancellable {

    private final LivingEntity entity;
    private final DamageSource source;
    private float amount;
    private boolean cancelled = false;

    public ActuallyHurtEventPayload(LivingEntity entity, DamageSource source, float amount) {
        this.entity = entity;
        this.source = source;
        this.amount = amount;
    }

    @Override
    public Key<?> type() {
        return UEvent.ACTUALLY_HURT;
    }

    public LivingEntity entity() {
        return this.entity;
    }

    public DamageSource source() {
        return this.source;
    }

    public float amount() {
        return this.amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
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
