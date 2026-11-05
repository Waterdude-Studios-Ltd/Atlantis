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
import java.util.concurrent.TimeUnit;

/**
 * Stores the name, the skin and the last login time of a player. Each player is a file in the {@code players}
 * folder.
 */
@FileBackedObject(mod = Atlantis.ID, path = "players", force = true)
public class AtlantisPlayer implements PlayerData {

    private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private UUID uuid = AtlantisPlayer.NULL_UUID;
    private String name = "";
    private Skin skinObject = new Skin();
    private long lastLogin = -1;
    private long lastSkinFetch = -1;

    /**
     * Creates empty player data with a nil UUID and an empty name.
     */
    public AtlantisPlayer() {}

    /**
     * Creates player data from the UUID and the name of the player, and looks up the skin in the background.
     *
     * @param player the player
     */
    public AtlantisPlayer(ServerPlayer player) {
        this(player.getUUID(), player.getName().getString());
    }

    /**
     * Looks up the skin from Mojang in the background.
     *
     * @param uuid the UUID of the player
     * @param name the name of the player
     */
    public AtlantisPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        SkinQuery.from(uuid).to(this).query();
    }

    /**
     * Looks up the UUID from Mojang in the background.
     */
    public AtlantisPlayer(String name) {
        this.name = name;
        UUIDQuery.from(name).to(this).query();
    }

    /**
     * Looks up the name and the skin from Mojang in the background.
     */
    public AtlantisPlayer(UUID uuid) {
        this.uuid = uuid;
        UsernameQuery.from(uuid).to(this).query();
        SkinQuery.from(uuid).to(this).query();
    }

    /**
     * Updates the UUID, the name and the last login time, then saves. Looks up the skin in the background when the
     * previous login was more than three hours ago.
     *
     * @param player the player that logged in
     */
    @Override
    public void onLogin(ServerPlayer player) {
        this.uuid = player.getUUID();
        this.name = player.getName().getString();
        if (this.setLastLogin() > 10800000L) {
            SkinQuery.from(this.uuid).to(this).query();
        }
        this.save();
    }

    /**
     * Does nothing.
     *
     * @param player the player that logged out
     */
    @Override
    public void onLogout(ServerPlayer player) {

    }

    /**
     * Creates a player head with the profile of this player.
     *
     * @return a stack of one player head
     */
    public ItemStack headItem() {
        var stack = new ItemStack(Blocks.PLAYER_HEAD, 1);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(new GameProfile(this.uuid, this.name)));
        return stack;
    }

    /**
     * Saves this player data.
     */
    public synchronized void save() {
        Atlantis.CONFIG.save(this);
    }

    /**
     * Gets the name of the player. If the name is empty and the UUID is known, starts a name lookup in the
     * background.
     *
     * @return the name. It is empty until the lookup completes.
     */
    @Override
    public String name() {
        if (this.name.isEmpty() && this.uuid != null && !this.uuid.equals(AtlantisPlayer.NULL_UUID)) {
            UsernameQuery.from(this.uuid).to(this).query();
        }
        return this.name;
    }

    /**
     * Sets the name of the player.
     *
     * @param name the new name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the player, or {@code null} if the player is not online
     */
    @Override
    public ServerPlayer entity() {
        return ServerUtils.player(this.uuid).orElse(null);
    }

    /**
     * Gets the UUID of the player. If the UUID is not known, starts a UUID lookup by name in the background.
     *
     * @return the UUID, or {@code null} until the lookup completes
     */
    @Override
    public UUID uuid() {
        if (this.uuid == null || this.uuid.equals(AtlantisPlayer.NULL_UUID)) {
            UUIDQuery.from(this.name).to(this).query();
            return null;
        }
        return this.uuid;
    }

    /**
     * Sets the UUID of the player.
     *
     * @param uuid the new UUID
     */
    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the UUID of the player as a string
     */
    @Override
    public String identifier() {
        return this.uuid.toString();
    }

    /**
     * @return the time of the last login in epoch milliseconds, or -1 if the player never logged in
     */
    public long lastLogin() {
        return this.lastLogin;
    }

    /**
     * Starts a new skin lookup in the background when there is no skin or the skin is old.
     *
     * @return the current skin. It does not wait for the lookup.
     */
    public Skin skin() {
        var time = System.currentTimeMillis();
        if (this.lastSkinFetch == -1) {
            this.lastSkinFetch = time;
        }
        if ((!this.skinObject.hasSkin() || time - this.lastSkinFetch >= TimeUnit.SECONDS.toMillis(Settings.get().getSkinRefetchGapSeconds())) && this.uuid() != null) {
            this.lastSkinFetch = time;
            SkinQuery.from(this.uuid).to(this).query();
        }
        return this.skinObject;
    }

    /**
     * Sets the time of the last login.
     *
     * @param lastLogin the time in epoch milliseconds
     */
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Sets the last login to now.
     *
     * @return the milliseconds since the previous login
     */
    public long setLastLogin() {
        long lastLogin = this.lastLogin;
        this.setLastLogin(System.currentTimeMillis());
        return this.lastLogin - lastLogin;
    }

    /**
     * Sets the skin of the player.
     *
     * @param skinObject the new skin
     */
    public void setSkinObject(Skin skinObject) {
        this.skinObject = skinObject;
    }

    /**
     * Gets the hash code with {@link #defaultHashCode()}.
     *
     * @return the hash code of the UUID
     */
    @Override
    public int hashCode() {
        return this.defaultHashCode();
    }

    /**
     * Compares with {@link #defaultEquals(Object)}.
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is this instance, or player data with the same UUID
     */
    @Override
    public boolean equals(Object obj) {
        return this.defaultEquals(obj);
    }

    /**
     * Gets the string with {@link #defaultToString()}.
     *
     * @return the simple class name followed by the UUID in parentheses
     */
    @Override
    public String toString() {
        return this.defaultToString();
    }
}
