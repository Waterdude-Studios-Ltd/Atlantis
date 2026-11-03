package de.waterdu.atlantis.util.entity;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.comm.MojangAPI;
import de.waterdu.atlantis.comm.Skin;
import de.waterdu.atlantis.comm.SkinQuery;
import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;
import de.waterdu.atlantis.playerdata.UUIDNameCache;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LevelUtils;
import de.waterdu.atlantis.util.network.NetworkUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.MessageUtils;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refers to a player by UUID, online or offline. It caches the name, the online player and loaded player data.
 */
public final class PlayerReference {

    private static final PlayerReference NULL = new PlayerReference(Util.NIL_UUID);

    private static final Map<UUID, Optional<PlayerReference>> REFERENCES = new ConcurrentHashMap<>();

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<PlayerReference> optional;
    private final Lazy<Optional<UUID>> uuid;
    private final ResettableLazy<Optional<String>> name;
    private final ResettableLazy<Optional<ServerPlayer>> entity;
    private final Map<Class<?>, PlayerData> data;
    private volatile @Nullable ServerPlayer known;

    /**
     * Creates a reference. Off the server thread, the lazy player lookup blocks until the server thread finds the
     * player.
     *
     * @param uuid the UUID of the player
     */
    private PlayerReference(UUID uuid) {
        this.optional = Optional.of(this);
        this.uuid = Lazy.of(() -> Optional.of(uuid));
        this.name = ResettableLazy.of(() -> Optional.of(UUIDNameCache.get(uuid)));
        this.entity = ResettableLazy.of(() -> {
            var known = this.known;
            if (known != null && !known.isRemoved()) {
                return Optional.of(known);
            }
            var server = ServerUtils.server();
            if (server == null || server.isSameThread()) {
                return ServerUtils.player(uuid);
            }
            var future = new CompletableFuture<Optional<ServerPlayer>>();
            Concurrency.sync(() -> future.complete(ServerUtils.player(uuid)));
            return future.join();
        });
        this.data = new HashMap<>();
    }

    /**
     * There is one reference for each UUID, and it stays in memory.
     *
     * @param player a {@link UUID}, a name from the name cache, a {@link Player}, a {@link PlayerReference} or a
     *     {@link PlayerData}. Names are case-sensitive.
     * @return empty for other objects, null, unknown names and the nil UUID
     */
    public static <T> Optional<PlayerReference> getSafely(@Nullable T player) {
        UUID uuid;
        switch (player) {
            case UUID asUUID -> uuid = asUUID;
            case String asName -> uuid = UUIDNameCache.get(asName);
            case Player asPlayer -> uuid = asPlayer.getUUID();
            case PlayerReference playerReference -> {
                return playerReference.optional();
            }
            case PlayerData asPlayerData -> uuid = asPlayerData.uuid();
            case null, default -> {
                return Optional.empty();
            }
        }
        if (Util.NIL_UUID.equals(uuid)) {
            return Optional.empty();
        }
        return PlayerReference.REFERENCES.computeIfAbsent(uuid, key -> new PlayerReference(key).optional());
    }

    /**
     * {@link #getSafely(Object)}, but gives a shared reference with the nil UUID instead of an empty optional.
     */
    @NonNull
    public static <T> PlayerReference get(@Nullable T player) {
        return PlayerReference.getSafely(player).orElse(NULL);
    }

    /**
     * @return an optional of this reference. It is the same instance on each call.
     */
    @NonNull
    public Optional<PlayerReference> optional() {
        return this.optional;
    }

    /**
     * @param playerLike an object that {@link #getSafely(Object)} accepts, or an {@link Optional} of one. A string is
     *     compared with the cached name, case-sensitive.
     */
    public <T> boolean is(@Nullable T playerLike) {
        if (playerLike instanceof Optional<?> optional) {
            return optional.map(this::is).orElse(false);
        }
        if (playerLike instanceof PlayerReference) {
            return this.equals(playerLike);
        }
        if (playerLike instanceof UUID) {
            return this.uuid().equals(playerLike);
        }
        if (playerLike instanceof String) {
            return this.name().equals(playerLike);
        }
        if (playerLike instanceof Player player) {
            return this.uuid().equals(player.getUUID());
        }
        if (playerLike instanceof PlayerData playerData) {
            return this.uuid().equals(playerData.uuid());
        }
        return false;
    }

    /**
     * @return the UUID of the player
     */
    public UUID uuid() {
        return this.uuid.get().orElse(Util.NIL_UUID);
    }

    /**
     * @return the name from the name cache. Empty when the name is not in the cache. The value stays the same until
     *     {@link #reset()}.
     */
    public String name() {
        return this.name.get().orElse("?");
    }

    /**
     * Off the server thread, this blocks until the server thread finds the player.
     *
     * @return the online player. Empty when the player is offline or no server runs.
     */
    public Optional<ServerPlayer> entity() {
        var entity = this.entity.get();
        if (entity.isPresent() && entity.get().isRemoved()) {
            this.entity.reset();
            entity = this.entity.get().filter(player -> !player.isRemoved());
        }
        return entity;
    }

    /**
     * {@link #entity()}, but gives {@code null} instead of an empty optional.
     *
     * @return the online player, or {@code null} when the player is offline or no server runs
     */
    @Nullable
    public ServerPlayer entityDirect() {
        return this.entity().orElse(null);
    }

    /**
     * Gets the current position of the online player.
     *
     * @return the position, or {@code null} when the player is offline
     */
    @Nullable
    public LevelPosition position() {
        return this.entity().map(LevelPosition::of).orElse(null);
    }

    /**
     * Loads the data of the player from the mod that owns the type. The reference keeps the data after it loads.
     */
    public <T extends PlayerData> CompletableFuture<T> data(Class<T> type) {
        return this.data(AtlantisConfig.findOwnerMod(type), type);
    }

    /**
     * Loads the data of the player from the mod. The reference keeps the data after it loads, and later calls return
     * it without a new load.
     *
     * @param modId the ID of the mod that owns the data
     * @param type the class of the data
     * @param <T> the type of the data
     * @return a future of the data
     */
    @SuppressWarnings("unchecked")
    public <T extends PlayerData> CompletableFuture<T> data(String modId, Class<T> type) {
        if (this.data.containsKey(type)) {
            return CompletableFuture.completedFuture((T)this.data.get(type));
        }
        return AtlantisConfigProxy.of(modId).get(type, this.uuid()).whenComplete((data, err) -> {
            if (data != null) {
                this.data.put(type, data);
            }
        });
    }

    /**
     * {@link #data(Class)}, but blocks until the data loads.
     */
    public <T extends PlayerData> T dataNow(Class<T> type) {
        return this.dataNow(AtlantisConfig.findOwnerMod(type), type);
    }

    /**
     * {@link #data(String, Class)}, but blocks until the data loads.
     *
     * @param modId the ID of the mod that owns the data
     * @param type the class of the data
     * @param <T> the type of the data
     * @return the data
     */
    @SuppressWarnings("unchecked")
    public <T extends PlayerData> T dataNow(String modId, Class<T> type) {
        if (this.data.containsKey(type)) {
            return (T) this.data.get(type);
        }
        return this.data(modId, type).join();
    }

    /**
     * Gets the Atlantis data of the player with {@link AtlantisData#get(PlayerReference)}.
     *
     * @return a future of the data
     */
    public CompletableFuture<AtlantisPlayer> atlantisData() {
        return AtlantisData.get(this);
    }

    /**
     * Creates a head item of the player with {@link AtlantisData#headItem(UUID)}.
     *
     * @return the head item, or a plain player head if the data is not available yet
     */
    public ItemStack skull() {
        return AtlantisData.headItem(this.uuid());
    }

    /**
     * Sends a new request to Mojang on each call.
     */
    public CompletableFuture<Skin> skin() {
        return MojangAPI.query(SkinQuery.from(this.uuid()));
    }

    /**
     * Gets the network address of the player from {@link NetworkUtils}.
     *
     * @return the address, or empty if it is not known
     */
    public Optional<SocketAddress> address() {
        return NetworkUtils.address(this);
    }

    /**
     * Gets the IP address of the player from {@link NetworkUtils}.
     *
     * @return the IP address, or empty if it is not known
     */
    public Optional<String> ip() {
        return NetworkUtils.ip(this);
    }

    /**
     * Plays the sound to only this player, in the {@link SoundSource#MASTER} source at full volume and normal pitch.
     * Nothing happens when the player is offline.
     *
     * @param sound the sound to play
     */
    public void sound(SoundEvent sound) {
        this.sound(sound, SoundSource.MASTER);
    }

    /**
     * Plays the sound to only this player, at full volume and normal pitch. Nothing happens when the player is offline.
     *
     * @param sound the sound to play
     * @param source the sound source
     */
    public void sound(SoundEvent sound, SoundSource source) {
        this.sound(sound, source, 1F, 1F);
    }

    /**
     * Plays the sound to only this player, in the {@link SoundSource#MASTER} source. Nothing happens when the player
     * is offline.
     *
     * @param sound the sound to play
     * @param volume the volume of the sound
     * @param pitch the pitch of the sound
     */
    public void sound(SoundEvent sound, float volume, float pitch) {
        this.sound(sound, SoundSource.MASTER, volume, pitch);
    }

    /**
     * Plays the sound to only this player. Nothing happens when the player is offline.
     *
     * @param sound the sound to play
     * @param source the sound source
     * @param volume the volume of the sound
     * @param pitch the pitch of the sound
     */
    public void sound(SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.entity().ifPresent(entity -> EntityUtils.playSound(entity, sound, source, volume, pitch));
    }

    /**
     * Sends the message to the player in chat if the player is online.
     *
     * @param message converted with {@link de.waterdu.atlantis.util.text.TextUtils#asComponent(Object)}. Nothing is
     *     sent if null.
     * @param <T> the type of the message
     */
    public <T> void send(T message) {
        MessageUtils.send(message, this);
    }

    /**
     * Opens the UI page for the player with {@link AtlantisUI#open(PlayerReference, Page)}.
     *
     * @param page the page to display. null closes any open page.
     */
    public void open(Page page) {
        AtlantisUI.open(this, page);
    }

    /**
     * Opens the UI page for the player with {@link AtlantisUI#open(PlayerReference, Page, Buttons)}.
     *
     * @param page the page to display. null closes any open page.
     * @param buttons the buttons that override the contents of the page
     */
    public void open(Page page, Buttons buttons) {
        AtlantisUI.open(this, page, buttons);
    }

    /**
     * Opens the UI page for the player with {@link AtlantisUI#open(PlayerReference, Page, boolean)}.
     *
     * @param page the page to display. null closes any open page.
     * @param waitForScreen if {@code true}, waits until the player closes the current page
     */
    public void open(Page page, boolean waitForScreen) {
        AtlantisUI.open(this, page, waitForScreen);
    }

    /**
     * Opens the UI page for the player with {@link AtlantisUI#open(PlayerReference, Page, Buttons, boolean)}.
     *
     * @param page the page to display. null closes any open page.
     * @param buttons the buttons that override the contents of the page
     * @param waitForScreen if {@code true}, waits until the player closes the current page
     */
    public void open(Page page, Buttons buttons, boolean waitForScreen) {
        AtlantisUI.open(this, page, buttons, waitForScreen);
    }

    /**
     * Closes the UI page of the player with {@link AtlantisUI#close}.
     */
    public void close() {
        AtlantisUI.close(this);
    }

    /**
     * Checks the permission with {@link Permissions#hasPermission(PlayerReference, String)}.
     *
     * @param permission the permission node
     * @return {@code true} if the player has the permission. {@code false} when the player is offline.
     */
    public boolean permission(String permission) {
        return Permissions.hasPermission(this, permission);
    }

    /**
     * @return the level of the player, or the overworld when the player is offline
     */
    public ServerLevel level() {
        return this.entity().map(ServerPlayer::serverLevel).orElse(LevelUtils.overworld());
    }

    /**
     * @return null when the player is offline
     */
    public Inventory inventory() {
        return this.entity().map(Player::getInventory).orElse(null);
    }

    /**
     * Clears the cached name and player. Atlantis calls this when the player logs out.
     */
    public void reset() {
        this.known = null;
        this.name.reset();
        this.entity.reset();
    }

    /**
     * {@link #reset()}, then caches this player. Atlantis calls this on login, respawn and dimension change.
     */
    public void reset(ServerPlayer player) {
        this.reset();
        this.known = player;
    }

    /**
     * @return the hash code of the UUID
     */
    @Override
    public int hashCode() {
        return this.uuid().hashCode();
    }

    /**
     * Checks if the other object is a reference with the same UUID.
     *
     * @param obj the object to compare with
     * @return {@code true} if the UUIDs are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlayerReference)) {
            return false;
        }
        return this.uuid().equals(((PlayerReference) obj).uuid());
    }

    /**
     * @return {@code PlayerReference(<uuid>)}
     */
    @Override
    public String toString() {
        return "PlayerReference(" + this.uuid() + ")";
    }

    /**
     * Serializes a reference to a JSON string of its UUID and deserializes it from one.
     */
    public static class TypeAdapter implements JsonSerializer<PlayerReference>, JsonDeserializer<PlayerReference> {

        /**
         * Gets the reference of the UUID in the JSON string with {@link PlayerReference#get(Object)}.
         *
         * @param json the JSON string of the UUID
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the reference. The shared nil reference for the nil UUID.
         */
        @Override
        public PlayerReference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return PlayerReference.get(UUID.fromString(json.getAsString()));
        }

        /**
         * Writes the UUID of the reference as a JSON string.
         *
         * @param src the reference to serialize
         * @param typeOfSrc the type of the reference
         * @param context the serialization context
         * @return the UUID as a JSON string
         */
        @Override
        public JsonElement serialize(PlayerReference src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.uuid().toString());
        }
    }
}
