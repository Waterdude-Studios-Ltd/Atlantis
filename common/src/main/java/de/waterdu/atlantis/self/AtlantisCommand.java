package de.waterdu.atlantis.self;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.level.schematic.Schematics;
import de.waterdu.atlantis.permission.AtlantisPermissionGroup;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.self.decoration.DecorationEditor;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.test.TestPage;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.BrigadierUtils;
import de.waterdu.atlantis.util.server.CommandUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.MessageUtils;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AtlantisCommand {

    private static final AtomicBoolean READY = new AtomicBoolean(false);
    private static final AtomicBoolean OUTDATED = new AtomicBoolean(false);
    public static final List<String> ATLANTIS_MODS = new ArrayList<>();

    public static final String ID = "atlantis";

    private static final String PIXELMON_ID = "pixelmon";
    private static final String AUTHORS = "authors";
    private static final String ISSUE_TRACKER = "issueTrackerURL";

    public static final String COMMAND_NAME = "atlantis";
    public static final List<String> COMMAND_ALIAS = Lists.newArrayList();
    public static final String PERMISSION = "atlantis.admin.use";

    public static final Text PREFIX = Text.Immutable.of("&0[&#@3079FF-1ABEDCAtlantis&0]&7 ");
    public static final String GRADIENT = "&#@55FFFF-00AAAA";
    public static final String BLUE = "&#73DCFF";

    public static final MutableComponent MODLIST = AtlantisCommand.PREFIX.compile().append(ChatFormatting.GRAY + "Mods: ");
    public static MutableComponent MESSAGE_A = null;
    public static MutableComponent MESSAGE_B = null;
    public static final MutableComponent UPDATE = AtlantisCommand.PREFIX.compile().append(ChatFormatting.DARK_RED + "Out of date mods: ");

    private AtlantisCommand() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void makeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_NAME)
                .requires(source -> source.hasPermission(Permissions.USER))
                .executes(context -> {
                    MessageUtils.send(AtlantisCommand.MESSAGE_B, context);
                    return 0;
                });

        root.then(Commands.literal("mods")
                .requires(source -> source.hasPermission(Permissions.ADMIN))
                .executes(context -> {
                    MessageUtils.send(AtlantisCommand.MODLIST, context);
                    return 0;
                })
        );

        root.then(Commands.literal("reload")
                .requires(CommandUtils.ANY_ADMIN_PREDICATE)
                .executes(context -> {
                    Atlantis.CONFIG.load(Settings.class);
                    Atlantis.CONFIG.load(AtlantisPermissionGroup.class);
                    Atlantis.CONFIG.load(Decorations.class);
                    MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Reloaded Atlantis configuration, permissions, and decorations."), context);
                    return 0;
                })
        );

        root.then(Commands.literal("logpages")
                .requires(CommandUtils.PLAYER_ADMIN_PREDICATE)
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        var player = context.getSource().getPlayer();
                        if (Atlantis.listeners().toggleClasspathTracking(player)) {
                            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Now logging opened page names to chat and console."), context.getSource());
                        } else {
                            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GRAY + "No longer logging opened page names to chat and console."), context.getSource());
                        }
                        return 0;
                    }
                    return -1;
                })
        );

        var schematics = Commands.literal("schematics")
                .requires(CommandUtils.ANY_ADMIN_PREDICATE);

        schematics.then(Commands.literal("undo").executes(context -> {
            var history = Schematics.pollHistory();
            if (history == null) {
                MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.RED + "Nothing to undo!"), context.getSource());
                return 0;
            }
            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GRAY + "Reverting schematic placement."), context.getSource());
            history.restore();
            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Schematic reverted!"), context.getSource());
            return 1;
        }));
        schematics.then(Commands.literal("reload").executes(context -> {
            Schematics.loadAll();
            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Reloaded schematics."), context);
            return Schematics.ids().size();
        }));
        schematics.then(Commands.literal("place")
                .then(Commands.argument("schematic", StringArgumentType.string()).suggests(BrigadierUtils.suggest(Schematics.ids(), SuggestionsBuilder::suggest))
                        .then(Commands.argument("location", BlockPosArgument.blockPos())
                                .then(Commands.argument("strategy", StringArgumentType.string()).suggests(BrigadierUtils.suggest("floor", "airborne"))
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(context -> {
                                                    var schematic = Schematics.get(StringArgumentType.getString(context, "schematic"));
                                                    var location = BlockPosArgument.getBlockPos(context, "location");
                                                    var strategy = StringArgumentType.getString(context, "strategy");
                                                    var floor = strategy.equalsIgnoreCase("floor");
                                                    var airborne = strategy.equalsIgnoreCase("airborne");
                                                    var dimension = DimensionArgument.getDimension(context, "dimension");

                                                    if (schematic == null) {
                                                        MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.RED + "Invalid schematic!"), context);
                                                        return -1;
                                                    }

                                                    if (!floor && !airborne) {
                                                        MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.RED + "Invalid placement strategy!"), context);
                                                        return -1;
                                                    }

                                                    if (floor) {
                                                        while (location.getY() > 0 && dimension.getBlockState(location).isAir()) {
                                                            location = location.below();
                                                        }
                                                        location = location.above();
                                                    }

                                                    var settings = Settings.get();
                                                    var finalPos = new BlockPos(location);

                                                    MessageUtils.send(AtlantisCommand.PREFIX.compile().append(
                                                                    ChatFormatting.GRAY + "Placing schematic " + ChatFormatting.WHITE + schematic.name() +
                                                                            ChatFormatting.GRAY + " at x " + ChatFormatting.WHITE + finalPos.getX() +
                                                                            ChatFormatting.GRAY + " y " + ChatFormatting.WHITE + finalPos.getY() +
                                                                            ChatFormatting.GRAY + " z " + ChatFormatting.WHITE + finalPos.getZ() +
                                                                            ChatFormatting.GRAY + " in level " + ChatFormatting.WHITE + dimension.dimension().location() +
                                                                            ChatFormatting.GRAY + " with placement strategy " + ChatFormatting.WHITE + strategy +
                                                                            ChatFormatting.GRAY + "."
                                                            ),
                                                            context.getSource()
                                                    );

                                                    if (settings.isSchematicAsync()) {
                                                        schematic.placeAsync(
                                                                dimension,
                                                                finalPos,
                                                                settings.isSchematicIgnoreAir(),
                                                                settings.getSchematicBlocksPerStep(),
                                                                settings.getSchematicTicksPerStep()
                                                        ).whenComplete((result, err) ->
                                                                MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Schematic placed!"), context.getSource())
                                                        );
                                                    } else {
                                                        Concurrency.sync(() -> {
                                                            schematic.place(dimension, finalPos, 1, settings.isSchematicIgnoreAir());
                                                            MessageUtils.send(AtlantisCommand.PREFIX.compile().append(ChatFormatting.GREEN + "Schematic placed!"), context.getSource());
                                                        });
                                                    }

                                                    return 0;
                                                })
                                        )
                                )
                        )
                )
        );

        root.then(schematics);

        root.then(Commands.literal("decorate")
                .requires(CommandUtils.PLAYER_ADMIN_PREDICATE)
                .then(Commands.argument("page", StringArgumentType.string()).suggests(BrigadierUtils.suggest(Atlantis.listeners().pageNames(), SuggestionsBuilder::suggest))
                        .executes(context -> {
                            if (context.getSource().isPlayer()) {
                                var player = context.getSource().getPlayer();
                                var page = Atlantis.listeners().getPage(StringArgumentType.getString(context, "page"));
                                if (page != null) {
                                    new DecorationEditor(page).open(player);
                                    return 0;
                                }
                            }
                            return -1;
                        })
                )
        );

        root.then(Commands.literal("testpage")
                .requires(CommandUtils.PLAYER_ADMIN_PREDICATE)
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        var player = context.getSource().getPlayer();
                        new TestPage().open(player);
                        return 0;
                    }
                    return -1;
                }));

        root.then(Commands.literal("levelinfo")
                .requires(CommandUtils.PLAYER_ADMIN_PREDICATE)
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof Entity entity) {
                        if (entity.level() instanceof ServerLevel level) {
                            MessageUtils.send(AtlantisCommand.PREFIX.append(String.valueOf(ChatFormatting.AQUA) + ChatFormatting.BOLD + "Level Info"), context.getSource());
                            MessageUtils.send(ChatFormatting.GRAY + "Dimension Key: " + ChatFormatting.WHITE + level.dimension().location(), context.getSource());
                            MessageUtils.send(ChatFormatting.GRAY + "Chunk Generator: " + ChatFormatting.WHITE + level.getChunkSource().getGenerator().getClass().getSimpleName(), context.getSource());
                            MessageUtils.send(ChatFormatting.GRAY + "Dimension Type: " + ChatFormatting.WHITE + ServerUtils.key(Registries.DIMENSION_TYPE, level.dimensionType()), context.getSource());
                            MessageUtils.send(ChatFormatting.GRAY + "Level Seed: " + ChatFormatting.WHITE + level.getSeed(), context.getSource());
                            return 0;
                        }
                    } else {
                        MessageUtils.send(AtlantisCommand.PREFIX.append(ChatFormatting.RED + "Not in a level!"), context.getSource());
                    }
                    return -1;
                }));

        root.then(Commands.literal("goto")
                .requires(CommandUtils.PLAYER_ADMIN_PREDICATE)
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> {
                            if (context.getSource().isPlayer()) {
                                var entity = context.getSource().getPlayer();
                                var level = DimensionArgument.getDimension(context, "dimension");
                                entity.teleportTo(level, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                                MessageUtils.send(AtlantisCommand.PREFIX.append(ChatFormatting.GRAY + "Teleported to " + level.dimension().location() + "."), context.getSource());
                                return 0;
                            }
                            return -1;
                        })
                ));

        dispatcher.register(root);
    }

    public static void setup(Consumer<Runnable> enqueue) {
        enqueue.accept(() -> Atlantis.THREAD_POOL.schedule(() -> {
            var messageA = new StringBuilder();
            var messageB = new StringBuilder();
            var atlantisMod = "";
            var first = true;
            for (var mod : FMLLoader.getLoadingModList().getMods()) {
                var isPixelmon = mod.getModId().equalsIgnoreCase(AtlantisCommand.PIXELMON_ID);

                var component = Text.of(mod.getDisplayName()).compile();

                var version = VersionChecker.getResult(mod);

                var status = version == null ? VersionChecker.Status.FAILED : version.status();
                var latest = version == null || version.target() == null ? "?" : version.target().toString();
                var tracker = mod.getOwningFile().getIssueURL();

                var style = Style.EMPTY;
                var hover = "";
                if (tracker != null) {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, tracker.toString()));
                    hover = "\n\n" + ChatFormatting.AQUA + "Click to open issue tracker";
                }

                var outdated = false;

                if (status == VersionChecker.Status.UP_TO_DATE || status == VersionChecker.Status.BETA) {
                    latest = mod.getVersion().toString();

                    var beta = status == VersionChecker.Status.BETA ?
                            ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD + "β" + ChatFormatting.RESET + ChatFormatting.LIGHT_PURPLE + " Up to date (beta)" :
                            ChatFormatting.GREEN + "" + ChatFormatting.BOLD + "✓" + ChatFormatting.RESET + ChatFormatting.GREEN + " Up to date";
                    style = style.withColor(ChatFormatting.GREEN);
                    hover = ChatFormatting.GRAY + "Name: " + ChatFormatting.WHITE + mod.getDisplayName() + "\n" +
                            ChatFormatting.GRAY + "ID: " + ChatFormatting.WHITE + mod.getModId() + "\n\n" +
                            ChatFormatting.GRAY + "Version: " + ChatFormatting.GREEN + latest + "\n" +
                            ChatFormatting.GRAY + "Latest: " + ChatFormatting.WHITE + latest + "\n\n" +
                            beta + hover;
                } else if (status == VersionChecker.Status.OUTDATED || status == VersionChecker.Status.BETA_OUTDATED) {
                    style = style.withColor(ChatFormatting.RED);
                    hover = ChatFormatting.GRAY + "Name: " + ChatFormatting.WHITE + mod.getDisplayName() + "\n" +
                            ChatFormatting.GRAY + "ID: " + ChatFormatting.WHITE + mod.getModId() + "\n\n" +
                            ChatFormatting.GRAY + "Version: " + ChatFormatting.RED + mod.getVersion().toString() + "\n" +
                            ChatFormatting.GRAY + "Latest: " + ChatFormatting.WHITE + latest + "\n\n" +
                            ChatFormatting.RED + ChatFormatting.BOLD + "✗" + ChatFormatting.RESET + ChatFormatting.RED + " Outdated" + (status == VersionChecker.Status.BETA_OUTDATED ? " (beta)" : "") + hover;
                    outdated = true;
                } else if (status == VersionChecker.Status.AHEAD) {
                    style = style.withColor(ChatFormatting.GOLD);
                    hover = ChatFormatting.GRAY + "Name: " + ChatFormatting.WHITE + mod.getDisplayName() + "\n" +
                            ChatFormatting.GRAY + "ID: " + ChatFormatting.WHITE + mod.getModId() + "\n\n" +
                            ChatFormatting.GRAY + "Version: " + ChatFormatting.GOLD + mod.getVersion().toString() + "\n" +
                            ChatFormatting.GRAY + "Latest: " + ChatFormatting.WHITE + latest + "\n\n" +
                            ChatFormatting.GOLD + "★" + ChatFormatting.RESET + ChatFormatting.GOLD + " Ahead of latest" + hover;
                } else {
                    style = style.withColor(ChatFormatting.YELLOW);
                    hover = ChatFormatting.GRAY + "Name: " + ChatFormatting.WHITE + mod.getDisplayName() + "\n" +
                            ChatFormatting.GRAY + "ID: " + ChatFormatting.WHITE + mod.getModId() + "\n\n" +
                            ChatFormatting.GRAY + "Version: " + ChatFormatting.YELLOW + mod.getVersion().toString() + "\n" +
                            ChatFormatting.GRAY + "Latest: " + ChatFormatting.WHITE + latest + "\n\n" +
                            ChatFormatting.RED + ChatFormatting.BOLD + "▲" + ChatFormatting.RESET + ChatFormatting.RED + " Failed to fetch info" + hover;
                }

                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover)));

                component.setStyle(style);

                if (outdated) {
                    if (AtlantisCommand.OUTDATED.get()) {
                        AtlantisCommand.UPDATE.append(ChatFormatting.DARK_RED + ", ");
                    }
                    AtlantisCommand.UPDATE.append(component);
                    AtlantisCommand.OUTDATED.set(true);
                }

                if (!first) {
                    AtlantisCommand.MODLIST.append(ChatFormatting.GRAY + ", ");
                }
                AtlantisCommand.MODLIST.append(component);
                first = false;

                var authors = mod.getConfigElement(AtlantisCommand.AUTHORS).orElse("");
                if (isPixelmon || (authors instanceof String && ((String) authors).contains(Atlantis.WATER_NAME))) {
                    if (!atlantisMod.isEmpty()) {
                        if (messageA.isEmpty()) {
                            messageA.append(AtlantisCommand.PREFIX.get()).append(AtlantisCommand.BLUE).append("Water, the developer of ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                            messageB.append(AtlantisCommand.PREFIX.get()).append(AtlantisCommand.BLUE).append("By Water, powered by Atlantis: ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                        } else {
                            messageA.append(AtlantisCommand.BLUE).append(", ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                            messageB.append(AtlantisCommand.BLUE).append(", ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                        }
                    }
                    atlantisMod = ChatFormatting.stripFormatting(mod.getDisplayName());
                    AtlantisCommand.ATLANTIS_MODS.add(mod.getModId());
                }
            }

            if (!atlantisMod.isEmpty()) {
                if (messageA.isEmpty()) {
                    messageA.append(AtlantisCommand.PREFIX.get()).append(AtlantisCommand.BLUE).append("Water, the developer of ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                    messageB.append(AtlantisCommand.PREFIX.get()).append(AtlantisCommand.BLUE).append("By Water, powered by Atlantis: ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                } else {
                    messageA.append(AtlantisCommand.BLUE).append(" and ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                    messageB.append(AtlantisCommand.BLUE).append(" and ").append(AtlantisCommand.GRADIENT).append(atlantisMod);
                }
            }
            messageA.append(AtlantisCommand.BLUE).append(", has joined!");
            AtlantisCommand.MESSAGE_A = Text.of(messageA.toString()).compile();
            messageB.append(AtlantisCommand.BLUE).append(".").append(AtlantisCommand.BLUE).append("[L]").append(" Click for more information!");
            AtlantisCommand.MESSAGE_B = Text.of(messageB.toString()).compile();
            AtlantisCommand.MESSAGE_B.setStyle(AtlantisCommand.MESSAGE_B.getStyle()
                    .withHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.of(AtlantisCommand.BLUE + "Click to visit Atlantis Development!").compile()
                            )
                    ).withClickEvent(
                            new ClickEvent(
                                    ClickEvent.Action.OPEN_URL,
                                    "https://www.discord.gg/Ff3bEwb"
                            )
                    )
            );
            AtlantisLogger.info("Atlantis is ready.");
            AtlantisCommand.READY.set(true);
        }, 30, TimeUnit.SECONDS));
    }

    public static void login(ServerPlayer player) {
        if (AtlantisCommand.READY.get()) {
            if (AtlantisCommand.OUTDATED.get() && Permissions.hasPermission(player, AtlantisCommand.PERMISSION)) {
                MessageUtils.send(AtlantisCommand.UPDATE, player);
            }
            if (Settings.get().isAtlantisPublicMessage() && Atlantis.isPlayerWater(player)) {
                MessageUtils.sendToAll(AtlantisCommand.MESSAGE_A);
            }
        }
    }

    public static boolean isReady() {
        return AtlantisCommand.READY.get();
    }

    public static int countAtlantisMods() {
        var count = 0;
        for (var mod : FMLLoader.getLoadingModList().getMods()) {
            var authors = mod.getConfigElement(AtlantisCommand.AUTHORS).orElse("");
            if (authors instanceof String authorString && authorString.contains(Atlantis.WATER_NAME)) {
                count++;
            }
        }
        return count;
    }
}
