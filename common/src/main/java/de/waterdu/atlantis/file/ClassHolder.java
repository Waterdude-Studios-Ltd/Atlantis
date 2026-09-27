package de.waterdu.atlantis.file;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class ClassHolder<T extends Configuration> {

    private final Class<T> configuration;
    private final Class<? extends Configuration> data;
    private final Class<? extends Configuration> namedData;
    private final Class<? extends Configuration> playerData;

    private final Constructor<? extends PlayerData> playerDataConstructor;
    private final Method playerDataOnLoginCallback;
    private final Method playerDataOnLogoutCallback;

    private final String simpleName;
    private final String canonicalName;

    @SuppressWarnings("unchecked")
    public ClassHolder(Class<T> clazz) {
        Constructor<? extends PlayerData> playerDataConstructor;
        Method playerDataOnLoginCallback;
        Method playerDataOnLogoutCallback;

        this.simpleName = clazz.getSimpleName();
        this.canonicalName = clazz.getCanonicalName();

        if (Configuration.class.isAssignableFrom(clazz)) {
            this.configuration = clazz;
        } else {
            this.configuration = null;
        }

        if (Data.class.isAssignableFrom(clazz)) {
            this.data = clazz;
        } else {
            this.data = null;
        }

        if (NamedData.class.isAssignableFrom(clazz)) {
            this.namedData = clazz;
        } else {
            this.namedData = null;
        }

        if (PlayerData.class.isAssignableFrom(clazz)) {
            this.playerData = clazz;
            try {
                playerDataConstructor = (Constructor<? extends PlayerData>) clazz.getConstructor(ServerPlayer.class);
            } catch (NoSuchMethodException e) {
                AtlantisLogger.warn("Couldn't find constructor {}#<init>(ServerPlayer)", this.simpleName);
                playerDataConstructor = null;
            }
            try {
                playerDataOnLoginCallback = clazz.getMethod("onLogin", ServerPlayer.class);
            } catch (NoSuchMethodException e) {
                AtlantisLogger.warn("Couldn't find method {}#onLogin(ServerPlayer)", this.simpleName);
                playerDataOnLoginCallback = null;
            }
            try {
                playerDataOnLogoutCallback = clazz.getMethod("onLogout", ServerPlayer.class);
            } catch (NoSuchMethodException e) {
                AtlantisLogger.warn("Couldn't find method {}#onLogout(ServerPlayer)", this.simpleName);
                playerDataOnLogoutCallback = null;
            }
        } else {
            this.playerData = null;
            playerDataConstructor = null;
            playerDataOnLoginCallback = null;
            playerDataOnLogoutCallback = null;
        }

        this.playerDataConstructor = playerDataConstructor;
        this.playerDataOnLoginCallback = playerDataOnLoginCallback;
        this.playerDataOnLogoutCallback = playerDataOnLogoutCallback;
    }

    public Class<T> getConfigurationClass() {
        return this.configuration;
    }

    @SuppressWarnings("unchecked")
    public <V extends Configuration> Optional<Class<V>> dataClass() {
        return Optional.ofNullable((Class<V>) this.data);
    }

    @SuppressWarnings("unchecked")
    public <V extends Configuration> Optional<Class<V>> namedDataClass() {
        return Optional.ofNullable((Class<V>) this.namedData);
    }

    @SuppressWarnings("unchecked")
    public <V extends Configuration> Optional<Class<V>> playerDataClass() {
        return Optional.ofNullable((Class<V>) this.playerData);
    }

    @SuppressWarnings("unchecked")
    public <V extends Configuration> Optional<Constructor<V>> playerDataConstructor() {
        return Optional.ofNullable((Constructor<V>) this.playerDataConstructor);
    }

    public Optional<Method> playerDataOnLoginCallback() {
        return Optional.ofNullable(this.playerDataOnLoginCallback);
    }

    public Optional<Method> playerDataOnLogoutCallback() {
        return Optional.ofNullable(this.playerDataOnLogoutCallback);
    }

    @ApiStatus.Internal
    public void invokeLoginCallback(Config<T> config, PlayerData data, ServerPlayer player) {
        if (this.playerDataOnLoginCallback != null) {
            try {
                this.playerDataOnLoginCallback.invoke(data, player);
            } catch (IllegalAccessException | InvocationTargetException e) {
                AtlantisLogger.log(
                        Settings.get().getMissingCallbacksSeverity(),
                        "No valid void onLogin(ServerPlayer) callback in data " + config.name()
                );
            }
        }
    }

    @ApiStatus.Internal
    public void invokeLogoutCallback(Config<T> config, PlayerData data, ServerPlayer player) {
        if (this.playerDataOnLogoutCallback != null) {
            try {
                this.playerDataOnLogoutCallback.invoke(data, player);
            } catch (IllegalAccessException | InvocationTargetException e) {
                AtlantisLogger.log(
                        Settings.get().getMissingCallbacksSeverity(),
                        "No valid void onLogout(ServerPlayer) callback in data " + config.name()
                );
            }
        }
    }

    public String simpleName() {
        return this.simpleName;
    }

    public String canonicalName() {
        return this.canonicalName;
    }
}
