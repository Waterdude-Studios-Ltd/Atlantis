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
    private boolean modIDPrefixOnTableNames = true;

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

        this.remappedParsedItemStacks.put("pixelmon:beast_ball", "pixelmon:poke_ball {PokeBallID:beast_ball}");
        this.remappedParsedItemStacks.put("beast_ball", "pixelmon:poke_ball {PokeBallID:beast_ball}");
        this.remappedParsedItemStacks.put("pixelmon:cherish_ball", "pixelmon:poke_ball {PokeBallID:cherish_ball}");
        this.remappedParsedItemStacks.put("cherish_ball", "pixelmon:poke_ball {PokeBallID:cherish_ball}");
        this.remappedParsedItemStacks.put("pixelmon:dive_ball", "pixelmon:poke_ball {PokeBallID:dive_ball}");
        this.remappedParsedItemStacks.put("dive_ball", "pixelmon:poke_ball {PokeBallID:dive_ball}");
        this.remappedParsedItemStacks.put("pixelmon:dream_ball", "pixelmon:poke_ball {PokeBallID:dream_ball}");
        this.remappedParsedItemStacks.put("dream_ball", "pixelmon:poke_ball {PokeBallID:dream_ball}");
        this.remappedParsedItemStacks.put("pixelmon:dusk_ball", "pixelmon:poke_ball {PokeBallID:dusk_ball}");
        this.remappedParsedItemStacks.put("dusk_ball", "pixelmon:poke_ball {PokeBallID:dusk_ball}");
        this.remappedParsedItemStacks.put("pixelmon:fast_ball", "pixelmon:poke_ball {PokeBallID:fast_ball}");
        this.remappedParsedItemStacks.put("fast_ball", "pixelmon:poke_ball {PokeBallID:fast_ball}");
        this.remappedParsedItemStacks.put("pixelmon:friend_ball", "pixelmon:poke_ball {PokeBallID:friend_ball}");
        this.remappedParsedItemStacks.put("friend_ball", "pixelmon:poke_ball {PokeBallID:friend_ball}");
        this.remappedParsedItemStacks.put("pixelmon:great_ball", "pixelmon:poke_ball {PokeBallID:great_ball}");
        this.remappedParsedItemStacks.put("great_ball", "pixelmon:poke_ball {PokeBallID:great_ball}");
        this.remappedParsedItemStacks.put("pixelmon:gs_ball", "pixelmon:poke_ball {PokeBallID:gs_ball}");
        this.remappedParsedItemStacks.put("gs_ball", "pixelmon:poke_ball {PokeBallID:gs_ball}");
        this.remappedParsedItemStacks.put("pixelmon:heal_ball", "pixelmon:poke_ball {PokeBallID:heal_ball}");
        this.remappedParsedItemStacks.put("heal_ball", "pixelmon:poke_ball {PokeBallID:heal_ball}");
        this.remappedParsedItemStacks.put("pixelmon:heavy_ball", "pixelmon:poke_ball {PokeBallID:heavy_ball}");
        this.remappedParsedItemStacks.put("heavy_ball", "pixelmon:poke_ball {PokeBallID:heavy_ball}");
        this.remappedParsedItemStacks.put("pixelmon:level_ball", "pixelmon:poke_ball {PokeBallID:level_ball}");
        this.remappedParsedItemStacks.put("level_ball", "pixelmon:poke_ball {PokeBallID:level_ball}");
        this.remappedParsedItemStacks.put("pixelmon:love_ball", "pixelmon:poke_ball {PokeBallID:love_ball}");
        this.remappedParsedItemStacks.put("love_ball", "pixelmon:poke_ball {PokeBallID:love_ball}");
        this.remappedParsedItemStacks.put("pixelmon:lure_ball", "pixelmon:poke_ball {PokeBallID:lure_ball}");
        this.remappedParsedItemStacks.put("lure_ball", "pixelmon:poke_ball {PokeBallID:lure_ball}");
        this.remappedParsedItemStacks.put("pixelmon:luxury_ball", "pixelmon:poke_ball {PokeBallID:luxury_ball}");
        this.remappedParsedItemStacks.put("luxury_ball", "pixelmon:poke_ball {PokeBallID:luxury_ball}");
        this.remappedParsedItemStacks.put("pixelmon:master_ball", "pixelmon:poke_ball {PokeBallID:master_ball}");
        this.remappedParsedItemStacks.put("master_ball", "pixelmon:poke_ball {PokeBallID:master_ball}");
        this.remappedParsedItemStacks.put("pixelmon:moon_ball", "pixelmon:poke_ball {PokeBallID:moon_ball}");
        this.remappedParsedItemStacks.put("moon_ball", "pixelmon:poke_ball {PokeBallID:moon_ball}");
        this.remappedParsedItemStacks.put("pixelmon:nest_ball", "pixelmon:poke_ball {PokeBallID:nest_ball}");
        this.remappedParsedItemStacks.put("nest_ball", "pixelmon:poke_ball {PokeBallID:nest_ball}");
        this.remappedParsedItemStacks.put("pixelmon:net_ball", "pixelmon:poke_ball {PokeBallID:net_ball}");
        this.remappedParsedItemStacks.put("net_ball", "pixelmon:poke_ball {PokeBallID:net_ball}");
        this.remappedParsedItemStacks.put("pixelmon:park_ball", "pixelmon:poke_ball {PokeBallID:park_ball}");
        this.remappedParsedItemStacks.put("park_ball", "pixelmon:poke_ball {PokeBallID:park_ball}");
        this.remappedParsedItemStacks.put("pixelmon:poke_ball", "pixelmon:poke_ball {PokeBallID:poke_ball}");
        this.remappedParsedItemStacks.put("poke_ball", "pixelmon:poke_ball {PokeBallID:poke_ball}");
        this.remappedParsedItemStacks.put("pixelmon:premier_ball", "pixelmon:poke_ball {PokeBallID:premier_ball}");
        this.remappedParsedItemStacks.put("premier_ball", "pixelmon:poke_ball {PokeBallID:premier_ball}");
        this.remappedParsedItemStacks.put("pixelmon:quick_ball", "pixelmon:poke_ball {PokeBallID:quick_ball}");
        this.remappedParsedItemStacks.put("quick_ball", "pixelmon:poke_ball {PokeBallID:quick_ball}");
        this.remappedParsedItemStacks.put("pixelmon:repeat_ball", "pixelmon:poke_ball {PokeBallID:repeat_ball}");
        this.remappedParsedItemStacks.put("repeat_ball", "pixelmon:poke_ball {PokeBallID:repeat_ball}");
        this.remappedParsedItemStacks.put("pixelmon:safari_ball", "pixelmon:poke_ball {PokeBallID:safari_ball}");
        this.remappedParsedItemStacks.put("safari_ball", "pixelmon:poke_ball {PokeBallID:safari_ball}");
        this.remappedParsedItemStacks.put("pixelmon:sport_ball", "pixelmon:poke_ball {PokeBallID:sport_ball}");
        this.remappedParsedItemStacks.put("sport_ball", "pixelmon:poke_ball {PokeBallID:sport_ball}");
        this.remappedParsedItemStacks.put("pixelmon:timer_ball", "pixelmon:poke_ball {PokeBallID:timer_ball}");
        this.remappedParsedItemStacks.put("timer_ball", "pixelmon:poke_ball {PokeBallID:timer_ball}");
        this.remappedParsedItemStacks.put("pixelmon:ultra_ball", "pixelmon:poke_ball {PokeBallID:ultra_ball}");
        this.remappedParsedItemStacks.put("ultra_ball", "pixelmon:poke_ball {PokeBallID:ultra_ball}");
        this.remappedParsedItemStacks.put("pixelmon:ancient_poke_ball", "pixelmon:poke_ball {PokeBallID:ancient_poke_ball}");
        this.remappedParsedItemStacks.put("ancient_poke_ball", "pixelmon:poke_ball {PokeBallID:ancient_poke_ball}");
        this.remappedParsedItemStacks.put("pixelmon:ancient_great_ball", "pixelmon:poke_ball {PokeBallID:ancient_great_ball}");
        this.remappedParsedItemStacks.put("ancient_great_ball", "pixelmon:poke_ball {PokeBallID:ancient_great_ball}");
        this.remappedParsedItemStacks.put("pixelmon:ancient_ultra_ball", "pixelmon:poke_ball {PokeBallID:ancient_ultra_ball}");
        this.remappedParsedItemStacks.put("ancient_ultra_ball", "pixelmon:poke_ball {PokeBallID:ancient_ultra_ball}");
        this.remappedParsedItemStacks.put("pixelmon:ancient_heavy_ball", "pixelmon:poke_ball {PokeBallID:ancient_heavy_ball}");
        this.remappedParsedItemStacks.put("ancient_heavy_ball", "pixelmon:poke_ball {PokeBallID:ancient_heavy_ball}");
        this.remappedParsedItemStacks.put("pixelmon:leaden_ball", "pixelmon:poke_ball {PokeBallID:leaden_ball}");
        this.remappedParsedItemStacks.put("leaden_ball", "pixelmon:poke_ball {PokeBallID:leaden_ball}");
        this.remappedParsedItemStacks.put("pixelmon:gigaton_ball", "pixelmon:poke_ball {PokeBallID:gigaton_ball}");
        this.remappedParsedItemStacks.put("gigaton_ball", "pixelmon:poke_ball {PokeBallID:gigaton_ball}");
        this.remappedParsedItemStacks.put("pixelmon:feather_ball", "pixelmon:poke_ball {PokeBallID:feather_ball}");
        this.remappedParsedItemStacks.put("feather_ball", "pixelmon:poke_ball {PokeBallID:feather_ball}");
        this.remappedParsedItemStacks.put("pixelmon:wing_ball", "pixelmon:poke_ball {PokeBallID:wing_ball}");
        this.remappedParsedItemStacks.put("wing_ball", "pixelmon:poke_ball {PokeBallID:wing_ball}");
        this.remappedParsedItemStacks.put("pixelmon:jet_ball", "pixelmon:poke_ball {PokeBallID:jet_ball}");
        this.remappedParsedItemStacks.put("jet_ball", "pixelmon:poke_ball {PokeBallID:jet_ball}");
        this.remappedParsedItemStacks.put("pixelmon:origin_ball", "pixelmon:poke_ball {PokeBallID:origin_ball}");
        this.remappedParsedItemStacks.put("origin_ball", "pixelmon:poke_ball {PokeBallID:origin_ball}");
        this.remappedParsedItemStacks.put("pixelmon:strange_ball", "pixelmon:poke_ball {PokeBallID:strange_ball}");
        this.remappedParsedItemStacks.put("strange_ball", "pixelmon:poke_ball {PokeBallID:strange_ball}");
        this.remappedParsedItemStacks.put("pixelmon:christmas_ball", "pixelmon:poke_ball {PokeBallID:christmas_ball}");
        this.remappedParsedItemStacks.put("christmas_ball", "pixelmon:poke_ball {PokeBallID:christmas_ball}");
    }

    public int getThreads() {
        return this.threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isUseAsyncAutosaver() {
        return this.useAsyncAutosaver;
    }

    public void setUseAsyncAutosaver(boolean useAsyncAutosaver) {
        this.useAsyncAutosaver = useAsyncAutosaver;
    }

    public EvictionMode getPlayerDataEvictionAlgorithm() {
        return this.playerDataEvictionAlgorithm;
    }

    public void setPlayerDataEvictionAlgorithm(EvictionMode playerDataEvictionAlgorithm) {
        this.playerDataEvictionAlgorithm = playerDataEvictionAlgorithm;
    }

    public long getAsyncSaveDelaySeconds() {
        return this.asyncSaveDelaySeconds;
    }

    public void setAsyncSaveDelaySeconds(long asyncSaveDelaySeconds) {
        this.asyncSaveDelaySeconds = asyncSaveDelaySeconds;
    }

    public long getDataSaveDelay() {
        return this.dataSaveDelay;
    }

    public void setDataSaveDelay(long dataSaveDelay) {
        this.dataSaveDelay = dataSaveDelay;
    }

    public long getScheduledTaskDelay() {
        return this.scheduledTaskDelay;
    }

    public void setScheduledTaskDelay(long scheduledTaskDelay) {
        this.scheduledTaskDelay = scheduledTaskDelay;
    }

    public long getDoubleClickDelay() {
        return this.doubleClickDelay;
    }

    public void setDoubleClickDelay(long doubleClickDelay) {
        this.doubleClickDelay = doubleClickDelay;
    }

    public long getDuplicateSaveDelay() {
        return this.duplicateSaveDelay;
    }

    public void setDuplicateSaveDelay(long duplicateSaveDelay) {
        this.duplicateSaveDelay = duplicateSaveDelay;
    }

    public boolean isUseErrorBin() {
        return this.useErrorBin;
    }

    public void setUseErrorBin(boolean useErrorBin) {
        this.useErrorBin = useErrorBin;
    }

    public long getSkinRefetchGapSeconds() {
        return this.skinRefetchGapSeconds;
    }

    public void setSkinRefetchGapSeconds(long skinRefetchGapSeconds) {
        this.skinRefetchGapSeconds = skinRefetchGapSeconds;
    }

    public long getLeaderboardGetDataTimeoutSeconds() {
        return this.leaderboardGetDataTimeoutSeconds;
    }

    public void setLeaderboardGetDataTimeoutSeconds(long leaderboardGetDataTimeoutSeconds) {
        this.leaderboardGetDataTimeoutSeconds = leaderboardGetDataTimeoutSeconds;
    }

    public long getLeaderboardRecalculateGapSeconds() {
        return this.leaderboardRecalculateGapSeconds;
    }

    public void setLeaderboardRecalculateGapSeconds(long leaderboardRecalculateGapSeconds) {
        this.leaderboardRecalculateGapSeconds = leaderboardRecalculateGapSeconds;
    }

    public boolean isLeaderboardMultithreadedRecalculate() {
        return this.leaderboardMultithreadedRecalculate;
    }

    public void setLeaderboardMultithreadedRecalculate(boolean leaderboardMultithreadedRecalculate) {
        this.leaderboardMultithreadedRecalculate = leaderboardMultithreadedRecalculate;
    }

    public UEvent.Priority getOccasionsDefaultEventHandlerPriority() {
        return this.occasionsDefaultEventHandlerPriority;
    }

    public void setOccasionsDefaultEventHandlerPriority(UEvent.Priority occasionsDefaultEventHandlerPriority) {
        this.occasionsDefaultEventHandlerPriority = occasionsDefaultEventHandlerPriority;
    }

    public AtlantisLogger.Severity getLogLoadMessages() {
        return this.logLoadMessages;
    }

    public void setLogLoadMessages(AtlantisLogger.Severity logLoadMessages) {
        this.logLoadMessages = logLoadMessages;
    }

    public AtlantisLogger.Severity getLogPackets() {
        return this.logPackets;
    }

    public void setLogPackets(AtlantisLogger.Severity logPackets) {
        this.logPackets = logPackets;
    }

    public boolean isUsePlaceholdersInPages() {
        return this.usePlaceholdersInPages;
    }

    public void setUsePlaceholdersInPages(boolean usePlaceholdersInPages) {
        this.usePlaceholdersInPages = usePlaceholdersInPages;
    }

    public void setAtlantisPublicMessage(boolean atlantisPublicMessage) {
        this.atlantisPublicMessage = atlantisPublicMessage;
    }

    public boolean isAtlantisPublicMessage() {
        return this.atlantisPublicMessage;
    }

    public void setCleanSchematicsAfterPrepare(boolean cleanSchematicsAfterPrepare) {
        this.cleanSchematicsAfterPrepare = cleanSchematicsAfterPrepare;
    }

    public boolean isCleanSchematicsAfterPrepare() {
        return this.cleanSchematicsAfterPrepare;
    }

    public void setSchematicIgnoreAir(boolean schematicIgnoreAir) {
        this.schematicIgnoreAir = schematicIgnoreAir;
    }

    public boolean isSchematicIgnoreAir() {
        return this.schematicIgnoreAir;
    }

    public void setSchematicAsync(boolean schematicAsync) {
        this.schematicAsync = schematicAsync;
    }

    public boolean isSchematicAsync() {
        return this.schematicAsync;
    }

    public void setSchematicBlocksPerStep(int schematicBlocksPerStep) {
        this.schematicBlocksPerStep = schematicBlocksPerStep;
    }

    public int getSchematicBlocksPerStep() {
        return this.schematicBlocksPerStep;
    }

    public void setSchematicTicksPerStep(int schematicTicksPerStep) {
        this.schematicTicksPerStep = schematicTicksPerStep;
    }

    public int getSchematicTicksPerStep() {
        return this.schematicTicksPerStep;
    }

    public void setSchematicHistoryDepth(int schematicHistoryDepth) {
        this.schematicHistoryDepth = schematicHistoryDepth;
    }

    public int getSchematicHistoryDepth() {
        return this.schematicHistoryDepth;
    }

    public void setCreateDefaultAtlantisLevel(boolean createDefaultAtlantisLevel) {
        this.createDefaultAtlantisLevel = createDefaultAtlantisLevel;
    }

    public boolean isCreateDefaultAtlantisLevel() {
        return this.createDefaultAtlantisLevel;
    }

    public void setModIDPrefixOnTableNames(boolean modIDPrefixOnTableNames) {
        this.modIDPrefixOnTableNames = modIDPrefixOnTableNames;
    }

    public boolean isModIDPrefixOnTableNames() {
        return this.modIDPrefixOnTableNames;
    }

    public void setGlobalDatabaseSchemaOverride(String globalDatabaseSchemaOverride) {
        this.globalDatabaseSchemaOverride = globalDatabaseSchemaOverride;
    }

    public String getGlobalDatabaseSchemaOverride() {
        return this.globalDatabaseSchemaOverride;
    }

    public void setWarnForMissingCallbacks(boolean warnForMissingCallbacks) {
        this.warnForMissingCallbacks = warnForMissingCallbacks;
    }

    public boolean isWarnForMissingCallbacks() {
        return this.warnForMissingCallbacks;
    }

    public AtlantisLogger.Severity getMissingCallbacksSeverity() {
        return this.warnForMissingCallbacks ? AtlantisLogger.Severity.WARN : AtlantisLogger.Severity.DEBUG;
    }

    public long getRedisAckMilliseconds() {
        return this.redisAckMilliseconds;
    }

    public void setRedisAckMilliseconds(long redisAckMilliseconds) {
        this.redisAckMilliseconds = redisAckMilliseconds;
    }

    public long getRedisSyncMilliseconds() {
        return this.redisSyncMilliseconds;
    }

    public void setRedisSyncMilliseconds(long redisSyncMilliseconds) {
        this.redisSyncMilliseconds = redisSyncMilliseconds;
    }

    public boolean isRedisSSL() {
        return this.redisSSL;
    }

    public void setRedisSSL(boolean redisSSL) {
        this.redisSSL = redisSSL;
    }

    public int getDefaultDatabaseConnections() {
        return this.defaultDatabaseConnections;
    }

    public void setDefaultDatabaseConnections(int defaultDatabaseConnections) {
        this.defaultDatabaseConnections = defaultDatabaseConnections;
    }

    public void setHikariProperties(Map<String, String> hikariProperties) {
        this.hikariProperties = hikariProperties;
    }

    public Map<String, String> getHikariProperties() {
        return this.hikariProperties;
    }

    public void setRemappedDatabaseNames(Map<String, String> remappedDatabaseNames) {
        this.remappedDatabaseNames = remappedDatabaseNames;
    }

    public Map<String, String> getRemappedDatabaseNames() {
        return this.remappedDatabaseNames;
    }

    public String getDatabaseName(String value) {
        return this.globalDatabaseSchemaOverride.isEmpty() ? this.remappedDatabaseNames.getOrDefault(value, value) : this.globalDatabaseSchemaOverride;
    }

    public void setRemappedParsedItemStacks(Map<String, String> remappedParsedItemStacks) {
        this.remappedParsedItemStacks = remappedParsedItemStacks;
    }

    public Map<String, String> getRemappedParsedItemStacks() {
        return this.remappedParsedItemStacks;
    }

    public String getParsedItemStack(String item) {
        return this.remappedParsedItemStacks.getOrDefault(item, item);
    }

    public void setPinnedDecorations(List<ConfigurableItemStack> pinnedDecorations) {
        this.pinnedDecorations = pinnedDecorations;
    }

    public List<ConfigurableItemStack> getPinnedDecorations() {
        return this.pinnedDecorations;
    }

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

    @Override
    public void save() {
        this.parsedPinnedDecorations = null;
        Configuration.super.save();
    }

    public static Settings get() {
        return Atlantis.CONFIG.orElse(Settings.class, Settings.DEFAULTS);
    }

    public enum EvictionMode {

        NEVER,
        DATABASE,
        ALWAYS,
        ;

        public boolean passes(Config<?> config) {
            return switch (this) {
                case ALWAYS -> true;
                case DATABASE -> config.storage() instanceof Database;
                default -> false;
            };
        }
    }
}
