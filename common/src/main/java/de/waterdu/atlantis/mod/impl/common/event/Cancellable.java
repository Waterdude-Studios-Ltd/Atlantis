package de.waterdu.atlantis.mod.impl.common.event;

public interface Cancellable {

    void cancel();

    boolean cancelled();
}
