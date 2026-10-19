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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    @Nonnull
    public static <T> PlayerReference get(@Nullable T player) {
        return PlayerReference.getSafely(player).orElse(NULL);
    }

    @Nonnull
    public Optional<PlayerReference> optional() {
        return this.optional;
    }

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

    public UUID uuid() {
        return this.uuid.get().orElse(Util.NIL_UUID);
    }

    public String name() {
        return this.name.get().orElse("?");
    }

    public Optional<ServerPlayer> entity() {
        var entity = this.entity.get();
        if (entity.isPresent() && entity.get().isRemoved()) {
            this.entity.reset();
            entity = this.entity.get().filter(player -> !player.isRemoved());
        }
        return entity;
    }

    @Nullable
    public ServerPlayer entityDirect() {
        return this.entity().orElse(null);
    }

    @Nullable
    public LevelPosition position() {
        return this.entity().map(LevelPosition::of).orElse(null);
    }

    public <T extends PlayerData> CompletableFuture<T> data(Class<T> type) {
        return this.data(AtlantisConfig.findOwnerMod(type), type);
    }

    @SuppressWarnings("unchecked")
    public <T extends PlayerData> CompletableFuture<T> data(String modid, Class<T> type) {
        if (this.data.containsKey(type)) {
            return CompletableFuture.completedFuture((T)this.data.get(type));
        }
        return AtlantisConfigProxy.of(modid).get(type, this.uuid()).whenComplete((data, err) -> {
            if (data != null) {
                this.data.put(type, data);
            }
        });
    }

    public <T extends PlayerData> T dataNow(Class<T> type) {
        return this.dataNow(AtlantisConfig.findOwnerMod(type), type);
    }

    @SuppressWarnings("unchecked")
    public <T extends PlayerData> T dataNow(String modid, Class<T> type) {
        if (this.data.containsKey(type)) {
            return (T) this.data.get(type);
        }
        return this.data(modid, type).join();
    }

    public CompletableFuture<AtlantisPlayer> atlantisData() {
        return AtlantisData.get(this);
    }

    public ItemStack skull() {
        return AtlantisData.headItem(this.uuid());
    }

    public CompletableFuture<Skin> skin() {
        return MojangAPI.query(SkinQuery.from(this.uuid()));
    }

    public Optional<SocketAddress> address() {
        return NetworkUtils.address(this);
    }

    public Optional<String> ip() {
        return NetworkUtils.ip(this);
    }

    public void sound(SoundEvent sound) {
        this.sound(sound, SoundSource.MASTER);
    }

    public void sound(SoundEvent sound, SoundSource source) {
        this.sound(sound, source, 1F, 1F);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        this.sound(sound, SoundSource.MASTER, volume, pitch);
    }

    public void sound(SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.entity().ifPresent(entity -> EntityUtils.playSound(entity, sound, source, volume, pitch));
    }

    public <T> void send(T message) {
        MessageUtils.send(message, this);
    }

    public void open(Page page) {
        AtlantisUI.open(this, page);
    }

    public void open(Page page, Buttons buttons) {
        AtlantisUI.open(this, page, buttons);
    }

    public void open(Page page, boolean waitForScreen) {
        AtlantisUI.open(this, page, waitForScreen);
    }

    public void open(Page page, Buttons buttons, boolean waitForScreen) {
        AtlantisUI.open(this, page, buttons, waitForScreen);
    }

    public void close() {
        AtlantisUI.close(this);
    }

    public boolean permission(String permission) {
        return Permissions.hasPermission(this, permission);
    }

    public ServerLevel level() {
        return this.entity().map(ServerPlayer::serverLevel).orElse(LevelUtils.overworld());
    }

    public Inventory inventory() {
        return this.entity().map(Player::getInventory).orElse(null);
    }

    public void reset() {
        this.known = null;
        this.name.reset();
        this.entity.reset();
    }

    public void reset(ServerPlayer player) {
        this.reset();
        this.known = player;
    }

    @Override
    public int hashCode() {
        return this.uuid().hashCode();
    }

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

    @Override
    public String toString() {
        return "PlayerReference(" + this.uuid() + ")";
    }

    public static class TypeAdapter implements JsonSerializer<PlayerReference>, JsonDeserializer<PlayerReference> {

        @Override
        public PlayerReference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return PlayerReference.get(UUID.fromString(json.getAsString()));
        }

        @Override
        public JsonElement serialize(PlayerReference src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.uuid().toString());
        }
    }
}
