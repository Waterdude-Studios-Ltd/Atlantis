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

public final class LevelUtils {

    private LevelUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Throws an exception when no server runs.
     */
    public static @NonNull ServerLevel overworld() {
        var server = ServerUtils.server();
        if (server == null) {
            throw new NullPointerException("Server is null");
        }

        return server.overworld();
    }

    /**
     * @param       dimension   A level id. The namespace defaults to {@code minecraft}.
     * @return                  null when the id is empty.
     */
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

    public static @Nullable ServerLevel level(ResourceKey<Level> key) {
        var server = ServerUtils.server();
        if (server == null) {
            return null;
        }

        return server.getLevel(key);
    }

    public static @Nullable ServerLevel level(String key) {
        return LevelUtils.level(LevelUtils.dimension(key));
    }

    public static List<ServerLevel> levels() {
        var server = ServerUtils.server();
        if (server == null) {
            return Lists.newArrayList();
        }

        var levels = Lists.<ServerLevel>newArrayList();
        for (var level : server.getAllLevels()) {
            levels.add(level);
        }
        return levels;
    }

    public static List<String> levelIds() {
        var server = ServerUtils.server();
        if (server == null) {
            return Lists.newArrayList();
        }

        var levels = new ArrayList<String>();
        for (var level : server.getAllLevels()) {
            levels.add(level.dimension().location().toString());
        }
        return levels;
    }
}
