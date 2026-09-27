package de.waterdu.atlantis.level.level;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.text.MessageUtils;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.ChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@FileBackedObject(mod = Atlantis.ID, path = "levels.json")
public final class AtlantisLevelStorage implements Configuration {

    private static final Text.Immutable WARNING_MESSAGE = Text.Immutable.of(ChatFormatting.DARK_RED + "Please restart the server to enable your new Atlantis level!");

    private Map<String, AtlantisLevel> levels = new HashMap<>();

    public AtlantisLevelStorage() {
        // noop
    }

    public void addDefaults() {
        if (Settings.get().isCreateDefaultAtlantisLevel() && this.levels.isEmpty()) {
            this.addLevel(AtlantisLevel.builder("atlantis", "example")
                    .chunkGenerator(AtlantisLevelRegistry.SUPERFLAT_CHUNK_GENERATOR)
                    .dimensionType(AtlantisLevelRegistry.UNIQUE_OVERWORLD_DIMENSION_TYPE)
                    .build()
            );
        }
    }

    public void addLevel(AtlantisLevel level) {
        this.levels.put(level.id(), level);
        this.save();
        MessageUtils.sendToMatching(WARNING_MESSAGE, Permissions::isOperator);
    }

    public Optional<AtlantisLevel> level(String id) {
        return Optional.ofNullable(this.levels.get(id));
    }

    public Map<String, AtlantisLevel> levels() {
        return this.levels;
    }

    public void setLevels(Map<String, AtlantisLevel> worlds) {
        this.levels = worlds;
    }

    public static AtlantisLevelStorage instance() {
        if (!Atlantis.CONFIG.internal().ready()) {
            Atlantis.CONFIG.forcedImmediate(Settings.class, FileBackedObject.class, ___ -> {});
            return Atlantis.CONFIG.forcedImmediate(AtlantisLevelStorage.class, FileBackedObject.class, AtlantisLevelStorage::addDefaults);
        }
        return Atlantis.CONFIG.get(AtlantisLevelStorage.class);
    }
}
