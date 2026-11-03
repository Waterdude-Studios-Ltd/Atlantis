package de.waterdu.atlantis.permission;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A configurable group of permission patterns with one loader permission node. Each group is a file in the
 * {@code permissions} folder.
 */
@FileBackedObject(mod = Atlantis.ID, path = "permissions", force = true)
public class AtlantisPermissionGroup implements NamedData {

    private final transient UUID uuid = UUID.randomUUID();
    private String name;
    private String node;
    private DefaultPermissionUser defaultUser;
    private int permissionLevel;
    private final List<AtlantisPermission> permissions = new ArrayList<>();

    /**
     * Creates an empty group for deserialization.
     */
    public AtlantisPermissionGroup() {
        // noop
    }

    /**
     * @param node the name of the loader permission node, without the {@code atlantis.} prefix
     * @param defaultUser the players that have the node when no permission handler sets it
     * @param permissionLevel the vanilla permission level that a source that is not a player needs
     * @param permissions the patterns that this group grants. See
     *     {@link AtlantisPermission#AtlantisPermission(String)}.
     */
    public AtlantisPermissionGroup(String name, String node, DefaultPermissionUser defaultUser, int permissionLevel, String... permissions) {
        this.name = name;
        this.node = node;
        this.defaultUser = defaultUser;
        this.permissionLevel = permissionLevel;
        for (var permission : permissions) {
            this.permissions.add(new AtlantisPermission(permission));
        }
    }

    /**
     * @return the name of the group
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Sets the name of the group.
     *
     * @param name the new name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return a random UUID that is made for each instance and is not saved
     */
    @Override
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * Does nothing, because the UUID of a group is not saved.
     *
     * @param uuid ignored
     */
    @Override
    public void setUUID(UUID uuid) {

    }

    /**
     * @return the name of the group
     */
    @Override
    public String identifier() {
        return this.name;
    }

    /**
     * Does not check a player. Use {@link Permissions#hasPermission(net.minecraft.world.entity.player.Player, String)} for that.
     *
     * @return {@code true} if a pattern of this group matches the permission
     */
    public boolean hasPermission(String permission) {
        for (var perm : this.permissions) {
            if (perm.matches(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the loader permission node {@code atlantis.<node>} of this group. The default user decides the value
     * when no permission handler sets it.
     *
     * @return the permission node
     */
    public UPermissionNode<?, Boolean> makeNode() {
        return Mods.modLoader().createBooleanNode(Atlantis.ID, this.node, this.defaultUser::test);
    }

    /**
     * @return the vanilla permission level that a source that is not a player needs
     */
    public int permissionLevel() {
        return this.permissionLevel;
    }
}
