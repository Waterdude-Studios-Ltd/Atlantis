package de.waterdu.atlantis.mod.impl.common.event;

/**
 * Marks an event payload that a listener can cancel.
 */
public interface Cancellable {

    /**
     * Cancels the event. The listeners after the current one do not run. A listener also cancels the event if it
     * returns a result other than {@link net.minecraft.world.InteractionResult#PASS}.
     */
    void cancel();

    /**
     * @return {@code true} if the event is cancelled
     */
    boolean cancelled();
}
