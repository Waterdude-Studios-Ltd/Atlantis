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

/**
 * Sends messages to players and command sources.
 */
public final class MessageUtils {

    /**
     * Always throws, because this class only has static methods.
     */
    private MessageUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     */
    @SafeVarargs
    public static <T> void send(T message, CommandContext<CommandSourceStack>... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.getSource().sendSystemMessage(component);
            }
        }
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     */
    public static <T> void send(T message, CommandSourceStack... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.sendSystemMessage(component);
            }
        }
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     */
    public static <T> void send(T message, CommandSource... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.sendSystemMessage(component);
            }
        }
    }

    /**
     * {@link #send(Object, boolean, PlayerData...)} without force.
     */
    public static <T> void send(T message, PlayerData... recipients) {
        MessageUtils.send(message, false, recipients);
    }

    /**
     * Sends to the recipients that are online.
     *
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     */
    public static <T> void send(T message, boolean force, PlayerData... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.safeEntity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    /**
     * {@link #send(Object, boolean, PlayerReference...)} without force.
     */
    public static <T> void send(T message, PlayerReference... recipients) {
        MessageUtils.send(message, false, recipients);
    }

    /**
     * Sends to the recipients that are online.
     *
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     */
    public static <T> void send(T message, boolean force, PlayerReference... recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.entity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    /**
     * {@link #send(Object, boolean, Collection)} without force.
     */
    public static <T> void send(T message, Collection<PlayerReference> recipients) {
        MessageUtils.send(message, false, recipients);
    }

    /**
     * Sends to the recipients that are online.
     *
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     */
    public static <T> void send(T message, boolean force, Collection<PlayerReference> recipients) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            for (var recipient : recipients) {
                recipient.entity().ifPresent(player -> player.sendSystemMessage(component, force));
            }
        }
    }

    /**
     * {@link #sendToAllWithPerm(Object, boolean, String)} without force.
     */
    public static <T> void sendToAllWithPerm(T message, String permission) {
        MessageUtils.sendToAllWithPerm(message, false, permission);
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     * @param permission checked with {@link Permissions#hasPermission}
     */
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

    /**
     * {@link #sendToAll(Object, boolean)} without force.
     */
    public static <T> void sendToAll(T message) {
        MessageUtils.sendToAll(message, false);
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     */
    public static <T> void sendToAll(T message, boolean force) {
        if (message != null) {
            var component = TextUtils.asComponent(message);
            ServerUtils.forEachPlayer(player -> player.sendSystemMessage(component, force));
        }
    }

    /**
     * {@link #sendToAllExcept(Object, boolean, Predicate)} without force.
     */
    public static <T> void sendToAllExcept(T message, Predicate<ServerPlayer> predicate) {
        MessageUtils.sendToAllExcept(message, false, predicate);
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     * @param predicate players it matches are skipped
     */
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

    /**
     * {@link #sendToMatching(Object, boolean, Predicate)} without force.
     */
    public static <T> void sendToMatching(T message, Predicate<ServerPlayer> predicate) {
        MessageUtils.sendToMatching(message, false, predicate);
    }

    /**
     * @param message converted with {@link TextUtils#asComponent(Object)}. Nothing is sent if null.
     * @param force shows the message on the action bar instead of in chat, even to players who have chat hidden
     * @param predicate only players it matches receive the message
     */
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
