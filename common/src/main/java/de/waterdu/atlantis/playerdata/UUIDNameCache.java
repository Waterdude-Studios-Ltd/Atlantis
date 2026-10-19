package de.waterdu.atlantis.playerdata;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@FileBackedObject(mod = Atlantis.ID, path = "uuids.json")
public class UUIDNameCache implements Configuration {

    private final Map<String, UUID> map = new ConcurrentHashMap<>();
    private transient Map<UUID, String> inverse = new ConcurrentHashMap<>();

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

    private static UUIDNameCache get() {
        return Atlantis.CONFIG.get(UUIDNameCache.class);
    }

    private static void write() {
        Atlantis.CONFIG.save(UUIDNameCache.class);
    }

    public static void add(Player player) {
        UUIDNameCache.add(player.getName().getString(), player.getUUID());
    }

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

    public static UUID get(String name) {
        UUIDNameCache.invert();
        return UUIDNameCache.get().map.getOrDefault(name, Util.NIL_UUID);
    }

    public static String get(UUID uuid) {
        UUIDNameCache.invert();
        return UUIDNameCache.get().inverse.getOrDefault(uuid, "");
    }
}
