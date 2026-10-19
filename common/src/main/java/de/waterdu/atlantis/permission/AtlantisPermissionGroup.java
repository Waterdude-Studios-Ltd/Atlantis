package de.waterdu.atlantis.permission;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@FileBackedObject(mod = Atlantis.ID, path = "permissions", force = true)
public class AtlantisPermissionGroup implements NamedData {

    private final transient UUID uuid = UUID.randomUUID();
    private String name;
    private String node;
    private DefaultPermissionUser defaultUser;
    private int permissionLevel;
    private final List<AtlantisPermission> permissions = new ArrayList<>();

    public AtlantisPermissionGroup() {
        // noop
    }

    public AtlantisPermissionGroup(String name, String node, DefaultPermissionUser defaultUser, int permissionLevel, String... permissions) {
        this.name = name;
        this.node = node;
        this.defaultUser = defaultUser;
        this.permissionLevel = permissionLevel;
        for (var permission : permissions) {
            this.permissions.add(new AtlantisPermission(permission));
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public void setUUID(UUID uuid) {

    }

    @Override
    public String identifier() {
        return this.name;
    }

    public boolean hasPermission(String permission) {
        for (var perm : this.permissions) {
            if (perm.matches(permission)) {
                return true;
            }
        }
        return false;
    }

    public UPermissionNode<?, Boolean> makeNode() {
        return Mods.modLoader().createBooleanNode(Atlantis.ID, this.node, this.defaultUser::test);
    }

    public int permissionLevel() {
        return this.permissionLevel;
    }
}
