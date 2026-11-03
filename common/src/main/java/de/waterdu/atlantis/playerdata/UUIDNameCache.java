package de.waterdu.atlantis.playerdata;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the names and UUIDs of players in both directions. The cache is saved in {@code uuids.json}.
 */
@FileBackedObject(mod = Atlantis.ID, path = "uuids.json")
public class UUIDNameCache implements Configuration {

    private final Map<String, UUID> map = new ConcurrentHashMap<>();
    private transient Map<UUID, String> inverse = new ConcurrentHashMap<>();

    /**
     * Rebuilds the map from UUID to name when its size is different from the map from name to UUID.
     */
    private static void invert() {
        var instance = UUIDNameCache.get();
        if (instance.map.size() != instance.inverse.size()) {
            var inverse = new ConcurrentHashMap<UUID, String>(instance.map.size());
            for (var entry : instance.map.entrySet()) {
                inverse.put(entry.getValue(), entry.getKey());
            }
            instance.inverse = inverse;
        }
    }

    /**
     * @return the loaded cache
     */
    private static UUIDNameCache get() {
        return Atlantis.CONFIG.get(UUIDNameCache.class);
    }

    /**
     * Saves the cache to its file.
     */
    private static void write() {
        Atlantis.CONFIG.save(UUIDNameCache.class);
    }

    /**
     * Adds the name and the UUID of the player.
     *
     * @param player the player
     */
    public static void add(Player player) {
        UUIDNameCache.add(player.getName().getString(), player.getUUID());
    }

    /**
     * Adds the name and the UUID, and removes the old entries of both. Saves the cache only when something changes.
     * Ignores an empty name and a name that is equal to the UUID.
     *
     * @param name the name of the player
     * @param uuid the UUID of the player
     */
    public static void add(String name, UUID uuid) {
        if (name.isEmpty() || name.equalsIgnoreCase(uuid.toString())) {
            return;
        }
        if (!uuid.equals(UUIDNameCache.get(name)) || !name.equals(UUIDNameCache.get(uuid))) {
            var instance = UUIDNameCache.get();
            synchronized (instance) {
                var previousOwner = instance.map.put(name, uuid);
                if (previousOwner != null && !previousOwner.equals(uuid)) {
                    instance.inverse.remove(previousOwner, name);
                }
                var previousName = instance.inverse.put(uuid, name);
                if (previousName != null && !previousName.equals(name)) {
                    instance.map.remove(previousName, uuid);
                }
            }
            UUIDNameCache.write();
        }
    }

    /**
     * @param name the exact name. The lookup is case-sensitive.
     * @return {@link Util#NIL_UUID} when the name is not in the cache
     */
    public static UUID get(String name) {
        UUIDNameCache.invert();
        return UUIDNameCache.get().map.getOrDefault(name, Util.NIL_UUID);
    }

    /**
     * @return an empty string when the UUID is not in the cache
     */
    public static String get(UUID uuid) {
        UUIDNameCache.invert();
        return UUIDNameCache.get().inverse.getOrDefault(uuid, "");
    }
}
