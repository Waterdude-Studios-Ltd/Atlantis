package de.waterdu.atlantis.permission;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests permission matching, accessors and node creation of {@link AtlantisPermissionGroup}.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisPermissionGroupTest {

    /**
     * Checks that a group has a permission when any one of its patterns matches it.
     */
    @Test
    void matchesAnyOfItsPermissions() {
        var group = new AtlantisPermissionGroup("mods", "mods", DefaultPermissionUser.ALL, 1, "atlantis.reload", "*.user.+");

        assertThat(group.hasPermission("atlantis.reload")).isTrue();
        assertThat(group.hasPermission("pixelmon.user.spawn.legendary")).isTrue();
        assertThat(group.hasPermission("atlantis.admin.reload")).isFalse();
    }

    /**
     * Checks that a group with no patterns has no permission.
     */
    @Test
    void groupWithoutPermissionsMatchesNothing() {
        var group = new AtlantisPermissionGroup("empty", "empty", DefaultPermissionUser.ALL, 0);

        assertThat(group.hasPermission("atlantis.reload")).isFalse();
    }

    /**
     * Checks that the group gives back its name, identifier and permission level, and that {@code setName} changes
     * the name.
     */
    @Test
    void exposesNameLevelAndIdentifier() {
        var group = new AtlantisPermissionGroup("admin", "admin", DefaultPermissionUser.OP, 2, "*.admin.+");

        assertThat(group.name()).isEqualTo("admin");
        assertThat(group.identifier()).isEqualTo("admin");
        assertThat(group.permissionLevel()).isEqualTo(2);
        group.setName("renamed");
        assertThat(group.name()).isEqualTo("renamed");
    }

    /**
     * Checks that {@code makeNode} gives a node with the key {@code atlantis.<node>}, and that the default
     * value of the node comes from the {@link DefaultPermissionUser} of the group.
     */
    @Test
    void makeNodeUsesTheAtlantisNamespaceAndDefaultUser() {
        var everyone = new AtlantisPermissionGroup("user", "user", DefaultPermissionUser.ALL, 0, "*.user.+").makeNode();
        var nobody = new AtlantisPermissionGroup("none", "none", DefaultPermissionUser.NONE, 0).makeNode();

        assertThat(everyone.key()).isEqualTo("atlantis.user");
        assertThat(everyone.get(null)).isTrue();
        assertThat(nobody.key()).isEqualTo("atlantis.none");
        assertThat(nobody.get(null)).isFalse();
    }

    /**
     * Checks that a group like the default user group matches user nodes from {@link Permissions#buildNode} but not
     * admin nodes, and checks the values of {@link Permissions#USER} and {@link Permissions#ADMIN}.
     */
    @Test
    void defaultGroupNodesMatchBuiltNodes() {
        var user = new AtlantisPermissionGroup(Permissions.USER_GROUP_NAME, Permissions.USER_GROUP_NAME, DefaultPermissionUser.ALL, 0, "*." + Permissions.USER_GROUP_NAME + ".+");

        assertThat(user.hasPermission(Permissions.buildNode("atlantis", "reload"))).isTrue();
        assertThat(user.hasPermission(Permissions.buildNode("atlantis", "reload", true))).isFalse();
        assertThat(Permissions.USER).isEqualTo("atlantis.user");
        assertThat(Permissions.ADMIN).isEqualTo("atlantis.admin");
    }
}
