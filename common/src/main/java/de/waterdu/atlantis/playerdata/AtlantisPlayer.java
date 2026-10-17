package de.waterdu.atlantis.playerdata;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.comm.Skin;
import de.waterdu.atlantis.comm.SkinQuery;
import de.waterdu.atlantis.comm.UUIDQuery;
import de.waterdu.atlantis.comm.UsernameQuery;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

@FileBackedObject(mod = Atlantis.ID, path = "players", force = true)
public class AtlantisPlayer implements PlayerData {

    private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private UUID uuid = AtlantisPlayer.NULL_UUID;
    private String name = "";
    private Skin skinObject = new Skin();
    private long lastLogin = -1;
    private long lastSkinFetch = -1;

    public AtlantisPlayer() {}

    public AtlantisPlayer(ServerPlayer player) {
        this(player.getUUID(), player.getName().getString());
    }

    public AtlantisPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        SkinQuery.from(uuid).to(this).query();
    }

    public AtlantisPlayer(String name) {
        this.name = name;
        UUIDQuery.from(name).to(this).query();
    }

    public AtlantisPlayer(UUID uuid) {
        this.uuid = uuid;
        UsernameQuery.from(uuid).to(this).query();
        SkinQuery.from(uuid).to(this).query();
    }

    @Override
    public void onLogin(ServerPlayer player) {
        this.uuid = player.getUUID();
        this.name = player.getName().getString();
        if (this.setLastLogin() > 10800000L) {
            SkinQuery.from(this.uuid).to(this).query();
        }
        this.save();
    }

    @Override
    public void onLogout(ServerPlayer player) {

    }

    public ItemStack headItem() {
        var stack = new ItemStack(Blocks.PLAYER_HEAD, 1);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(new GameProfile(this.uuid, this.name)));
        return stack;
    }

    public synchronized void save() {
        Atlantis.CONFIG.save(this);
    }

    @Override
    public String name() {
        if (this.name.isEmpty() && this.uuid != null && !this.uuid.equals(AtlantisPlayer.NULL_UUID)) {
            UsernameQuery.from(this.uuid).to(this).query();
        }
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ServerPlayer entity() {
        return ServerUtils.player(this.uuid).orElse(null);
    }

    @Override
    public UUID uuid() {
        if (this.uuid == null || this.uuid.equals(AtlantisPlayer.NULL_UUID)) {
            UUIDQuery.from(this.name).to(this).query();
            return null;
        }
        return this.uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String identifier() {
        return this.uuid.toString();
    }

    public long lastLogin() {
        return this.lastLogin;
    }

    public Skin skin() {
        var time = System.currentTimeMillis();
        if (this.lastSkinFetch == -1) {
            this.lastSkinFetch = time;
        }
        if ((!this.skinObject.hasSkin() || time - this.lastSkinFetch >= Settings.get().getSkinRefetchGapSeconds()) && this.uuid() != null) {
            this.lastSkinFetch = time;
            SkinQuery.from(this.uuid).to(this).query();
        }
        return this.skinObject;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long setLastLogin() {
        long lastLogin = this.lastLogin;
        this.setLastLogin(System.currentTimeMillis());
        return this.lastLogin - lastLogin;
    }

    public void setSkinObject(Skin skinObject) {
        this.skinObject = skinObject;
    }

    @Override
    public int hashCode() {
        return this.defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.defaultEquals(obj);
    }

    @Override
    public String toString() {
        return this.defaultToString();
    }
}
