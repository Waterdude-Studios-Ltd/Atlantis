package de.waterdu.atlantis;

import com.google.common.collect.Lists;
import de.waterdu.atlantis.file.AonComment;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Database;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the global Atlantis settings, loaded from {@code atlantis.aon} before every other config.
 */
@FileBackedObject(mod = Atlantis.ID, path = "atlantis.aon", order = LoadOrder.FIRST)
@AonComment(value = {"Atlantis global settings", "These settings affect everything using Atlantis.", "Be sure to periodically update this file, ensuring you have good settings for performance!"}, indents = {3,2,2})
public class Settings implements Configuration {

    private static final Settings DEFAULTS = new Settings();

    @AonComment({"The number of threads Atlantis should use.", "-1 will automatically assign a value based on the number of plugins in use.", "Default: -1"})
    private int threads = -1;

    @AonComment({"Whether to save files automatically, asynchronously.", "Default: true"})
    private boolean useAsyncAutosaver = true;

    @AonComment({"When player data should be invalidated on player join/leave.", "Options: NEVER, DATABASE, ALWAYS.", "Default: DATABASE"})
    private EvictionMode playerDataEvictionAlgorithm = EvictionMode.DATABASE;

    @AonComment({"How many seconds between each async autosave, if enabled.", "Default: 60"})
    private long asyncSaveDelaySeconds = 60;

    @AonComment({"How many milliseconds to put between each individual file save.", "Default: 30"})
    private long dataSaveDelay = 30;

    @AonComment({"How many milliseconds between each attempt at executing config scheduled tasks.", "Default: 500"})
    private long scheduledTaskDelay = 500;

    @AonComment({"How many milliseconds a second click must be within on a page to be considered a double click.", "Default: 500"})
    private long doubleClickDelay = 250;

    @AonComment({"How many milliseconds to put between a duplicate file save and its retry.", "Default: 50"})
    private long duplicateSaveDelay = 50;

    @AonComment({"Whether to use the error bin instead of renaming and marking files as errored.", "Default: false"})
    private boolean useErrorBin = false;

    @AonComment({"Logging level at which load messages are logged to.", "Options: ERROR, WARN, INFO, DEBUG, NONE", "Default: DEBUG"})
    private AtlantisLogger.Severity logLoadMessages = AtlantisLogger.Severity.DEBUG;

    @AonComment({"Logging level at which intercepted packets are logged to.", "Options: ERROR, WARN, INFO, DEBUG, NONE", "Default: NONE"})
    private AtlantisLogger.Severity logPackets = AtlantisLogger.Severity.NONE;

    @AonComment({"Whether a present and registered placeholder API should be used in the processing of text in pages.", "Default: true"})
    private boolean usePlaceholdersInPages = true;

    @AonComment({"How many seconds until a cached player skin is invalidated and is gotten from Mojang again.", "Default: 120960"})
    private long skinRefetchGapSeconds = 120960;

    @AonComment({"How many seconds until a leaderboard times out when trying to load data.", "Default: 5"})
    private long leaderboardGetDataTimeoutSeconds = 5;

    @AonComment({"How many seconds between automatic leaderboard recalculations.", "Default: 120"})
    private long leaderboardRecalculateGapSeconds = 120;

    @AonComment({"Whether leaderboards recalculate using multiple threads.", "Default: true"})
    private boolean leaderboardMultithreadedRecalculate = true;

    @AonComment({"Default event priority to register occasion event handlers with if not specified.", "Options: HIGHEST, HIGH, NORMAL, LOW, LOWEST.", "Default: HIGHEST"})
    private UEvent.Priority occasionsDefaultEventHandlerPriority = UEvent.Priority.HIGHEST;

    @AonComment({"Whether Atlantis should send a public broadcast when Water joins.", "Default: true"})
    private boolean atlantisPublicMessage = true;

    @AonComment({"Whether to remove extra data after a schematic is prepared for placement.", "Default: true"})
    private boolean cleanSchematicsAfterPrepare = true;

    @AonComment({"Whether schematics placed with '/atlantis schematic' should ignore air blocks.", "Default: true"})
    private boolean schematicIgnoreAir = true;

    @AonComment({"Whether schematics placed with '/atlantis schematic' should use an async placement strategy.", "Default: false"})
    private boolean schematicAsync = false;

    @AonComment({"Number of blocks to place per schematic placement step when using '/atlantis schematic'.", "Default: 500"})
    private int schematicBlocksPerStep = 500;

    @AonComment({"Number of ticks per schematic placement step when using '/atlantis schematic'.", "Default: 1"})
    private int schematicTicksPerStep = 1;

    @AonComment({"Number of schematic placements to hold in history for '/atlantis undo'.", "Default: 10"})
    private int schematicHistoryDepth = 10;

    @AonComment({"Whether the mod ID of a data type is prefixed to database table names.", "Default: true"})
    private boolean modIdPrefixOnTableNames = true;

    @AonComment({"Database schema to use instead of each mod's mod ID.", "Default: N/A"})
    private String globalDatabaseSchemaOverride = "";

    @AonComment({"Whether warnings are sent to console if player login/logout callbacks are missing.", "Default: true"})
    private boolean warnForMissingCallbacks = true;

    @AonComment({"Whether Atlantis will create an example Atlantis level.", "Default: false"})
    private boolean createDefaultAtlantisLevel = false;

    @AonComment({"How many milliseconds to wait for Redis acknowledgements before proceeding with syncing.", "Default: 2000"})
    private long redisAckMilliseconds = 2000;

    @AonComment({"How many milliseconds to wait between each Redis sync message.", "Default: 250"})
    private long redisSyncMilliseconds = 250;

    @AonComment({"Whether to use SSL when establishing a Redis connection.", "Default: true"})
    private boolean redisSSL = true;

    @AonComment({"The number of connections to use in database connection pools by default.", "Default: 10"})
    private int defaultDatabaseConnections = 10;

    @AonComment({"Key/value pairs of HikariCP database properties."})
    private Map<String, String> hikariProperties = new HashMap<>();

    @AonComment({"Remapped database names to alter the default mod ID based database names."})
    private Map<String, String> remappedDatabaseNames = new HashMap<>();

    @AonComment({"Pinned decoration items in the page decorator.", "Can be modified in-game from within the decorator."})
    private List<ConfigurableItemStack> pinnedDecorations = Lists.newArrayList(
            ConfigurableItemStack.of("white_stained_glass_pane"),
            ConfigurableItemStack.of("orange_stained_glass_pane"),
            ConfigurableItemStack.of("magenta_stained_glass_pane"),
            ConfigurableItemStack.of("light_blue_stained_glass_pane"),
            ConfigurableItemStack.of("yellow_stained_glass_pane"),
            ConfigurableItemStack.of("lime_stained_glass_pane"),
            ConfigurableItemStack.of("pink_stained_glass_pane"),
            ConfigurableItemStack.of("gray_stained_glass_pane"),
            ConfigurableItemStack.of("light_gray_stained_glass_pane"),
            ConfigurableItemStack.of("cyan_stained_glass_pane"),
            ConfigurableItemStack.of("purple_stained_glass_pane"),
            ConfigurableItemStack.of("blue_stained_glass_pane"),
            ConfigurableItemStack.of("brown_stained_glass_pane"),
            ConfigurableItemStack.of("green_stained_glass_pane"),
            ConfigurableItemStack.of("red_stained_glass_pane"),
            ConfigurableItemStack.of("black_stained_glass_pane")
    );

    @AonComment({"Remappings for parsed item stacks."})
    private Map<String, String> remappedParsedItemStacks = new HashMap<>();

    private transient List<Item> parsedPinnedDecorations = null;
    
    /**
     * Creates the default settings. Fills the default HikariCP properties, an example database remap and the
     * remaps for Poké Ball item names.
     */
    public Settings() {
        this.hikariProperties.put("cachePrepStmts", "true");
        this.hikariProperties.put("prepStmtCacheSize", "250");
        this.hikariProperties.put("prepStmtCacheSqlLimit", "2048");
        this.hikariProperties.put("useServerPrepStmts", "true");
        this.hikariProperties.put("cacheCallableStmts", "true");
        this.hikariProperties.put("alwaysSendSetIsolation", "false");
        this.hikariProperties.put("cacheServerConfiguration", "true");
        this.hikariProperties.put("elideSetAutoCommits", "true");
        this.hikariProperties.put("useLocalSessionState", "true");
        this.hikariProperties.put("characterEncoding","UTF-8");
        this.hikariProperties.put("passwordCharacterEncoding", "UTF-8");
        this.hikariProperties.put("useUnicode","true");
        this.hikariProperties.put("maxLifetime", "1800000");
        this.hikariProperties.put("driverClassName", "com.mysql.cj.jdbc.Driver");

        this.remappedDatabaseNames.put("exampleold", "exampleremap");

        this.remappedParsedItemStacks.put("pixelmon:beast_ball", "#pokeball beast_ball #end");
        this.remappedParsedItemStacks.put("beast_ball", "#pokeball beast_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:cherish_ball", "#pokeball cherish_ball #end");
        this.remappedParsedItemStacks.put("cherish_ball", "#pokeball cherish_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:dive_ball", "#pokeball dive_ball #end");
        this.remappedParsedItemStacks.put("dive_ball", "#pokeball dive_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:dream_ball", "#pokeball dream_ball #end");
        this.remappedParsedItemStacks.put("dream_ball", "#pokeball dream_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:dusk_ball", "#pokeball dusk_ball #end");
        this.remappedParsedItemStacks.put("dusk_ball", "#pokeball dusk_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:fast_ball", "#pokeball fast_ball #end");
        this.remappedParsedItemStacks.put("fast_ball", "#pokeball fast_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:friend_ball", "#pokeball friend_ball #end");
        this.remappedParsedItemStacks.put("friend_ball", "#pokeball friend_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:great_ball", "#pokeball great_ball #end");
        this.remappedParsedItemStacks.put("great_ball", "#pokeball great_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:gs_ball", "#pokeball gs_ball #end");
        this.remappedParsedItemStacks.put("gs_ball", "#pokeball gs_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:heal_ball", "#pokeball heal_ball #end");
        this.remappedParsedItemStacks.put("heal_ball", "#pokeball heal_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:heavy_ball", "#pokeball heavy_ball #end");
        this.remappedParsedItemStacks.put("heavy_ball", "#pokeball heavy_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:level_ball", "#pokeball level_ball #end");
        this.remappedParsedItemStacks.put("level_ball", "#pokeball level_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:love_ball", "#pokeball love_ball #end");
        this.remappedParsedItemStacks.put("love_ball", "#pokeball love_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:lure_ball", "#pokeball lure_ball #end");
        this.remappedParsedItemStacks.put("lure_ball", "#pokeball lure_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:luxury_ball", "#pokeball luxury_ball #end");
        this.remappedParsedItemStacks.put("luxury_ball", "#pokeball luxury_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:master_ball", "#pokeball master_ball #end");
        this.remappedParsedItemStacks.put("master_ball", "#pokeball master_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:moon_ball", "#pokeball moon_ball #end");
        this.remappedParsedItemStacks.put("moon_ball", "#pokeball moon_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:nest_ball", "#pokeball nest_ball #end");
        this.remappedParsedItemStacks.put("nest_ball", "#pokeball nest_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:net_ball", "#pokeball net_ball #end");
        this.remappedParsedItemStacks.put("net_ball", "#pokeball net_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:park_ball", "#pokeball park_ball #end");
        this.remappedParsedItemStacks.put("park_ball", "#pokeball park_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:poke_ball", "#pokeball poke_ball #end");
        this.remappedParsedItemStacks.put("poke_ball", "#pokeball poke_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:premier_ball", "#pokeball premier_ball #end");
        this.remappedParsedItemStacks.put("premier_ball", "#pokeball premier_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:quick_ball", "#pokeball quick_ball #end");
        this.remappedParsedItemStacks.put("quick_ball", "#pokeball quick_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:repeat_ball", "#pokeball repeat_ball #end");
        this.remappedParsedItemStacks.put("repeat_ball", "#pokeball repeat_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:safari_ball", "#pokeball safari_ball #end");
        this.remappedParsedItemStacks.put("safari_ball", "#pokeball safari_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:sport_ball", "#pokeball sport_ball #end");
        this.remappedParsedItemStacks.put("sport_ball", "#pokeball sport_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:timer_ball", "#pokeball timer_ball #end");
        this.remappedParsedItemStacks.put("timer_ball", "#pokeball timer_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:ultra_ball", "#pokeball ultra_ball #end");
        this.remappedParsedItemStacks.put("ultra_ball", "#pokeball ultra_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:ancient_poke_ball", "#pokeball ancient_poke_ball #end");
        this.remappedParsedItemStacks.put("ancient_poke_ball", "#pokeball ancient_poke_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:ancient_great_ball", "#pokeball ancient_great_ball #end");
        this.remappedParsedItemStacks.put("ancient_great_ball", "#pokeball ancient_great_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:ancient_ultra_ball", "#pokeball ancient_ultra_ball #end");
        this.remappedParsedItemStacks.put("ancient_ultra_ball", "#pokeball ancient_ultra_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:ancient_heavy_ball", "#pokeball ancient_heavy_ball #end");
        this.remappedParsedItemStacks.put("ancient_heavy_ball", "#pokeball ancient_heavy_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:leaden_ball", "#pokeball leaden_ball #end");
        this.remappedParsedItemStacks.put("leaden_ball", "#pokeball leaden_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:gigaton_ball", "#pokeball gigaton_ball #end");
        this.remappedParsedItemStacks.put("gigaton_ball", "#pokeball gigaton_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:feather_ball", "#pokeball feather_ball #end");
        this.remappedParsedItemStacks.put("feather_ball", "#pokeball feather_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:wing_ball", "#pokeball wing_ball #end");
        this.remappedParsedItemStacks.put("wing_ball", "#pokeball wing_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:jet_ball", "#pokeball jet_ball #end");
        this.remappedParsedItemStacks.put("jet_ball", "#pokeball jet_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:origin_ball", "#pokeball origin_ball #end");
        this.remappedParsedItemStacks.put("origin_ball", "#pokeball origin_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:strange_ball", "#pokeball strange_ball #end");
        this.remappedParsedItemStacks.put("strange_ball", "#pokeball strange_ball #end");
        this.remappedParsedItemStacks.put("pixelmon:christmas_ball", "#pokeball christmas_ball #end");
        this.remappedParsedItemStacks.put("christmas_ball", "#pokeball christmas_ball #end");
    }

    /**
     * @return the configured thread count. A value of zero or less means that Atlantis chooses the count.
     */
    public int getThreads() {
        return this.threads;
    }

    /**
     * Sets the thread count.
     *
     * @param threads the thread count. A value of zero or less means that Atlantis chooses the count.
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * @return {@code true} if files save automatically and asynchronously
     */
    public boolean isUseAsyncAutosaver() {
        return this.useAsyncAutosaver;
    }

    /**
     * Sets if files save automatically and asynchronously.
     *
     * @param useAsyncAutosaver {@code true} to use the async autosaver
     */
    public void setUseAsyncAutosaver(boolean useAsyncAutosaver) {
        this.useAsyncAutosaver = useAsyncAutosaver;
    }

    /**
     * @return the mode that decides when cached player data is evicted
     */
    public EvictionMode getPlayerDataEvictionAlgorithm() {
        return this.playerDataEvictionAlgorithm;
    }

    /**
     * Sets the mode that decides when cached player data is evicted.
     *
     * @param playerDataEvictionAlgorithm the eviction mode
     */
    public void setPlayerDataEvictionAlgorithm(EvictionMode playerDataEvictionAlgorithm) {
        this.playerDataEvictionAlgorithm = playerDataEvictionAlgorithm;
    }

    /**
     * @return the time between async autosaves, in seconds
     */
    public long getAsyncSaveDelaySeconds() {
        return this.asyncSaveDelaySeconds;
    }

    /**
     * Sets the time between async autosaves.
     *
     * @param asyncSaveDelaySeconds the time in seconds
     */
    public void setAsyncSaveDelaySeconds(long asyncSaveDelaySeconds) {
        this.asyncSaveDelaySeconds = asyncSaveDelaySeconds;
    }

    /**
     * @return the delay in milliseconds
     */
    public long getDataSaveDelay() {
        return this.dataSaveDelay;
    }

    /**
     * Sets the delay between each file save.
     *
     * @param dataSaveDelay the delay in milliseconds
     */
    public void setDataSaveDelay(long dataSaveDelay) {
        this.dataSaveDelay = dataSaveDelay;
    }

    /**
     * @return the delay in milliseconds
     */
    public long getScheduledTaskDelay() {
        return this.scheduledTaskDelay;
    }

    /**
     * Sets the delay between each attempt to run config scheduled tasks.
     *
     * @param scheduledTaskDelay the delay in milliseconds
     */
    public void setScheduledTaskDelay(long scheduledTaskDelay) {
        this.scheduledTaskDelay = scheduledTaskDelay;
    }

    /**
     * @return the delay in milliseconds
     */
    public long getDoubleClickDelay() {
        return this.doubleClickDelay;
    }

    /**
     * Sets the maximum time between two clicks on a page that count as a double click.
     *
     * @param doubleClickDelay the delay in milliseconds
     */
    public void setDoubleClickDelay(long doubleClickDelay) {
        this.doubleClickDelay = doubleClickDelay;
    }

    /**
     * @return the delay in milliseconds
     */
    public long getDuplicateSaveDelay() {
        return this.duplicateSaveDelay;
    }

    /**
     * Sets the delay between a duplicate file save and its retry.
     *
     * @param duplicateSaveDelay the delay in milliseconds
     */
    public void setDuplicateSaveDelay(long duplicateSaveDelay) {
        this.duplicateSaveDelay = duplicateSaveDelay;
    }

    /**
     * @return {@code true} if errored files go to the error bin instead of being renamed and marked
     */
    public boolean isUseErrorBin() {
        return this.useErrorBin;
    }

    /**
     * Sets if errored files go to the error bin instead of being renamed and marked.
     *
     * @param useErrorBin {@code true} to use the error bin
     */
    public void setUseErrorBin(boolean useErrorBin) {
        this.useErrorBin = useErrorBin;
    }

    /**
     * @return the time until a cached player skin is fetched from Mojang again, in seconds
     */
    public long getSkinRefetchGapSeconds() {
        return this.skinRefetchGapSeconds;
    }

    /**
     * Sets the time until a cached player skin is fetched from Mojang again.
     *
     * @param skinRefetchGapSeconds the time in seconds
     */
    public void setSkinRefetchGapSeconds(long skinRefetchGapSeconds) {
        this.skinRefetchGapSeconds = skinRefetchGapSeconds;
    }

    /**
     * @return the time until a leaderboard stops waiting for data, in seconds
     */
    public long getLeaderboardGetDataTimeoutSeconds() {
        return this.leaderboardGetDataTimeoutSeconds;
    }

    /**
     * Sets the time until a leaderboard stops waiting for data.
     *
     * @param leaderboardGetDataTimeoutSeconds the time in seconds
     */
    public void setLeaderboardGetDataTimeoutSeconds(long leaderboardGetDataTimeoutSeconds) {
        this.leaderboardGetDataTimeoutSeconds = leaderboardGetDataTimeoutSeconds;
    }

    /**
     * @return the time between automatic leaderboard recalculations, in seconds
     */
    public long getLeaderboardRecalculateGapSeconds() {
        return this.leaderboardRecalculateGapSeconds;
    }

    /**
     * Sets the time between automatic leaderboard recalculations.
     *
     * @param leaderboardRecalculateGapSeconds the time in seconds
     */
    public void setLeaderboardRecalculateGapSeconds(long leaderboardRecalculateGapSeconds) {
        this.leaderboardRecalculateGapSeconds = leaderboardRecalculateGapSeconds;
    }

    /**
     * @return {@code true} if leaderboards recalculate on multiple threads
     */
    public boolean isLeaderboardMultithreadedRecalculate() {
        return this.leaderboardMultithreadedRecalculate;
    }

    /**
     * Sets if leaderboards recalculate on multiple threads.
     *
     * @param leaderboardMultithreadedRecalculate {@code true} to use multiple threads
     */
    public void setLeaderboardMultithreadedRecalculate(boolean leaderboardMultithreadedRecalculate) {
        this.leaderboardMultithreadedRecalculate = leaderboardMultithreadedRecalculate;
    }

    /**
     * @return the priority for occasion event handlers that do not specify one
     */
    public UEvent.Priority getOccasionsDefaultEventHandlerPriority() {
        return this.occasionsDefaultEventHandlerPriority;
    }

    /**
     * Sets the priority for occasion event handlers that do not specify one.
     *
     * @param occasionsDefaultEventHandlerPriority the default priority
     */
    public void setOccasionsDefaultEventHandlerPriority(UEvent.Priority occasionsDefaultEventHandlerPriority) {
        this.occasionsDefaultEventHandlerPriority = occasionsDefaultEventHandlerPriority;
    }

    /**
     * @return the level at which load messages are logged
     */
    public AtlantisLogger.Severity getLogLoadMessages() {
        return this.logLoadMessages;
    }

    /**
     * Sets the level at which load messages are logged.
     *
     * @param logLoadMessages the level, or {@link AtlantisLogger.Severity#NONE} to log nothing
     */
    public void setLogLoadMessages(AtlantisLogger.Severity logLoadMessages) {
        this.logLoadMessages = logLoadMessages;
    }

    /**
     * @return the level at which intercepted packets are logged
     */
    public AtlantisLogger.Severity getLogPackets() {
        return this.logPackets;
    }

    /**
     * Sets the level at which intercepted packets are logged.
     *
     * @param logPackets the level, or {@link AtlantisLogger.Severity#NONE} to log nothing
     */
    public void setLogPackets(AtlantisLogger.Severity logPackets) {
        this.logPackets = logPackets;
    }

    /**
     * @return {@code true} if a registered placeholder API processes the text in pages
     */
    public boolean isUsePlaceholdersInPages() {
        return this.usePlaceholdersInPages;
    }

    /**
     * Sets if a registered placeholder API processes the text in pages.
     *
     * @param usePlaceholdersInPages {@code true} to use placeholders in pages
     */
    public void setUsePlaceholdersInPages(boolean usePlaceholdersInPages) {
        this.usePlaceholdersInPages = usePlaceholdersInPages;
    }

    /**
     * Sets if Atlantis sends a public broadcast when Water joins.
     *
     * @param atlantisPublicMessage {@code true} to send the broadcast
     */
    public void setAtlantisPublicMessage(boolean atlantisPublicMessage) {
        this.atlantisPublicMessage = atlantisPublicMessage;
    }

    /**
     * @return {@code true} if Atlantis sends a public broadcast when Water joins
     */
    public boolean isAtlantisPublicMessage() {
        return this.atlantisPublicMessage;
    }

    /**
     * Sets if extra data is removed after a schematic is prepared for placement.
     *
     * @param cleanSchematicsAfterPrepare {@code true} to remove the extra data
     */
    public void setCleanSchematicsAfterPrepare(boolean cleanSchematicsAfterPrepare) {
        this.cleanSchematicsAfterPrepare = cleanSchematicsAfterPrepare;
    }

    /**
     * @return {@code true} if extra data is removed after a schematic is prepared for placement
     */
    public boolean isCleanSchematicsAfterPrepare() {
        return this.cleanSchematicsAfterPrepare;
    }

    /**
     * Sets if schematics placed with {@code /atlantis schematic} ignore air blocks.
     *
     * @param schematicIgnoreAir {@code true} to ignore air blocks
     */
    public void setSchematicIgnoreAir(boolean schematicIgnoreAir) {
        this.schematicIgnoreAir = schematicIgnoreAir;
    }

    /**
     * @return {@code true} if schematics placed with {@code /atlantis schematic} ignore air blocks
     */
    public boolean isSchematicIgnoreAir() {
        return this.schematicIgnoreAir;
    }

    /**
     * Sets if schematics placed with {@code /atlantis schematic} use async placement.
     *
     * @param schematicAsync {@code true} to use async placement
     */
    public void setSchematicAsync(boolean schematicAsync) {
        this.schematicAsync = schematicAsync;
    }

    /**
     * @return {@code true} if schematics placed with {@code /atlantis schematic} use async placement
     */
    public boolean isSchematicAsync() {
        return this.schematicAsync;
    }

    /**
     * Sets the number of blocks to place in each schematic placement step.
     *
     * @param schematicBlocksPerStep the number of blocks
     */
    public void setSchematicBlocksPerStep(int schematicBlocksPerStep) {
        this.schematicBlocksPerStep = schematicBlocksPerStep;
    }

    /**
     * @return the number of blocks to place in each schematic placement step
     */
    public int getSchematicBlocksPerStep() {
        return this.schematicBlocksPerStep;
    }

    /**
     * Sets the number of ticks in each schematic placement step.
     *
     * @param schematicTicksPerStep the number of ticks
     */
    public void setSchematicTicksPerStep(int schematicTicksPerStep) {
        this.schematicTicksPerStep = schematicTicksPerStep;
    }

    /**
     * @return the number of ticks in each schematic placement step
     */
    public int getSchematicTicksPerStep() {
        return this.schematicTicksPerStep;
    }

    /**
     * Sets the number of schematic placements that {@code /atlantis undo} keeps in history.
     *
     * @param schematicHistoryDepth the number of placements
     */
    public void setSchematicHistoryDepth(int schematicHistoryDepth) {
        this.schematicHistoryDepth = schematicHistoryDepth;
    }

    /**
     * @return the number of schematic placements that {@code /atlantis undo} keeps in history
     */
    public int getSchematicHistoryDepth() {
        return this.schematicHistoryDepth;
    }

    /**
     * Sets if Atlantis creates an example Atlantis level.
     *
     * @param createDefaultAtlantisLevel {@code true} to create the example level
     */
    public void setCreateDefaultAtlantisLevel(boolean createDefaultAtlantisLevel) {
        this.createDefaultAtlantisLevel = createDefaultAtlantisLevel;
    }

    /**
     * @return {@code true} if Atlantis creates an example Atlantis level
     */
    public boolean isCreateDefaultAtlantisLevel() {
        return this.createDefaultAtlantisLevel;
    }

    /**
     * Sets if the mod ID of a data type is put before its database table names.
     *
     * @param modIdPrefixOnTableNames {@code true} to add the prefix
     */
    public void setModIdPrefixOnTableNames(boolean modIdPrefixOnTableNames) {
        this.modIdPrefixOnTableNames = modIdPrefixOnTableNames;
    }

    /**
     * @return {@code true} if the mod ID of a data type is put before its database table names
     */
    public boolean isModIdPrefixOnTableNames() {
        return this.modIdPrefixOnTableNames;
    }

    /**
     * Sets the database schema that every mod uses instead of its mod ID.
     *
     * @param globalDatabaseSchemaOverride the schema name, or an empty string for no override
     */
    public void setGlobalDatabaseSchemaOverride(String globalDatabaseSchemaOverride) {
        this.globalDatabaseSchemaOverride = globalDatabaseSchemaOverride;
    }

    /**
     * @return the database schema that every mod uses instead of its mod ID, or an empty string for no override
     */
    public String getGlobalDatabaseSchemaOverride() {
        return this.globalDatabaseSchemaOverride;
    }

    /**
     * Sets if missing player login and logout callbacks log a warning.
     *
     * @param warnForMissingCallbacks {@code true} to log a warning
     */
    public void setWarnForMissingCallbacks(boolean warnForMissingCallbacks) {
        this.warnForMissingCallbacks = warnForMissingCallbacks;
    }

    /**
     * @return {@code true} if missing player login and logout callbacks log a warning
     */
    public boolean isWarnForMissingCallbacks() {
        return this.warnForMissingCallbacks;
    }

    /**
     * @return {@link AtlantisLogger.Severity#WARN} if {@link #isWarnForMissingCallbacks()} is {@code true},
     *     otherwise {@link AtlantisLogger.Severity#DEBUG}
     */
    public AtlantisLogger.Severity getMissingCallbacksSeverity() {
        return this.warnForMissingCallbacks ? AtlantisLogger.Severity.WARN : AtlantisLogger.Severity.DEBUG;
    }

    /**
     * @return the time to wait for Redis acknowledgements before syncing continues, in milliseconds
     */
    public long getRedisAckMilliseconds() {
        return this.redisAckMilliseconds;
    }

    /**
     * Sets the time to wait for Redis acknowledgements before syncing continues.
     *
     * @param redisAckMilliseconds the time in milliseconds
     */
    public void setRedisAckMilliseconds(long redisAckMilliseconds) {
        this.redisAckMilliseconds = redisAckMilliseconds;
    }

    /**
     * @return the time between Redis sync messages, in milliseconds
     */
    public long getRedisSyncMilliseconds() {
        return this.redisSyncMilliseconds;
    }

    /**
     * Sets the time between Redis sync messages.
     *
     * @param redisSyncMilliseconds the time in milliseconds
     */
    public void setRedisSyncMilliseconds(long redisSyncMilliseconds) {
        this.redisSyncMilliseconds = redisSyncMilliseconds;
    }

    /**
     * @return {@code true} if Redis connections use SSL
     */
    public boolean isRedisSSL() {
        return this.redisSSL;
    }

    /**
     * Sets if Redis connections use SSL.
     *
     * @param redisSSL {@code true} to use SSL
     */
    public void setRedisSSL(boolean redisSSL) {
        this.redisSSL = redisSSL;
    }

    /**
     * @return the default number of connections in a database connection pool
     */
    public int getDefaultDatabaseConnections() {
        return this.defaultDatabaseConnections;
    }

    /**
     * Sets the default number of connections in a database connection pool.
     *
     * @param defaultDatabaseConnections the number of connections
     */
    public void setDefaultDatabaseConnections(int defaultDatabaseConnections) {
        this.defaultDatabaseConnections = defaultDatabaseConnections;
    }

    /**
     * Sets the HikariCP database properties.
     *
     * @param hikariProperties the property names mapped to their values
     */
    public void setHikariProperties(Map<String, String> hikariProperties) {
        this.hikariProperties = hikariProperties;
    }

    /**
     * @return the HikariCP property names mapped to their values
     */
    public Map<String, String> getHikariProperties() {
        return this.hikariProperties;
    }

    /**
     * Sets the remaps for default database names.
     *
     * @param remappedDatabaseNames the default names mapped to the names to use
     */
    public void setRemappedDatabaseNames(Map<String, String> remappedDatabaseNames) {
        this.remappedDatabaseNames = remappedDatabaseNames;
    }

    /**
     * @return the default database names mapped to the names to use
     */
    public Map<String, String> getRemappedDatabaseNames() {
        return this.remappedDatabaseNames;
    }

    /**
     * @param value the default database name, normally a mod ID
     * @return the global schema override if one is set, otherwise the remapped name or the value itself
     */
    public String getDatabaseName(String value) {
        return this.globalDatabaseSchemaOverride.isEmpty() ? this.remappedDatabaseNames.getOrDefault(value, value) : this.globalDatabaseSchemaOverride;
    }

    /**
     * Sets the remaps for parsed item stacks.
     *
     * @param remappedParsedItemStacks the item strings mapped to their replacements
     */
    public void setRemappedParsedItemStacks(Map<String, String> remappedParsedItemStacks) {
        this.remappedParsedItemStacks = remappedParsedItemStacks;
    }

    /**
     * @return the item strings mapped to their replacements
     */
    public Map<String, String> getRemappedParsedItemStacks() {
        return this.remappedParsedItemStacks;
    }

    /**
     * @return the remapped item string, or the item itself if it has no remapping
     */
    public String getParsedItemStack(String item) {
        return this.remappedParsedItemStacks.getOrDefault(item, item);
    }

    /**
     * Sets the pinned decoration items of the page decorator.
     *
     * @param pinnedDecorations the pinned items
     */
    public void setPinnedDecorations(List<ConfigurableItemStack> pinnedDecorations) {
        this.pinnedDecorations = pinnedDecorations;
    }

    /**
     * @return the pinned decoration items of the page decorator
     */
    public List<ConfigurableItemStack> getPinnedDecorations() {
        return this.pinnedDecorations;
    }

    /**
     * Parses the pinned decorations once and caches the result until the next {@link #save()}.
     */
    public List<Item> getParsedPinnedDecorations() {
        if (this.parsedPinnedDecorations == null) {
            this.parsedPinnedDecorations = new ArrayList<>();
            for (var rl : this.getPinnedDecorations()) {
                this.parsedPinnedDecorations.add(rl.item());
            }
            this.parsedPinnedDecorations.remove(null);
        }
        return this.parsedPinnedDecorations;
    }

    /**
     * Clears the cached result of {@link #getParsedPinnedDecorations()}, then saves the settings.
     */
    @Override
    public void save() {
        this.parsedPinnedDecorations = null;
        Configuration.super.save();
    }

    /**
     * @return the loaded settings, or the defaults if the config is not loaded yet
     */
    public static Settings get() {
        return Atlantis.CONFIG.orElse(Settings.class, Settings.DEFAULTS);
    }

    /**
     * Decides when cached player data is evicted as players join and leave.
     */
    public enum EvictionMode {

        NEVER,
        DATABASE,
        ALWAYS,
        ;

        /**
         * @return {@code true} if the config's cached player data is evicted when players join and leave
         */
        public boolean passes(Config<?> config) {
            return switch (this) {
                case ALWAYS -> true;
                case DATABASE -> config.storage() instanceof Database;
                default -> false;
            };
        }
    }
}
