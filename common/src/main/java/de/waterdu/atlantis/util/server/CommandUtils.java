package de.waterdu.atlantis.util.server;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class CommandUtils {

    public static final Predicate<CommandSourceStack> ANY_USER_PREDICATE = stack -> Permissions.hasPermission(stack, Permissions.USER);
    public static final Predicate<CommandSourceStack> ANY_PLAYER_PREDICATE = stack -> stack.isPlayer();
    public static final Predicate<CommandSourceStack> PLAYER_USER_PREDICATE = ANY_USER_PREDICATE.and(ANY_PLAYER_PREDICATE);
    public static final Predicate<CommandSourceStack> ANY_ADMIN_PREDICATE = stack -> Permissions.hasPermission(stack, Permissions.ADMIN);
    public static final Predicate<CommandSourceStack> PLAYER_ADMIN_PREDICATE = ANY_ADMIN_PREDICATE.and(ANY_PLAYER_PREDICATE);
    public static final Predicate<CommandSourceStack> ANY_ENTITY_PREDICATE = stack -> stack.getEntity() != null;
    public static final Predicate<CommandSourceStack> ENTITY_ADMIN_PREDICATE = ANY_ADMIN_PREDICATE.and(ANY_ENTITY_PREDICATE);

    private CommandUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static boolean doesStringStartWith(String original, String region) {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibilities) {
        return CommandUtils.getListOfStringsMatchingLastWord(args, Lists.newArrayList(possibilities));
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions) {
        var s = inputArgs[inputArgs.length - 1];
        var list = new ArrayList<String>();

        if (!possibleCompletions.isEmpty()) {
            for (var s1 : possibleCompletions.stream().map(Functions.toStringFunction()).toList()) {
                if (CommandUtils.doesStringStartWith(s, s1)) {
                    list.add(s1);
                }
            }

            if (list.isEmpty()) {
                for (var object : possibleCompletions) {
                    if (object instanceof ResourceLocation rl && CommandUtils.doesStringStartWith(s, rl.getPath())) {
                        list.add(rl.toString());
                    }
                }
            }
        }

        return list;
    }
}
