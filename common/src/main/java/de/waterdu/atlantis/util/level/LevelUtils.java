package de.waterdu.atlantis.util.level;

import com.google.common.collect.Lists;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LevelUtils {

    private LevelUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static @NonNull ServerLevel overworld() {
        return ServerUtils.server().overworld();
    }

    public static @Nullable ResourceKey<Level> dimension(String dimension) {
        if (dimension.isEmpty()) {
            return null;
        } else {
            return LevelUtils.dimension(ResourceLocation.parse(dimension));
        }
    }

    public static @NonNull ResourceKey<Level> dimension(ResourceLocation key) {
        return ResourceKey.create(Registries.DIMENSION, key);
    }

    public static Optional<ServerLevel> level(ResourceKey<Level> key) {
        return Optional.ofNullable(ServerUtils.server().getLevel(key));
    }

    public static Optional<ServerLevel> level(String key) {
        return LevelUtils.level(LevelUtils.dimension(key));
    }

    public static List<ServerLevel> levels() {
        var levels = Lists.<ServerLevel>newArrayList();
        for (var level : ServerUtils.server().getAllLevels()) {
            levels.add(level);
        }
        return levels;
    }

    public static List<String> levelIDs() {
        var levels = new ArrayList<String>();
        for (var level : LevelUtils.levels()) {
            levels.add(level.dimension().location().toString());
        }
        return levels;
    }
}
