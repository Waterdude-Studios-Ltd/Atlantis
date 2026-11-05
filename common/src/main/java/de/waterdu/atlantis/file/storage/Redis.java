package de.waterdu.atlantis.file.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.util.java.UUIDUtils;
import de.waterdu.atlantis.util.java.interfaces.QuadConsumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Volatile Redis {@link Storage} implementation for AtlantisConfig.
 * <p>
 * <a href="https://github.com/EnvyWare/API">Danil</a>
 * <a href="https://lettuce.io/">🥬</a>
 */
public class Redis implements Storage {

    private final UUID uuid = UUID.randomUUID();
    private final Set<UUID> connections = Sets.newConcurrentHashSet();

    private final AtomicBoolean ack = new AtomicBoolean(false);

    private final RedisURI uri;
    private final RedisClient client;
    private final StatefulRedisPubSubConnection<String, String> sub; // in
    private final StatefulRedisPubSubConnection<String, String> pub; // out

    private final Map<UUID, Long> timestamps = new ConcurrentHashMap<>();
    private final AtomicBoolean updateTimestamps = new AtomicBoolean(true);

    private Config<? extends Configuration> config;

    private String channel;

    /**
     * Connects to Redis immediately.
     *
     * @param url the format is {@code redis://[user:]password@host:port/}. The credentials are optional.
     */
    public Redis(String url) throws InvalidRedisUrlException {
        this(Redis.toURIArray(url));
    }

    /**
     * Connects to Redis immediately.
     *
     * @param uriArray the host, port, username and password from {@link #toURIArray}
     */
    private Redis(Object[] uriArray) {
        this((String) uriArray[0], (Integer) uriArray[1], (String) uriArray[2], (char[]) uriArray[3]);
    }

    /**
     * Connects to Redis immediately.
     *
     * @param username empty for no username
     * @param password empty for no password
     */
    public Redis(String ip, int port, String username, char[] password) {
        RedisURI.Builder builder = RedisURI.builder().withHost(ip).withPort(port);
        if (!username.isEmpty()) {
            builder.withAuthentication(username, password);
        }
        if (password.length > 0) {
            builder.withPassword(password);
        }
        builder.withSsl(Settings.get().isRedisSSL());
        this.uri = builder.build();
        this.client = RedisClient.create(this.uri);
        this.sub = this.client.connectPubSub();
        this.pub = this.client.connectPubSub();
    }

    /**
     * Subscribes to the channel {@code modId:path} and sends a {@code HI} message to the other servers on it.
     *
     * @param config the config that uses this storage
     * @param modId the ID of the mod that owns the config
     * @param key the key of the config
     * @param path the path of the config, used in the channel name
     * @param gson the Gson instance of the config
     * @param configuration the default configuration instance
     * @param classes the classes of the config
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void init(Config<T> config, String modId, String key, String path, Gson gson,
                                               T configuration, ClassHolder<T> classes) {
        this.config = config;
        this.channel = modId + ":" + path;
        this.sub.sync().subscribe(this.channel);
        this.sub.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                Redis.this.receive(message);
            }
        });
        this.publishAsync(Message.HI.prepare(this));
    }

    /**
     * Closes both connections and shuts down the client.
     *
     * @param config the config that uses this storage
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void destruct(Config<T> config) {
        this.sub.close();
        this.pub.close();
        this.client.shutdown();
    }

    /**
     * Publishes every data instance in memory, or publishes the configuration under the zeroed UUID. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config to publish
     * @param <T> the configuration type
     * @return a future of {@code true} if the publish was successful, or of {@code false} if the server is not alive
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }

        boolean updateTimestamps = this.updateTimestamps.get();

        if (config.container().isData()) {
            Atlantis.THREAD_POOL.submit(() -> {
                for (T out : config.uuidDataMap().values()) {
                    if (out != null) {
                        try {
                            this.writeSpecific(config, out, updateTimestamps);
                        } catch (Exception e) {
                            AtlantisLogger.error("Failed to publish instance of {} called {} with UUID {}: {}", config.name(), out.identifier(), out.uuid(), e.getMessage());
                        }
                    }
                }
                result.complete(true);
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully published {}", config.name());
            });
        } else {
            Atlantis.THREAD_POOL.submit(() -> {
                try {
                    if (updateTimestamps) {
                        this.timestamps.put(UUIDUtils.ZEROED_UUID, System.currentTimeMillis());
                    }
                    this.publishJson(UUIDUtils.ZEROED_UUID, config.gson().toJson(config.container().get()));
                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully published {}", config.name());
                    config.remapLang();
                    result.complete(true);
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to publish {}: {}", config.name(), e.getMessage());
                    result.complete(false);
                }
            });
        }

        return result;
    }

    /**
     * Publishes the data instance on the channel.
     *
     * @param config the config of the data
     * @param data the data instance to publish
     * @param <T> the configuration type
     * @return a future of {@code true} if the publish was successful
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) {
        return this.writeSpecific(config, data, this.updateTimestamps.get());
    }

    /**
     * Publishes the data instance on the channel on {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param data the data instance to publish
     * @param updateTimestamps sets the timestamp of the instance to the current time before the publish
     * @param <T> the configuration type
     * @return a future of {@code true} if the publish was successful, or of {@code false} if the server is not alive
     */
    private  <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data, boolean updateTimestamps) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }

        Atlantis.THREAD_POOL.submit(() -> {
            try {
                if (updateTimestamps) {
                    this.timestamps.put(data.uuid(), System.currentTimeMillis());
                }
                this.publishJson(data.uuid(), config.gson().toJson(data));
                result.complete(true);
            } catch (Exception e) {
                AtlantisLogger.error("Failed to publish instance of {} for file {}: {}", config.name(), data.identifier(), e.getMessage());
                result.complete(false);
            }
        });

        return result;
    }

    /**
     * Reads nothing, because the instances arrive as messages from other servers.
     *
     * @param config the config to read
     * @param skipNulls ignored
     * @param <T> the configuration type
     * @return a completed future of {@code false}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Reads nothing, because the instances arrive as messages from other servers.
     *
     * @param config the config to read
     * @param dir ignored
     * @param names ignored
     * @param <T> the configuration type
     * @return a completed future of {@code false}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Reads nothing, because the instances arrive as messages from other servers.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a completed future of {@code null}
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Reads nothing, because the instances arrive as messages from other servers.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a completed future of {@code null}
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Removes the data instance from memory and sends a {@code DEL} message to the other servers.
     *
     * @param config the config of the data
     * @param data the data instance to delete
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        return this.delete(config, data, true);
    }

    /**
     * Removes the data instance from the UUID, file name and name maps of the config on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param data the data instance to delete
     * @param sendMessage sends a {@code DEL} message to the other servers
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if the server is not alive or the removal failed
     */
    private <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data, boolean sendMessage) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            try {
                var removed = config.uuidDataMap().remove(data.uuid());
                config.filenameDataMap().remove(config.filename((Data) data));
                if (data instanceof NamedData namedData) {
                    config.nameDataMap().remove(namedData.name());
                }
                if (sendMessage) {
                    this.publish(Message.DEL.prepare(this, removed.uuid()));
                }
                result.complete(removed);
            } catch (Exception e) {
                AtlantisLogger.error("Failed to remove {} for file {}: {}", config.name(), data.identifier(), e.getMessage());
                result.complete(null);
            }
        });
        return result;
    }

    /**
     * Deletes the data instance with the given UUID and sends a {@code DEL} message to the other servers.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid) {
        return this.deleteFromUUID(config, uuid, true);
    }

    /**
     * Deletes the data instance with the given UUID with {@link #delete(Config, Configuration, boolean)}. The
     * instance must already be loaded.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param sendMessage sends a {@code DEL} message to the other servers
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if the server is not alive or nothing was removed
     */
    private <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid, boolean sendMessage) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        var data = config.get(uuid);
        if (data != null && data.isDone()) {
            try {
                return this.delete(config, data.get(), sendMessage);
            } catch (ExecutionException | InterruptedException ignored) {}
        }
        result.complete(null);
        return result;
    }

    /**
     * Deletes the named data instance with the given name with {@link #delete(Config, Configuration)}.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        var data = config.get(name);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.delete(config, data);
    }

    /**
     * Publishes a {@code JSON} message with the stored timestamp of the UUID, or {@code 0} if it has none.
     *
     * @param uuid the UUID of the instance
     * @param json the serialized instance
     */
    private void publishJson(UUID uuid, String json) {
        this.publish(Message.JSON + " " + this.timestamps.getOrDefault(uuid, 0L) + " " + json);
    }

    /**
     * Sends the message on the channel of this storage and blocks until Redis replies.
     * Use it only after the storage initializes.
     */
    public void publish(String message) {
        this.pub.sync().publish(this.channel, message);
    }

    /**
     * Sends the message on the channel of this storage and does not wait for Redis to reply. Use it only after the
     * storage initializes.
     *
     * @param message the message to send
     */
    public void publishAsync(String message) {
        this.pub.async().publish(this.channel, message);
    }

    /**
     * Processes a message from the channel and logs its type.
     *
     * @param message the received message
     */
    private void receive(String message) {
        Message.process(this, message).ifPresent(msg -> AtlantisLogger.debug("Received Redis message of type {} on channel {}", msg.toString(), this.channel));
    }

    /**
     * Puts the received instance in memory if its timestamp is not older than the stored one. Data goes in the
     * config, and a configuration replaces the current one.
     *
     * @param timestamp the time of the change, in milliseconds
     * @param json the serialized instance
     * @param <T> the configuration type
     */
    @SuppressWarnings("unchecked")
    private <T extends Configuration> void put(long timestamp, String json) {
        var config = (Config<T>) this.config;
        var instance = config.gson().fromJson(json, config.classes().getConfigurationClass());
        if (instance != null && this.timestamps.getOrDefault(instance.uuid(), -1L) <= timestamp) {
            this.timestamps.put(instance.uuid(), timestamp);
            if (instance instanceof Data) {
                config.put(instance);
            } else {
                config.container().set(config, instance);
            }
        }
    }

    /**
     * Thrown when a Redis URL is not in the format {@code redis://[user:]password@host:port/}.
     */
    public static class InvalidRedisUrlException extends RuntimeException {

        private final String uri;

        /**
         * Creates the exception for the given URL.
         *
         * @param uri the invalid URL
         */
        public InvalidRedisUrlException(String uri) {
            super(uri);
            this.uri = uri;
        }

        /**
         * @return the invalid URL
         */
        public String getURI() {
            return this.uri;
        }

        /**
         * Gets a message that shows the expected format, instead of the URL.
         *
         * @return the message
         */
        @Override
        public String getMessage() {
            return "URI must be in the format \"redis://password@ip:port/\".";
        }
    }

    /**
     * Splits a Redis URL into its parts.
     *
     * @param uri the URL in the format {@code redis://[user:]password@host:port/}
     * @return the host, the port as an {@link Integer}, the username and the password as a {@code char[]}. The
     *     username and password are empty if the URL has none.
     * @throws InvalidRedisUrlException if the URL is not in the expected format
     */
    @VisibleForTesting
    static Object[] toURIArray(String uri) throws InvalidRedisUrlException {
        var original = uri;
        uri = uri.replace("redis://", "");
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        String[] account = uri.split("@");
        if (account.length > 2) {
            throw new InvalidRedisUrlException(original);
        }
        String[] addr = account[account.length - 1].split(":");
        if (addr.length != 2 || addr[0].isEmpty()) {
            throw new InvalidRedisUrlException(original);
        }
        int port;
        try {
            port = Integer.parseInt(addr[1]);
        } catch (NumberFormatException e) {
            throw new InvalidRedisUrlException(original);
        }
        var username = "";
        var password = "";
        if (account.length == 2) {
            var credentials = account[0];
            var separator = credentials.indexOf(':');
            if (separator >= 0) {
                username = credentials.substring(0, separator);
                password = credentials.substring(separator + 1);
            } else {
                password = credentials;
            }
        }
        return new Object[]{addr[0], port, username, password.toCharArray()};
    }

    /**
     * Lists the message types that the servers send on a channel. Each message starts with the type and the UUID of
     * the sender. Some types also hold a target UUID.
     */
    private enum Message {

        SYNC((redis, uuid, ack, msg) -> {
            if (ack.equals(redis.uuid)) {
                redis.updateTimestamps.set(false);
                redis.write(redis.config);
                redis.updateTimestamps.set(true);
            }
        }),
        DEL((redis, uuid, ack, msg) -> {
            if (!uuid.equals(redis.uuid)) {
                redis.deleteFromUUID(redis.config, ack, false);
            }
        }),
        ACK((redis, uuid, ack, msg) -> {
            if (ack.equals(redis.uuid)) {
                redis.connections.add(uuid);
                if (redis.ack.compareAndSet(false, true)) {
                    Atlantis.THREAD_POOL.schedule(() -> {
                        var delay = Settings.get().getRedisSyncMilliseconds();
                        var totalDelay = 0L;
                        var i = 0;
                        for (var other : redis.connections) {
                            var last = i + 1 == redis.connections.size();
                            Atlantis.THREAD_POOL.schedule(() -> {
                                redis.publishAsync(Message.SYNC.prepare(redis, other));
                                if (last) {
                                    redis.ack.set(false);
                                }
                            }, totalDelay, TimeUnit.MILLISECONDS);
                            totalDelay += delay;
                            i++;
                        }
                    }, Settings.get().getRedisAckMilliseconds(), TimeUnit.MILLISECONDS);
                }
            }
        }),
        HI((redis, uuid, ack, msg) -> {
            if (!uuid.equals(redis.uuid)) {
                redis.connections.add(uuid);
                redis.publishAsync(Message.ACK.prepare(redis, uuid));
            }
        }),
        BYE((redis, uuid, ack, msg) -> {
            if (!uuid.equals(redis.uuid)) {
                redis.connections.remove(uuid);
            }
        }),
        JSON((redis, uuid, ack, msg) -> {})
        ;

        private static final Map<String, Message> MESSAGES = new ConcurrentHashMap<>();

        static {
            for (var message : Message.values()) {
                Message.MESSAGES.put(message.name().toLowerCase(Locale.ROOT), message);
            }
        }

        private final QuadConsumer<Redis, UUID, UUID, String> consumer;

        /**
         * Creates the message type.
         *
         * @param consumer handles the message with the storage, the sender UUID, the target UUID and the raw message
         */
        Message(QuadConsumer<Redis, UUID, UUID, String> consumer) {
            this.consumer = consumer;
        }

        /**
         * Parses the message and handles it. A {@code JSON} message puts its instance in memory.
         *
         * @param redis the storage that received the message
         * @param string the raw message
         * @return the type of the message, or an empty optional if the message is not valid
         */
        private static Optional<Message> process(Redis redis, String string) {
            if (string.isEmpty()) {
                return Optional.empty();
            }
            if (string.startsWith(Message.JSON.name())) {
                string = string.substring(5);
                var timestamp = Long.parseLong(string.substring(0, string.indexOf(" ")));
                string = string.substring(string.indexOf(" ") + 1);
                redis.put(timestamp, string);
                return Optional.of(Message.JSON);
            }
            var args = string.split(" ");
            if (args.length < 2) {
                return Optional.empty();
            }
            var message = Message.MESSAGES.getOrDefault(args[0].toLowerCase(Locale.ROOT), null);
            if (message == null) {
                return Optional.empty();
            }
            try {
                message.consumer.accept(redis, UUID.fromString(args[1]), args.length > 2 ? UUID.fromString(args[2]) : UUIDUtils.ZEROED_UUID, string);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
            return Optional.of(message);
        }

        /**
         * Creates a message of this type from the storage.
         *
         * @param redis the storage that sends the message
         * @return the type and the UUID of the storage, separated by a space
         */
        private String prepare(Redis redis) {
            return this.name() + " " + redis.uuid;
        }

        /**
         * Creates a message of this type from the storage to a target.
         *
         * @param redis the storage that sends the message
         * @param ack the target UUID
         * @return the type, the UUID of the storage and the target UUID, separated by spaces
         */
        private String prepare(Redis redis, UUID ack) {
            return this.prepare(redis) + " " + ack.toString();
        }

        /**
         * @return the name of the message type
         */
        @Override
        public String toString() {
            return this.name();
        }
    }
}
