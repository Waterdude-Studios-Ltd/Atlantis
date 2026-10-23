package de.waterdu.atlantis.permission;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisPermissionGroupTest {

    @Test
    void matchesAnyOfItsPermissions() {
        var group = new AtlantisPermissionGroup("mods", "mods", DefaultPermissionUser.ALL, 1, "atlantis.reload", "*.user.+");

        assertThat(group.hasPermission("atlantis.reload")).isTrue();
        assertThat(group.hasPermission("pixelmon.user.spawn.legendary")).isTrue();
        assertThat(group.hasPermission("atlantis.admin.reload")).isFalse();
    }

    @Test
    void groupWithoutPermissionsMatchesNothing() {
        var group = new AtlantisPermissionGroup("empty", "empty", DefaultPermissionUser.ALL, 0);

        assertThat(group.hasPermission("atlantis.reload")).isFalse();
    }

    @Test
    void exposesNameLevelAndIdentifier() {
        var group = new AtlantisPermissionGroup("admin", "admin", DefaultPermissionUser.OP, 2, "*.admin.+");

        assertThat(group.name()).isEqualTo("admin");
        assertThat(group.identifier()).isEqualTo("admin");
        assertThat(group.permissionLevel()).isEqualTo(2);
        group.setName("renamed");
        assertThat(group.name()).isEqualTo("renamed");
    }

    @Test
    void makeNodeUsesTheAtlantisNamespaceAndDefaultUser() {
        var everyone = new AtlantisPermissionGroup("user", "user", DefaultPermissionUser.ALL, 0, "*.user.+").makeNode();
        var nobody = new AtlantisPermissionGroup("none", "none", DefaultPermissionUser.NONE, 0).makeNode();

        assertThat(everyone.key()).isEqualTo("atlantis.user");
        assertThat(everyone.get(null)).isTrue();
        assertThat(nobody.key()).isEqualTo("atlantis.none");
        assertThat(nobody.get(null)).isFalse();
    }

    @Test
    void defaultGroupNodesMatchBuiltNodes() {
        var user = new AtlantisPermissionGroup(Permissions.USER_GROUP_NAME, Permissions.USER_GROUP_NAME, DefaultPermissionUser.ALL, 0, "*." + Permissions.USER_GROUP_NAME + ".+");

        assertThat(user.hasPermission(Permissions.buildNode("atlantis", "reload"))).isTrue();
        assertThat(user.hasPermission(Permissions.buildNode("atlantis", "reload", true))).isFalse();
        assertThat(Permissions.USER).isEqualTo("atlantis.user");
        assertThat(Permissions.ADMIN).isEqualTo("atlantis.admin");
    }
}
