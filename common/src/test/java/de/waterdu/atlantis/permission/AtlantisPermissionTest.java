package de.waterdu.atlantis.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests the pattern matching, validation and JSON adapter of {@link AtlantisPermission}.
 */
class AtlantisPermissionTest {

    /**
     * Checks that a permission pattern matches a permission string only when expected.
     *
     * @param node the permission pattern
     * @param permission the permission string to match
     * @param expected {@code true} if the pattern must match the permission
     */
    @ParameterizedTest
    @CsvSource({
            "atlantis.command,      atlantis.command,           true",
            "atlantis.command,      atlantis.commands,          false",
            "atlantis.command,      atlantisXcommand,           false",
            "atlantis.*,            atlantis.reload,            true",
            "atlantis.*,            atlantis.reload.all,        false",
            "atlantis.*,            atlantis.r3load,            true",
            "atlantis.*,            atlantis.3load,             false",
            "*.user.+,              my_mod.user.reload,         true",
            "*.user.+,              my-mod.user.give_all,       true",
            "*.user.+,              _mod.user.reload,           false",
            "atlantis.*,            atlantis.r,                 true",
            "atlantis.*,            atlantis,                   false",
            "atlantis.+,            atlantis.reload.all,        true",
            "atlantis.+,            atlantis.,                  false",
            "atlantis.+,            atlantis,                   true",
            "atlantis.+,            atlantisfoo,                false",
            "atlantis.?,            atlantis.,                  true",
            "atlantis.?,            atlantis.a.b2,              true",
            "atlantis.?,            atlantis,                   false",
            "+,                     atlantis.reload,            true",
            "atlantis.^.*,          atlantis.3.reload,          true",
            "atlantis.limit.^,      atlantis.limit.42,          true",
            "atlantis.limit.^,      atlantis.limit.x,           false",
    })
    void matches(String node, String permission, boolean expected) {
        assertThat(new AtlantisPermission(node).matches(permission)).isEqualTo(expected);
    }

    /**
     * Checks that a node from {@link Permissions#buildNode} matches the pattern of its own default group and not the
     * pattern of the other group.
     *
     * @param domain the domain of the node
     * @param admin {@code true} for an admin node, {@code false} for a user node
     * @param qualifiers the qualifiers of the node, separated by {@code .}, or an empty string for none
     */
    @ParameterizedTest
    @CsvSource({
            "atlantis, false, ''",
            "mod2,     true,  'give.all'",
            "my_mod,   false, 'give_all'",
    })
    void defaultGroupPatternsMatchBuiltNodes(String domain, boolean admin, String qualifiers) {
        var node = Permissions.buildNode(domain, "reload", admin, qualifiers.isEmpty() ? new String[0] : qualifiers.split("\\."));
        var own = new AtlantisPermission("*." + (admin ? "admin" : "user") + ".+");
        var other = new AtlantisPermission("*." + (admin ? "user" : "admin") + ".+");

        assertThat(own.matches(node)).isTrue();
        assertThat(other.matches(node)).isFalse();
    }

    /**
     * Checks that the constructor throws {@link IllegalArgumentException} for an invalid node.
     *
     * @param node the invalid node
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "atlantis command", "atlantis_command"})
    void rejectsInvalidNodes(String node) {
        assertThatIllegalArgumentException().isThrownBy(() -> new AtlantisPermission(node));
    }

    /**
     * Checks that the type adapter reads a permission from a JSON string and writes it back as the same string.
     */
    @Test
    void typeAdapterRoundTrips() {
        var gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(AtlantisPermission.class, new AtlantisPermission.TypeAdapter())
                .create();
        var permission = gson.fromJson("\"atlantis.*\"", AtlantisPermission.class);

        assertThat(permission.matches("atlantis.reload")).isTrue();
        assertThat(gson.toJson(permission)).isEqualTo("\"atlantis.*\"");
    }
}
