# Porting a legacy Atlantis mod to a Tine

This guide is for turning an old Forge (1.16–1.20) Atlantis mod, usually one built on Pixelmon, into a **Tine**: a
single loader-agnostic jar that Atlantis loads on Minecraft 1.21.1 under NeoForge or Fabric, with Pixelmon or
Cobblemon. AquaGyms was the first mod ported this way, and its port is used as the worked example throughout (see
[Worked example](#12-worked-example-aquagyms)).

Firstly, read the [`tine-example`](../../tine-example) README for the basics of a Tine project.

## Contents

1. [Is a Tine the right fit?](#1-is-a-tine-the-right-fit)
2. [Project setup](#2-project-setup)
3. [Lifecycle](#3-lifecycle)
4. [Old Atlantis API to Atlantis 9](#4-old-atlantis-api-to-atlantis-9)
5. [Forge events to Atlantis events](#5-forge-events-to-atlantis-events)
6. [Your mod's own events](#6-your-mods-own-events)
7. [Pixelmon to the Atlantis Pokémon API](#7-pixelmon-to-the-atlantis-pokémon-api)
8. [Features only one Pokémon mod has](#8-features-only-one-pokémon-mod-has)
9. [Minecraft 1.20 to 1.21.1](#9-minecraft-120-to-1211)
10. [Mixins in a Tine](#10-mixins-in-a-tine)
11. [Testing](#11-testing)
12. [Worked example: AquaGyms](#12-worked-example-aquagyms)
13. [Porting checklist](#13-porting-checklist)

## 1. Is a Tine the right fit?

A Tine compiles against `atlantis-common` and vanilla Minecraft only. It never sees NeoForge, Fabric, Pixelmon or
Cobblemon classes. That is what lets one jar run everywhere, and it sets the limits:

- **Server-side only.** Tines aren't loaded on the client. Anything a player sees must come from vanilla packets:
  chest menus (Atlantis pages), chat, items, entities the Pokémon mod already syncs.
- **No access transformers or access wideners.** Use `@Accessor`/`@Invoker` mixins instead.
- **No direct Pokémon mod calls.** Everything goes through `Mods.pokemon()` (see [§7](#7-pixelmon-to-the-atlantis-pokémon-api)).
  If something you need is missing, add it to Atlantis rather than reaching around it (see [§8](#8-features-only-one-pokémon-mod-has)).
- **No registries.** A Tine can't register blocks, items, entities or menus, because clients wouldn't know them.

If a mod needs any of those, it should stay a normal per-loader mod.

## 2. Project setup

1. Copy `tine-example` (or start from the existing Tine skeleton the mod was copied into). Keep `.gitignore`, the
   Gradle wrapper, `settings.gradle.kts` and `build.gradle.kts`.
2. Set the plugin version to the Atlantis version you target, in `build.gradle.kts`:
   ```kotlin
   plugins { id("de.waterdu.atlantis.tine") version "9.1.0" }
   ```
3. Delete `mods.toml`, `pack.mcmeta`, `META-INF/accesstransformer.cfg`, and any Forge-only resources.
4. Describe the mod in `src/main/resources/tine.json`. Only list `mixins` if the Tine actually has mixins; Tine
   events usually make them unnecessary.
   ```json
   {
     "$schema": "https://waterdu.de/atlantis/schemas/tine.schema.json",
     "id": "aquagyms",
     "version": "${version}",
     "name": "AquaGyms",
     "authors": ["Atlantis Development"],
     "atlantis": ">=${atlantisVersion}"
   }
   ```
   `${version}` and `${atlantisVersion}` are filled in at build time. Nothing else in the jar is templated, so
   constants like `"${mod_id}"` in Java code must become literals.
5. If the mod uses Lombok, add it yourself; the Tine plugin doesn't:
   ```kotlin
   dependencies {
       compileOnly("org.projectlombok:lombok:1.18.34")
       annotationProcessor("org.projectlombok:lombok:1.18.34")
   }
   ```
6. Replace the `@Mod` class with a `@Tine` class and a `@Tine.Barb` entrypoint:
   ```java
   @Tine
   public final class AquaGyms {
       public static final String ID = "aquagyms";
       public static final AtlantisConfigProxy.Impl CONFIG = AtlantisConfigProxy.of(AquaGyms.ID);

       private AquaGyms() {}

       @Tine.Barb
       static void barb() {
           // register listeners, commands and placeholders here
       }
   }
   ```
   There can be only one `@Tine` class per jar. Delete any `AtlantisChecker`-style "is Atlantis installed" guard;
   `tine.json`'s `atlantis` range does that job.
7. Replace log4j loggers with `TridentLogger` (`info`, `warn`, `error`, `debug`, with `{}` placeholders). It prefixes
   every line with `♆`.
8. If the mod's version is shown anywhere, read it at runtime instead of from a templated constant:
   ```java
   Mods.modLoader().mods().stream().filter(mod -> mod.id().equals(ID)).map(ModInfo::version).findFirst().orElse("unknown");
   ```

### Dev runtime with a Pokémon mod

`./gradlew runServer` always runs Fabric. Add the Fabric build of Cobblemon to try the Tine with a Pokémon mod. Three
things are needed beyond the dependency itself, each found the hard way while porting AquaGyms:

```kotlin
plugins {
    id("de.waterdu.atlantis.tine") version "9.1.0"
    // No Kotlin sources: Loom only remaps the Kotlin metadata of Kotlin mods, such as Cobblemon on the dev runtime, when this is applied.
    kotlin("jvm") version "2.2.20"
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.impactdev.net/repository/development/") { name = "ImpactDev" } }
        filter { includeGroup("com.cobblemon") }
    }
}

dependencies {
    // Transitive, because dev runs don't load the libraries Cobblemon bundles; its POM lists the same ones.
    modLocalRuntime("com.cobblemon:fabric:1.8.3+1.21.1")
}

configurations.matching { it.name.endsWith("untimeClasspath") }.configureEach {
    // Minecraft's newer icu4j would otherwise win, and the GraalJS that runs Cobblemon's Showdown can't use it.
    resolutionStrategy.force("com.ibm.icu:icu4j:71.1")
}
```

and `kotlin.stdlib.default.dependency=false` in `gradle.properties`, so the Kotlin plugin doesn't add the standard
library to the Tine. The symptoms if any of these is missing:

| Missing | Symptom |
|---|---|
| Kotlin plugin | `ClassNotFoundException: net.minecraft.class_2960` from `kotlin.reflect` while Cobblemon starts |
| Transitive dependency | `NoClassDefFoundError: kotlin/io/CloseableKt`, then `org/graalvm/polyglot/HostAccess` |
| icu4j pin | `NoClassDefFoundError: com/ibm/icu/text/DateFormat` on the "Cobblemon Showdown" thread, and the server hangs before `Done` |

None of this changes the built jar; it only affects the dev run. If you change the Atlantis or Cobblemon version
without changing the version string, delete `.gradle/loom-cache/remapped_mods/remapped/<group>` so Loom remaps them
again. Pixelmon is NeoForge-only; see [§11](#11-testing) for testing on NeoForge.

## 3. Lifecycle

| Forge | Tine |
|---|---|
| `@Mod` constructor, `FMLCommonSetupEvent` | `@Tine.Barb` (runs during mod construction, after `Mods` is ready) |
| `FMLLoadCompleteEvent` | `@Tine.Barb`, or `UEvent.SERVER_STARTING` if it needs other mods to be set up |
| `ServerStartingEvent` | `UEvent.SERVER_STARTING` (on NeoForge this fires before the server object exists; `server()` is null) |
| `ServerStartedEvent` | `UEvent.SERVER_STARTED` (configs are loaded by now) |
| `ServerStoppingEvent` / `ServerStoppedEvent` | `UEvent.SERVER_STOPPING` / `UEvent.SERVER_STOPPED` |
| `RegisterCommandsEvent` | `CommandRegistry.register(dispatcher -> ...)` from the Barb, or `ArgumentCommand.register()` |
| Config loaded callbacks | `UEvent.READ_CONFIG`, or `CONFIG.enqueue(...)` |

**Configs load after commands are first built.** Commands are registered while the server is being constructed,
before `onServerStarting` loads configs, so a command name read from a config isn't available yet. Don't call
`CONFIG.get(...)` while registering commands; it forces an early read. Use
`CONFIG.orElse(Settings.class, new Settings())` and re-register once configs have loaded. `ArgumentCommand` (below)
does this for you.

## 4. Old Atlantis API to Atlantis 9

Most renames are mechanical, and a script handles them well; AquaGyms' port did them across 90 files at once. Do
these first, then fix what's left by hand.

### Config and data

| Old | Atlantis 9 |
|---|---|
| `@AtlantisConfig(modID = ..., path = ...)` | `@FileBackedObject(mod = ..., path = ...)` |
| `loadOrder = LoadOrder.FIRST` | `order = LoadOrder.FIRST` |
| `lang = true` | `i18n = true` |
| `storageProviderClass` / `storageProviderMethod` | `providerClass` / `providerMethod` |
| `forceLoad = true` | `force = true` |
| `CONFIG.getFor(Class, key)` | `CONFIG.get(Class, key)` (a `CompletableFuture`) or `CONFIG.now(Class, key)` |
| `CONFIG.getAll(Class)` | `CONFIG.all(Class)` |
| `Data.getUUID()` | `Data.uuid()` (override it; you can keep `getUUID()` as an alias) |
| `Data.getUniqueName()` | `Data.identifier()` |
| `PlayerData.getName()` | `NamedData.name()`. **Override it**: the default returns the UUID. |
| `PlayerData.getPlayerEntity()` / `getReference()` | `entity()` / `reference()` |
| `DatabaseSettings.getStorage(modid)` | `DatabaseSettings.storage(modid)` |
| `de.waterdu.atlantis.playerdata.PlayerData` (Atlantis' own player record) | `AtlantisData.get(uuid)`; skulls via `AtlantisData.headItem(uuid)` |

### UI pages

| Old | Atlantis 9 |
|---|---|
| `getPageOptions(PlayerReference)` | `options(PlayerReference)` |
| `PageOptions.builder().setTitle(...).setRows(...).setInventoryHidden(...)` | `.title(...).rows(...).inventoryHidden(...)` |
| `options.getRows()` | `options.rows()` |
| `addButtons(PlayerReference, Set<Button>)` | `addButtons(PlayerReference, ButtonCollector)`; `collector.add(button)` |
| `onButtonClick(PlayerReference, int, ClickState)` | `onButtonClick(PlayerReference, int, Click)` (deprecated but supported; it runs on the server thread) |
| `clickState.getStyle() == ClickState.ClickStyle.SHIFT` | `click.style().isShift()` (also `isNormal()`) |
| `clickState.getButton() == ClickState.MouseButton.MIDDLE` | `click.button().isMiddle()` |
| `Button.builder().setName(...).setItem(...).setIndex(...).setLore(...).addLoreLine(...)` | `Button.builder().name(...).item(...).index(...).lore(...).loreLine(...)` |
| `Button.of(name, lore, item, index)` | unchanged, but it returns a builder: `.effect(...)` instead of `.setEffect(...)` |
| `container.getInnerContainer().getPage()` | `container.getAquaUIInventory().page()` |

`Button` *instances* still have `setName`/`setItem`/`setIndex`; only the builder methods were renamed. Rename them
inside `Button.builder() ... .build()` and `new Button.Builder() ... .build()` chains only.

### Items, levels, text, permissions

| Old | Atlantis 9 |
|---|---|
| `ParsedItemStack` | `ConfigurableItemStack` (same `of(...)`, `stack()`, `newStack()`) |
| `LazyLevel` | `ConfigurableLevel`; `getLevel()` is `get()`, `getKey()` is `key()` |
| `PermissionUtils.canUse` / `hasPermission` | `Permissions.canUse` / `hasPermission` |
| `Concurrency.mainThread(...)` | `Concurrency.sync(...)` |
| `MessageUtils.sendToAllExcept(...)` | loop over `ServerUtils.players()` |
| `pixelmon.util.EconomyUtils` | `Mods.economy()` (asynchronous; see below) |
| `ForgeRegistries.ITEMS` | `BuiltInRegistries.ITEM` |
| `Entity.getPersistentData()` | `EntityUtils.persistentData(entity)` |
| `AtlantisCommand` | `ArgumentCommand` (see below) |

**Permissions only resolve through groups.** `Permissions.hasPermission` checks the node against the configured
permission groups, which by default match `*.admin.+` (operators) and `*.user.+` (everyone). A node that matches no
group is denied, even to operators. Put every node under one of them: AquaGyms' `aquagyms.editor.gym` became
`aquagyms.admin.editor.gym`, and `aquagyms.viewteams` became `aquagyms.admin.viewteams`.

**The economy is asynchronous and optional.** `Mods.economy()` throws if there is no economy (Cobblemon without
Impactor, for example), so check `Mods.isDelegatePresent(EconomyDelegate.class)` first. `withdraw`, `deposit` and
`balance` return futures; finish the work with `Concurrency.sync(...)`. AquaGyms' `helper/EconomyHelper` wraps this:
fees are free when there's no economy, and menus use a cached balance (`canProbablyAfford`) because they can't
wait. The real check happens when the fee is taken.

### Commands: `ArgumentCommand`

Old mods parsed `String... args` themselves through `AtlantisCommand`. `de.waterdu.atlantis.util.server.ArgumentCommand`
keeps that style on Brigadier, so a large command class ports with a few signature changes:

| `AtlantisCommand` | `ArgumentCommand` |
|---|---|
| `getName()` | `name()` |
| `getAliases()` | `aliases()` |
| `execute(CommandContext, String... args) throws CommandRuntimeException` | `execute(CommandContext, String... args)` |
| `getTabCompletions(CommandContext, SuggestionsBuilder, String[] args)` | `suggestions(CommandContext, String... args)` (filtered by what's typed for you) |
| `getUsage`, `getRequiredPermissionLevel`, `checkPermission` | delete them; override `requirement()` if needed |
| `match(arg, options...)`, `send(source, message)` | unchanged |
| registered in `RegisterCommandsEvent` | `new MyCommand().register()` in the Barb |

`register()` re-registers the command once the server has started, so names and aliases read from a config
(through `CONFIG.orElse`, see [§3](#3-lifecycle)) take effect without a reload.

## 5. Forge events to Atlantis events

Listen with `Mods.modLoader().event(UEvent.KEY).listen([priority,] payload -> InteractionResult)`. Return
`InteractionResult.PASS` to continue; anything else cancels a cancellable payload and skips later listeners.

| Forge / Pixelmon event | Atlantis |
|---|---|
| `ServerChatEvent` | `UEvent.CHAT` (cancellable; `player()`, `text()`, `message()`) |
| `CommandEvent` | `UEvent.COMMAND` (cancellable; `source()`, `parse()`, `command()`, `rootLiteral()`) |
| `PlayerInteractEvent.EntityInteract(Specific)`, `AttackEntityEvent` | `UEvent.PLAYER_INTERACT_ENTITY` (cancellable; `action()` is `ATTACK`, `INTERACT` or `INTERACT_AT`) |
| `EntityJoinLevelEvent` | `UEvent.ENTITY_JOIN_LEVEL` (cancellable; `loadedFromDisk()`) |
| `PlayerLoggedInEvent` / `PlayerLoggedOutEvent` | `UEvent.LOG_IN` / `UEvent.LOG_OUT` |
| `TickEvent.ServerTickEvent` (`phase == START`/`END`) | `UEvent.SERVER_TICK_PRE` / `SERVER_TICK_POST` |
| Server lifecycle events | `UEvent.SERVER_STARTING/STARTED/STOPPING/STOPPED` |
| `BattleStartedEvent.Pre` / `.Post` | `UEvent.POKEMON_BATTLE_START_PRE` (cancellable) / `POKEMON_BATTLE_START_POST` |
| `BattleEndEvent` | `UEvent.POKEMON_BATTLE_END` (`results()`, `winners()`, `losers()`); fires for every way a battle ends on both mods |
| `ExperienceGainEvent` | `UEvent.POKEMON_EXPERIENCE_GAIN` (cancellable; `fromBattle()`) |
| Capture, evolve, level up, faint, send out, trade, … | the other `UEvent.POKEMON_*` keys |

Some things to know:

- **A right-click arrives twice.** Clients send `INTERACT_AT` and then `INTERACT`, once per hand. To take over
  right-clicks on an entity, cancel every action on it and act only on `INTERACT` with `InteractionHand.MAIN_HAND`.
- **Don't cancel chat with `INTERCEPT_PACKET`.** That hook runs on the network thread before the signed message
  chain is checked; dropping a chat packet there breaks the chain and kicks the player. Use `UEvent.CHAT`.
- **Commands from any source go through `COMMAND`,** including the console and command blocks. Check `source()`.
- **Look up a command by its root literal,** and strip a namespace, so `/pixelmon:endbattle` and `/endbattle` match.
- **Cancelling `ENTITY_JOIN_LEVEL` for an entity loaded from disk removes it for good** once its chunk saves.

## 6. Your mod's own events

Forge `Event` subclasses posted on a private `IEventBus` become Atlantis payloads with keys your mod owns. Other
Tines and mods subscribe to them exactly like Atlantis' own events.

```java
public final class AquaGymsEvents {
    public static final Key<UEvent<AddBadgeEvent>> ADD_BADGE = Key.of(AquaGyms.ID, "event/add_badge");

    /** Fires the event, returning whether a listener cancelled it, like Forge's {@code post}. */
    @SuppressWarnings("unchecked")
    public static <E extends EventPayload> boolean post(E event) {
        var payload = Mods.modLoader().event((Key<UEvent<E>>) event.type()).emit(event);
        return payload instanceof Cancellable cancellable && cancellable.cancelled();
    }
}

@Getter
public class AddBadgeEvent implements EventPayload, Cancellable {
    private boolean cancelled = false;
    // ...the old fields and constructor, unchanged...

    @Override public Key<?> type() { return AquaGymsEvents.ADD_BADGE; }
    @Override public void cancel() { this.cancelled = true; }
    @Override public boolean cancelled() { return this.cancelled; }
}
```

Keeping the class names, packages and getters means the call sites only change from `EVENT_BUS.post(...)` to
`AquaGymsEvents.post(...)`. `@Cancelable` events implement `Cancellable`; `setCanceled(true)` becomes `cancel()`.

Fields typed as Pixelmon classes (`BattleResults`, `NPCTrainer`) must change type. Where your code needs to *create*
a value (for example forcing a result to `DEFEAT`), use your own enum rather than an Atlantis wrapper, because
wrappers only come from the Pokémon mod. AquaGyms uses `GymResult`.

## 7. Pixelmon to the Atlantis Pokémon API

`Mods.pokemon()` returns the `PokemonDelegate` for whichever Pokémon mod is installed. Its wrappers (`UPokemon`,
`UParty`, `UBattle`, …) behave the same on Pixelmon and Cobblemon.

| Pixelmon | Atlantis |
|---|---|
| `Pokemon` | `UPokemon<?>` |
| `StorageProxy.getParty(player)` / `getPCForPlayer` | `Mods.pokemon().party(PlayerReference.get(player))` / `.pc(...)` |
| `party.getAll()` (with nulls) / `getTeam()` | `party.all()` (no nulls), `party.get(slot)` |
| `PixelmonSpecies.fromName(name)` | `Mods.pokemon().hasSpecies(name)` then `.species(name)` |
| `species.getForms()`, `pokemon.getForm()` | `species.forms()`, `pokemon.form()` |
| `Element` | `UElementalType<?>` (`pokemon.types()`, `move.template().elementalType()`) |
| `pokemon.getPersistentData()` | `pokemon.persistentData()` |
| `pokemon.getMoveset()`, `Attack.ppLevel` | `pokemon.moveset().moves()`, `move.ppUps()` / `setPPUps(int)` |
| `pokemon.getHeldItemAsItemHeld()` | `pokemon.heldItem()` (an `ItemStack`, empty for none) |
| `SpriteItemHelper.getPhoto(pokemon)` | `pokemon.toItem()` |
| `PixelmonItems.poke_ball` | `Mods.pokemon().pokeBallItem("poke_ball")` |
| `BattleRegistry.getBattle(player)` | `Mods.pokemon().battle(player)` / `isInBattle(player)` |
| `BattleRegistry.startBattle(...)` | `startPlayerBattle(...)`, `startTrainerBattle(...)`, `startWildBattle(...)` |
| `BattleRules` + `BattleRuleRegistry.*` | `BattleSettings.builder()...build()` |
| `BattleClauseRegistry` | `Mods.pokemon().battleClauses()` / `battleClause(id)`; `clause.validate(team)` |
| `NPCTrainer` | `UTrainer<?>`: `Mods.pokemon().spawnTrainer(...)`, `trainer(entity)` |
| `bc.endBattle(BattleEndCause.FORFEIT, results)` | `battle.forfeit(loserParticipant)`; `battle.end()` ends it with no winner |
| `pps.retrieveAll(...)` | `party.retrieveAll()` |
| `PokemonConverterFactory.importText/getExportText` | `Mods.pokemon().importShowdown(text)` / `exportShowdown(team)`, or `ShowdownFormat` |
| `PokeBagItem.PokeBag` (and shulker box NBT) | `Mods.pokemon().containedItems(stack)` |

### Battles

```java
var settings = BattleSettings.builder()
        .levelCap(50).raiseToCap(true)
        .format(UBattleFormat.SINGLES)
        .teamSize(6)
        .clauses(List.of("species_clause"))
        .fullHeal(true)
        .experience(false)
        .cloneTeams(true)
        .build();

var trainer = Mods.pokemon().spawnTrainer(level, position, yaw, Component.literal("Lt. Surge"));
trainer.setTeam(team);
trainer.setSkin("SomePlayerName");

Mods.pokemon().startTrainerBattle(player, chosenTeam, trainer, new TrainerSettings("tactical", null), settings)
        .thenAccept(battle -> { if (battle == null) { /* couldn't start */ } });
```

- **Teams.** A `null` team means the player's real party. An explicit list may include PC Pokémon or Pokémon that
  don't exist anywhere (imported from Showdown text); set `cloneTeams` so nothing in the battle touches the
  originals. With `experience(false)`, cloning is forced on Cobblemon, since that's the only way to stop XP there.
- **Temporary parties are gone.** Old code swapped a team into the party with `setInTemporaryMode` and swapped it
  back afterwards. Pass the team to the battle instead.
- **Futures.** Battle starts return `CompletableFuture<@Nullable UBattle<?>>`, which completes with `null` when the
  battle couldn't start (someone's already battling, a clause failed, a listener cancelled it). On Pixelmon with a
  team selection screen, it completes after selection.
- **Tracking your battles.** Don't tag entities with NBT markers to recognise your battles later. Keep an
  in-memory map: put the challenger in a "pending" map before starting, move it to an "active" map keyed by
  `battle.id()` in `POKEMON_BATTLE_START_POST`, and take it out in `POKEMON_BATTLE_END`. See AquaGyms' `GymBattles`.
- **Results.** `BattleEventPayload.End.results()` maps every participant to victory, defeat, draw or flee. A battle
  that was stopped (a command, a disconnect, an error) gives everyone a draw.
- **NPC messages.** Neither mod's NPC has greeting, win or lose text fields any more; send them as chat messages
  yourself on start and end.
- **Right-clicking trainers.** `spawnTrainer` creates an NPC with no interactions. Use `PLAYER_INTERACT_ENTITY` to
  challenge it.

### Wrappers are views

Every lookup returns a new wrapper object, so never compare wrappers with `==`. Compare `uuid()`s, or for Pokémon
recreated from Showdown text (which get a new UUID each time), compare their Showdown text.

### Showdown text

`ShowdownFormat.read(Mods.pokemon(), text, formAliases)` parses Showdown team text into `UPokemon`s on both mods.
Species, abilities, moves, natures and balls are looked up by several spellings (`Swords Dance`, `swordsdance`,
`swords_dance`). `formAliases` maps Showdown formes to the mod's form names where they differ, such as `alola` to
`Alolan`. Held items are matched by id in the mod's namespace (`Choice Scarf` → `cobblemon:choice_scarf`).

## 8. Features only one Pokémon mod has

Check `Mods.pokemon().supports(PokemonFeature.X)` and degrade gracefully:

| Feature | Pixelmon 9.4.2 | Cobblemon 1.8.3 |
|---|---|---|
| `TRAINER_BATTLES`, `LEVEL_CAP`, `RAISE_TO_CAP`, `BATTLE_AI`, `IN_BATTLE_CLAUSES` | yes | yes |
| `TEAM_PREVIEW` | yes | no (Cobblemon ignores team preview requests) |
| `TEAM_SELECTION_SCREEN` | yes | no |
| `TURN_TIMER` | yes | no |
| `BOSS_TIERS` | yes | no |
| `BATTLE_GIMMICKS` (mega, dynamax) | yes | no |

Option lists come from the mod: `trainerAIs()` (Pixelmon AI ids such as `tactical`; Cobblemon skill levels `0`–`5`),
`bossTiers()`, `battleGimmicks()`, and `battleClauses()` (Pixelmon's datapack rules such as `species_clause` and
`ban/baton_pass_clause`; Cobblemon gets Atlantis' `TeamClause` set).

Patterns that worked in AquaGyms:

- **Keep menus stable.** Editors that address buttons by slot shouldn't hide unsupported options, or every later
  slot shifts. Show a greyed-out "Not supported by this Pokémon mod" button in the same slot and ignore its clicks.
- **Pick mod-specific defaults.** Config defaults that name items should come from whichever mod is installed when
  the default config is written. AquaGyms' `helper/ModItems.pick(pixelmonId, cobblemonId, vanillaId)` and
  `shared(path, vanillaId)` (for items both mods name the same, like `rare_candy`) do this.
- **Store your own types, not the mod's.** Replace `Element` fields with your own enum (AquaGyms reuses its
  `TypeHelper`) and convert from `UElementalType.name()`.
- **Hide fees without an economy.**

### What Pixelmon removed in 9.x

Code written for Pixelmon on 1.16–1.20 often used APIs that no longer exist in Pixelmon 9.4.2. These were checked
against the 9.4.2 jar:

- **Quests are gone.** There's no quest system at all (only `QuestItem`/`QuestEditorItem` items remain). Quest
  objectives and quest requirements must be removed. The research system (`ResearchObjective`) is the closest thing,
  but it's a different feature.
- `NPCTrainer` is gone; NPCs are `NPC` built with `NPC.builder()`.
- `BattleRuleRegistry` and `BattleClauseRegistry` are gone. Settings are `ContextKeys`, clauses are datapack
  `BattleRule`s in `PixelmonRegistry.BATTLE_RULE_REGISTRY`.
- `BattleController` is an interface; fields like `battleIndex`, `participants`, `battleTurn` and `battleEnded` are
  methods.
- Temporary party mode (`setInTemporaryMode`) is gone.
- `BattleBuilder.start()` needs `registryAccess(...)` set first, or it throws.
- NPC skins are set from a URL (`PlayerSkinURLTextureProvider`), not a player name. Atlantis' `PlayerSkins.find(name)`
  looks the URL up for you.

### Cobblemon 1.8 notes

- `PokedexEntryProgress` is now `UNREGISTERED`, `SEEN`, `OWNED`.
- `BATTLE_ENDED` fires for every way a battle ends, and `BATTLE_VICTORY` now fires after the battle has closed.
- `BattleFormat` constants are shared; always `copy(...)` them before changing rules or `adjustLevel`.
- `pvn` doesn't check whether the NPC is already battling, and with `adjustLevel` it changes the NPC's real Pokémon.
  Atlantis gives the NPC fresh copies for every battle.

### When something is missing

Add it to Atlantis: a method on `PokemonDelegate` or a wrapper, implemented in `neoforge/.../mod/impl/pixelmon/` and
`common/.../mod/impl/cobblemon/`. Check the real API with `javap` or a decompiler (Vineflower is in the Gradle cache,
from Loom) rather than from memory; both mods changed a lot between versions.

A Tine *can* compile against Pixelmon directly as a last resort, but it's fragile: Atlantis' bootstrap loads every
annotated class on every loader, so any `@Bootstrapped` or config class that touches Pixelmon crashes Fabric, and
Pixelmon's events need the NeoForge bus API to compile. If you must, keep the Pixelmon code in an isolated package
with no Atlantis annotations, behind an interface, entered only after `Mods.isPresent("pixelmon")`.

## 9. Minecraft 1.20 to 1.21.1

- **Item NBT is now data components.** `stack.getTag()`, `getTagElement("BlockEntityTag")` and
  `ContainerHelper.loadAllItems` on item NBT don't work. Use `stack.get(DataComponents.CONTAINER)` and
  `BUNDLE_CONTENTS`, or `Mods.pokemon().containedItems(stack)`. Names and lore go through
  `ItemUtils.setDisplayName`/`setDisplayLore`. Skulls use `DataComponents.PROFILE`.
- **`new ResourceLocation(...)` is gone.** Use `ResourceLocation.parse`, `fromNamespaceAndPath` or `tryParse`.
- **`CommandRuntimeException` is gone.**
- **`Entity.getPersistentData()` is NeoForge-only.** `EntityUtils.persistentData(entity)` works on both loaders (it's
  saved under `AtlantisData`).
- **`javax.annotation.Nullable` isn't on the classpath.** Use `org.jspecify.annotations.Nullable`.

## 10. Mixins in a Tine

Most legacy mods only needed mixins for things Atlantis events now cover. If you do need one:

- Write it against Mojang names; there is no refmap, and Atlantis remaps the jar on Fabric production servers.
- List the config in `tine.json`'s `mixins`.
- Don't target classes that load before Tines are discovered on NeoForge (during mod construction), or Mixin fails
  with "loaded too early". Packet listeners, menus and entities are fine.
- No access wideners; use `@Accessor` and `@Invoker`.
- Reflection that looks up Minecraft members by name breaks on Fabric production, because only bytecode references
  are remapped.

## 11. Testing

Test on every combination you claim to support: Fabric + Cobblemon, NeoForge + Pixelmon, NeoForge + Cobblemon.

**Fabric + Cobblemon (dev).** With Cobblemon on `modLocalRuntime` ([§2](#dev-runtime-with-a-pokémon-mod)), run
`./gradlew runServer`, wait for `Done`, then `./gradlew runClient`. The client needs Cobblemon too, which the dev
runtime gives it.

**NeoForge (Atlantis' dev server).**

1. Build the Tine: `./gradlew build` → `build/libs/<name>-<version>.jar`.
2. In the Atlantis repo, put the Pokémon mod in `neoforge/versions/1.21.1/run/mods/` (never both), and the Tine jar
   in `neoforge/versions/1.21.1/run/config/atlantis/trident/`.
   - **Pixelmon:** `Pixelmon-1.21.1-9.4.2-universal.jar` (Gradle downloads it into its cache when building
     Atlantis).
   - **Cobblemon:** `com.cobblemon:neoforge:1.8.3+1.21.1`, plus KotlinForForge 5.10 or later from
     `https://thedarkcolour.github.io/KotlinForForge/`. KotlinForForge is four jars:
     `kotlinforforge-neoforge` (only the Kotlin libraries), `kfflang-neoforge` (the language provider),
     `kffmod-neoforge` and `kfflib-neoforge`. With only the first, NeoForge reports that Cobblemon "needs language
     provider kotlinforforge".
   - Cobblemon 1.8.3 needs NeoForge newer than its declared `[21,)`; on 21.1.100 it fails with
     `NoSuchMethodError: Registry.getKeyOrNull`. Atlantis builds against 21.1.265.
3. Run `./gradlew :neoforge:1.21.1:runServer`. The NeoForge dev server stops when stdin closes, so drive it over RCON
   (`enable-rcon=true`, `rcon.password=...` in `server.properties`), and set `online-mode=false` to join with a dev
   client.

**What to check,** at least:

- The Tine loads (`♆ Loaded 1 Tine(s)`), and its configs generate with defaults that suit the installed mod.
- Every command, permission and menu works; chat input works, and normal chat still works afterwards.
- Every battle path starts, ends normally, ends by forfeit or command, and ends by a disconnect, and the end is
  handled each time.
- Mod-specific options are hidden or disabled where they aren't supported.
- On a Fabric production server, the jar is remapped on first load (`config/atlantis/trident/.cache`).

## 12. Worked example: AquaGyms

The AquaGyms port touched almost every file. The interesting parts:

| Area | Before | After |
|---|---|---|
| Entrypoint | `@Mod` with FML listeners and a private Forge bus | `AquaGyms` `@Tine` with a Barb |
| Events | 11 Forge `Event` classes on `AquaGyms.EVENT_BUS` | same classes as payloads, keys in `api/AquaGymsEvents` |
| Battle tracking | `GymID`/`GymChallenger` NBT on players and NPCs | in-memory `gym/GymBattles` |
| NPC battles | Pixelmon's own NPC battle, intercepted in `BattleStartedEvent` | `PLAYER_INTERACT_ENTITY` on permanent NPCs → `startTrainerBattle` |
| Team swapping | Pixelmon temporary party mode | explicit teams with `cloneTeams` |
| Showdown | a Pixelmon `PokemonConverterProxy` | `ShowdownFormat` (moved into Atlantis) |
| Rules | Pixelmon `BattleRules` | `BattleRulesAG.getBattleSettings(...)` |
| Team checks | `PlayerPartyStorage`, `PokeBagItem`, shulker NBT | `List<UPokemon<?>>`, `containedItems` |
| Fees | synchronous `EconomyUtils` | asynchronous `helper/EconomyHelper` |
| `/endbattle` | `CommandEvent` + `instanceof EndBattleCommand` | `UEvent.COMMAND` + root literal `endbattle`/`stopbattle` |
| Commands | `AtlantisCommand` | `ArgumentCommand` |
| Editor chat input | `ServerChatEvent` | `UEvent.CHAT` |
| Quests | three Pixelmon quest objectives | removed (no quest API in Pixelmon 9.4) |

Testing found three Atlantis bugs that only show on a real server, all fixed in the same effort: Pixelmon battles
never started (`BattleBuilder` needed `registryAccess`), the Cobblemon integration never activated on 1.8 (Atlantis
looked for a `CobblemonKt` class that no longer exists), and Showdown formes like `Ninetales-Alola` lost their form on
Cobblemon. Treat "it compiles" as the start of testing, not the end.

Atlantis gained, in the same effort: Pixelmon 9.4.2 / Cobblemon 1.8.3 / NeoForge 21.1.265 support, the server lifecycle, chat,
command, entity interaction and entity join events, `EntityUtils.persistentData`, trainer battles and
`BattleSettings`, `UTrainer`, battle clauses, `PokemonFeature`, Showdown import and export, `containedItems`,
`PlayerSkins`, and `ArgumentCommand`.

## 13. Porting checklist

- [ ] Tine project builds with the right plugin version; `tine.json` is correct; Lombok added if used.
- [ ] `@Mod` → `@Tine` + `@Tine.Barb`; no templated constants or Atlantis checker left.
- [ ] Mechanical renames done ([§4](#4-old-atlantis-api-to-atlantis-9)).
- [ ] All Forge events moved to `UEvent`s; own events are payloads with own keys.
- [ ] No `com.pixelmonmod` imports; Pixelmon types replaced by wrappers or own enums.
- [ ] Removed Pixelmon features (quests, temporary parties) removed, not stubbed.
- [ ] Mod-specific features gated by `supports(...)`; config defaults picked per mod.
- [ ] Permission nodes all under `*.admin.*` or `*.user.*`.
- [ ] Economy optional and asynchronous.
- [ ] Commands on `ArgumentCommand` or Brigadier; config-driven names use `CONFIG.orElse`.
- [ ] Item NBT replaced with data components.
- [ ] Tested on Fabric + Cobblemon, NeoForge + Pixelmon and NeoForge + Cobblemon ([§11](#11-testing)).
