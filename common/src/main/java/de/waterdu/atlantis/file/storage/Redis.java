package de.waterdu.atlantis.file.storage;

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

    public Redis(String url) throws InvalidRedisUrlException {
        this(Redis.toURIArray(url));
    }

    private Redis(Object[] uriArray) {
        this((String) uriArray[0], (Integer) uriArray[1], (String) uriArray[2], (char[]) uriArray[3]);
    }

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

    @Override
    public <T extends Configuration> void init(Config<T> config, String modID, String key, String path, Gson gson,
                                               T configuration, ClassHolder<T> classes) {
        this.config = config;
        this.channel = modID + ":" + path;
        this.sub.sync().subscribe(this.channel);
        this.sub.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                Redis.this.receive(message);
            }
        });
        this.publishAsync(Message.HI.prepare(this));
    }

    @Override
    public <T extends Configuration> void destruct(Config<T> config) {
        this.sub.close();
        this.pub.close();
        this.client.shutdown();
    }

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

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) {
        return this.writeSpecific(config, data, this.updateTimestamps.get());
    }

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

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        return this.delete(config, data, true);
    }

    private <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data, boolean sendMessage) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            try {
                var removed = config.uuidDataMap().remove(data.uuid());
                config.filenameDataMap().remove(data.identifier() + config.dataFileExtension());
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

    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid) {
        return this.deleteFromUUID(config, uuid, true);
    }

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

    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        var data = config.get(name);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.delete(config, data);
    }

    private void publishJson(UUID uuid, String json) {
        this.publish(Message.JSON + " " + this.timestamps.get(uuid) + " " + json);
    }

    public void publish(String message) {
        this.pub.sync().publish(this.channel, message);
    }

    public void publishAsync(String message) {
        this.pub.async().publish(this.channel, message);
    }

    private void receive(String message) {
        Message.process(this, message).ifPresent(msg -> AtlantisLogger.debug("Received Redis message of type {} on channel {}", msg.toString(), this.channel));
    }

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

    public static class InvalidRedisUrlException extends RuntimeException {

        private final String uri;

        public InvalidRedisUrlException(String uri) {
            super(uri);
            this.uri = uri;
        }

        public String getURI() {
            return this.uri;
        }

        @Override
        public String getMessage() {
            return "URI must be in the format \"redis://password@ip:port/\".";
        }
    }

    private static Object[] toURIArray(String uri) throws InvalidRedisUrlException {
        uri = uri.replace("redis://", "");
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        String[] account = uri.split("@");
        if (account.length > 2) {
            throw new InvalidRedisUrlException(uri);
        }
        String[] addr = account[account.length - 1].split(":");
        if (addr.length != 2) {
            throw new InvalidRedisUrlException(uri);
        }
        String[] userpass = account[0].split(":");
        return new Object[]{addr[0], Integer.parseInt(addr[1]), userpass[0], userpass[1].toCharArray()};
    }

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

        Message(QuadConsumer<Redis, UUID, UUID, String> consumer) {
            this.consumer = consumer;
        }

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

        private String prepare(Redis redis) {
            return this.name() + " " + redis.uuid;
        }

        private String prepare(Redis redis, UUID ack) {
            return this.prepare(redis) + " " + ack.toString();
        }

        @Override
        public String toString() {
            return this.name();
        }
    }
}
