package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.status.Burn;
import com.pixelmonmod.pixelmon.battles.status.Freeze;
import com.pixelmonmod.pixelmon.battles.status.Paralysis;
import com.pixelmonmod.pixelmon.battles.status.Poison;
import com.pixelmonmod.pixelmon.battles.status.PoisonBadly;
import com.pixelmonmod.pixelmon.battles.status.Sleep;
import com.pixelmonmod.pixelmon.battles.status.StatusPersist;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class PixelmonStatus extends UStatus<StatusType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "status");

    public PixelmonStatus(StatusType status) {
        super(TYPE, status);
    }

    public static @Nullable PixelmonStatus of(StatusPersist status) {
        return status.type == null || status.type == StatusType.None ? null : new PixelmonStatus(status.type);
    }

    public static List<PixelmonStatus> all() {
        return List.of(
                new PixelmonStatus(StatusType.Burn),
                new PixelmonStatus(StatusType.Freeze),
                new PixelmonStatus(StatusType.Paralysis),
                new PixelmonStatus(StatusType.Poison),
                new PixelmonStatus(StatusType.PoisonBadly),
                new PixelmonStatus(StatusType.Sleep)
        );
    }

    @Override
    public String name() {
        return switch (this.dereference()) {
            case Burn -> BURN;
            case Freeze -> FREEZE;
            case Paralysis -> PARALYSIS;
            case Poison -> POISON;
            case PoisonBadly -> BAD_POISON;
            case Sleep -> SLEEP;
            default -> this.dereference().name();
        };
    }

    public StatusPersist create() {
        return switch (this.dereference()) {
            case Burn -> new Burn();
            case Freeze -> new Freeze();
            case Paralysis -> new Paralysis();
            case Poison -> new Poison();
            case PoisonBadly -> new PoisonBadly();
            case Sleep -> new Sleep();
            default -> throw new IllegalStateException("Not a persistent status: " + this.dereference());
        };
    }
}
