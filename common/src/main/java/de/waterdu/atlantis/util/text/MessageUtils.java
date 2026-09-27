package de.waterdu.atlantis.util.text;

import com.mojang.brigadier.context.CommandContext;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.Predicate;

public final class MessageUtils {

    private MessageUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    @SafeVarargs
    public static <T> void send(T message, CommandContext<CommandSourceStack>... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.getSource().sendSystemMessage(component);
            }
        }
    }

    public static <T> void send(T message, CommandSourceStack... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.sendSystemMessage(component);
            }
        }
    }

    public static <T> void send(T message, CommandSource... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.sendSystemMessage(component);
            }
        }
    }

    public static <T> void send(T message, PlayerData... recipients) {
        MessageUtils.send(message, false, recipients);
    }

    public static <T> void send(T message, boolean force, PlayerData... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.safeEntity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    public static <T> void send(T message, PlayerReference... recipients) {
        MessageUtils.send(message, false, recipients);
    }

    public static <T> void send(T message, boolean force, PlayerReference... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.entity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    public static <T> void send(T message, Collection<PlayerReference> recipients) {
        MessageUtils.send(message, false, recipients);
    }

    public static <T> void send(T message, boolean force, Collection<PlayerReference> recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.entity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    public static <T> void sendToAllWithPerm(T message, String permission) {
        MessageUtils.sendToAllWithPerm(message, false, permission);
    }

    public static <T> void sendToAllWithPerm(T message, boolean force, String permission) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            ServerUtils.forEachPlayer(player -> {
                if (Permissions.hasPermission(player, permission)) {
                    player.sendSystemMessage(component, force);
                }
            });
        }
    }

    public static <T> void sendToAll(T message) {
        MessageUtils.sendToAll(message, false);
    }

    public static <T> void sendToAll(T message, boolean force) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            ServerUtils.forEachPlayer(player -> player.sendSystemMessage(component, force));
        }
    }

    public static <T> void sendToAllExcept(T message, Predicate<ServerPlayer> predicate) {
        MessageUtils.sendToAllExcept(message, false, predicate);
    }

    public static <T> void sendToAllExcept(T message, boolean force, Predicate<ServerPlayer> predicate) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            ServerUtils.forEachPlayer(player -> {
                if (!predicate.test(player)) {
                    player.sendSystemMessage(component, force);
                }
            });
        }
    }

    public static <T> void sendToMatching(T message, Predicate<ServerPlayer> predicate) {
        MessageUtils.sendToMatching(message, false, predicate);
    }

    public static <T> void sendToMatching(T message, boolean force, Predicate<ServerPlayer> predicate) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            ServerUtils.forEachPlayer(player -> {
                if (predicate.test(player)) {
                    player.sendSystemMessage(component, force);
                }
            });
        }
    }
}
