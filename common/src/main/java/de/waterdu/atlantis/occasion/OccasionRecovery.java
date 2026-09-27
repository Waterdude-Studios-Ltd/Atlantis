package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.LevelPosition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FileBackedObject(mod = Atlantis.ID, path = "occasions.json")
public class OccasionRecovery implements Configuration {

    private final Map<PlayerReference, Pair<Integer, LevelPosition>> recovery = new ConcurrentHashMap<>();

    public OccasionRecovery() {

    }

    protected static OccasionRecovery get() {
        return Atlantis.CONFIG.get(OccasionRecovery.class);
    }

    protected static void write() {
        Atlantis.CONFIG.save(OccasionRecovery.class);
    }

    protected static void add(PlayerReference reference, int moment, LevelPosition recovery) {
        get().recovery.put(reference, Pair.of(moment, recovery));
        write();
    }

    protected static Pair<Integer, LevelPosition> pop(PlayerReference reference) {
        if (reference == null) {
            return null;
        }
        var value = get().recovery.remove(reference);
        if (value != null) {
            write();
            return value;
        }
        return null;
    }
}
