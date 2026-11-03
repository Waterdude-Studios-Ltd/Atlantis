package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.LevelPosition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the recovery positions of players that were removed from an occasion while offline. The positions are
 * saved in {@code occasions.json}, and the players are teleported when they log in.
 */
@FileBackedObject(mod = Atlantis.ID, path = "occasions.json")
public class OccasionRecovery implements Configuration {

    private final Map<PlayerReference, Pair<Integer, LevelPosition>> recovery = new ConcurrentHashMap<>();

    /**
     * Creates an empty recovery store.
     */
    public OccasionRecovery() {
        // noop
    }

    /**
     * @return the loaded recovery store
     */
    protected static OccasionRecovery get() {
        return Atlantis.CONFIG.get(OccasionRecovery.class);
    }

    /**
     * Saves the recovery store to its file.
     */
    protected static void write() {
        Atlantis.CONFIG.save(OccasionRecovery.class);
    }

    /**
     * Stores the recovery position of the player and saves the file. It replaces an earlier entry of the player.
     *
     * @param reference the player
     * @param moment the index of the moment that the player was in, or {@code -1}
     * @param recovery the position to teleport the player to
     */
    protected static void add(PlayerReference reference, int moment, LevelPosition recovery) {
        get().recovery.put(reference, Pair.of(moment, recovery));
        write();
    }

    /**
     * Removes the entry of the player and saves the file if there was one.
     *
     * @param reference the player
     * @return the moment index and the recovery position, or {@code null} when the player has no entry or the
     *     reference is {@code null}
     */
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
