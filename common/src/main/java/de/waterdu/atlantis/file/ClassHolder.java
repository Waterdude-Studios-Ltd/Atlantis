package de.waterdu.atlantis.file;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Holds the configuration class of a config, the data kinds that it implements, and its player data constructor and
 * callbacks.
 *
 * @param <T> the configuration type
 */
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

    /**
     * Checks which data kinds the class implements. For player data, finds the constructor that takes a
     * {@link ServerPlayer} and the {@code onLogin} and {@code onLogout} methods, and logs a warning for each one that
     * is missing.
     *
     * @param clazz the configuration class
     */
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

    /**
     * @return the configuration class
     */
    public Class<T> getConfigurationClass() {
        return this.configuration;
    }

    /**
     * @param <V> the type to cast the class to
     * @return the class if it implements {@link Data}, or else {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V extends Configuration> @Nullable Class<V> dataClass() {
        return (Class<V>) this.data;
    }

    /**
     * @param <V> the type to cast the class to
     * @return the class if it implements {@link NamedData}, or else {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V extends Configuration> @Nullable Class<V> namedDataClass() {
        return (Class<V>) this.namedData;
    }

    /**
     * @param <V> the type to cast the class to
     * @return the class if it implements {@link PlayerData}, or else {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V extends Configuration> @Nullable Class<V> playerDataClass() {
        return (Class<V>) this.playerData;
    }

    /**
     * @return the constructor that takes a {@link ServerPlayer}. Null if the class is not player data or has no such
     *     constructor.
     */
    @SuppressWarnings("unchecked")
    public <V extends Configuration> @Nullable Constructor<V> playerDataConstructor() {
        return (Constructor<V>) this.playerDataConstructor;
    }

    /**
     * @return the public {@code onLogin(ServerPlayer)} method. Null if the class is not player data or has no such
     *     method.
     */
    public @Nullable Method playerDataOnLoginCallback() {
        return this.playerDataOnLoginCallback;
    }

    /**
     * @return the public {@code onLogout(ServerPlayer)} method. Null if the class is not player data or has no such
     *     method.
     */
    public @Nullable Method playerDataOnLogoutCallback() {
        return this.playerDataOnLogoutCallback;
    }

    /**
     * Calls the {@code onLogin} method of the data, if the class has one. Logs a message at the
     * {@link Settings#getMissingCallbacksSeverity()} level if the call fails.
     *
     * @param config the config of the data
     * @param data the player data
     * @param player the player that logged in
     */
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

    /**
     * Calls the {@code onLogout} method of the data, if the class has one. Logs a message at the
     * {@link Settings#getMissingCallbacksSeverity()} level if the call fails.
     *
     * @param config the config of the data
     * @param data the player data
     * @param player the player that logged out
     */
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

    /**
     * @return the simple name of the configuration class
     */
    public String simpleName() {
        return this.simpleName;
    }

    /**
     * @return the canonical name of the configuration class
     */
    public String canonicalName() {
        return this.canonicalName;
    }
}
