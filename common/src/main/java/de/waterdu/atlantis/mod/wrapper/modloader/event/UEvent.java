package de.waterdu.atlantis.mod.wrapper.modloader.event;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.ActuallyHurtEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.VillagerTradeEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.ItemNameEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.LootTableEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.GatherTypeAdaptersEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.LeaderboardEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.RegisterModExtensionsEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.SetPlaceholderProviderEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.registry.RegistryInjectEventPayload;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.IllegalDereferenceException;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.InteractionResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class UEvent<Payload extends EventPayload> extends ModWrapper<Payload> {

    public static final Key<UEvent<ActuallyHurtEventPayload>> ACTUALLY_HURT = Key.of(Atlantis.ID, "event/actually_hurt");
    public static final Key<UEvent<VillagerTradeEventPayload.Pre>> VILLAGER_TRADE_PRE = Key.of(Atlantis.ID, "event/villager_trade/pre");
    public static final Key<UEvent<VillagerTradeEventPayload.Post>> VILLAGER_TRADE_POST = Key.of(Atlantis.ID, "event/villager_trade/post");
    public static final Key<UEvent<VillagerTradeEventPayload.XP>> VILLAGER_TRADE_XP = Key.of(Atlantis.ID, "event/villager_trade_xp");

    public static final Key<UEvent<ItemNameEventPayload>> ITEM_NAME = Key.of(Atlantis.ID, "event/item_name");
    public static final Key<UEvent<LootTableEventPayload.Pre>> LOOT_TABLE_PRE = Key.of(Atlantis.ID, "event/loot_table/pre");
    public static final Key<UEvent<LootTableEventPayload.Post>> LOOT_TABLE_POST = Key.of(Atlantis.ID, "event/loot_table/post");

    public static final Key<UEvent<ConfigEventPayload.Register>> REGISTER_CONFIG = Key.of(Atlantis.ID, "event/register_config");
    public static final Key<UEvent<ConfigEventPayload.Read>> READ_CONFIG = Key.of(Atlantis.ID, "event/read_config");
    public static final Key<UEvent<ConfigEventPayload.Write>> WRITE_CONFIG = Key.of(Atlantis.ID, "event/write_config");
    public static final Key<UEvent<ConfigEventPayload.Delete>> DELETE_CONFIG = Key.of(Atlantis.ID, "event/delete_config");

    public static final Key<UEvent<GatherTypeAdaptersEventPayload>> GATHER_TYPE_ADAPTERS = Key.of(Atlantis.ID, "event/gather_type_adapters");

    public static final Key<UEvent<LeaderboardEventPayload.Update<?, ?>>> UPDATE_LEADERBOARD = Key.of(Atlantis.ID, "event/update_leaderboard");
    public static final Key<UEvent<LeaderboardEventPayload.Recalculate.PreAll<?, ?>>> RECALCULATE_LEADERBOARD_PRE_ALL = Key.of(Atlantis.ID, "event/recalculate_leaderboard/pre_all");
    public static final Key<UEvent<LeaderboardEventPayload.Recalculate.PostAll<?, ?>>> RECALCULATE_LEADERBOARD_POST_ALL = Key.of(Atlantis.ID, "event/recalculate_leaderboard/post_all");
    public static final Key<UEvent<LeaderboardEventPayload.Recalculate.Pre<?, ?>>> RECALCULATE_LEADERBOARD_PRE = Key.of(Atlantis.ID, "event/recalculate_leaderboard/pre");
    public static final Key<UEvent<LeaderboardEventPayload.Recalculate.Post<?, ?>>> RECALCULATE_LEADERBOARD_POST = Key.of(Atlantis.ID, "event/recalculate_leaderboard/post");

    public static final Key<UEvent<PageEventPayload.Construct>> CONSTRUCT_PAGE = Key.of(Atlantis.ID, "event/construct_page");
    public static final Key<UEvent<PageEventPayload.Decorate>> DECORATE_PAGE = Key.of(Atlantis.ID, "event/decorate_page");
    public static final Key<UEvent<PageEventPayload.Destruct>> DESTRUCT_PAGE = Key.of(Atlantis.ID, "event/destruct_page");
    public static final Key<UEvent<PageEventPayload.Press>> PRESS_PAGE = Key.of(Atlantis.ID, "event/press_page");

    public static final Key<UEvent<RegisterModExtensionsEventPayload>> REGISTER_MOD_EXTENSIONS = Key.of(Atlantis.ID, "event/register_mod_extensions");
    public static final Key<UEvent<SetPlaceholderProviderEventPayload>> SET_PLACEHOLDER_PROVIDER = Key.of(Atlantis.ID, "event/set_placeholder_provider");
    public static final Key<UEvent<RegistryInjectEventPayload>> REGISTRY_INJECT = Key.of(Atlantis.ID, "event/registry_inject");

    public static final Key<UEvent<PlayerEventPayload.LogIn>> LOG_IN = Key.of(Atlantis.ID, "event/log_in");
    public static final Key<UEvent<PlayerEventPayload.LogOut>> LOG_OUT = Key.of(Atlantis.ID, "event/log_out");

    private final Key<UEvent<Payload>> eventType;

    protected UEvent(ModObjectKey type, Key<UEvent<Payload>> eventType) {
        super(new ModObject(type, null));
        this.eventType = eventType;
    }

    @Override
    public Payload dereference() {
        throw new IllegalDereferenceException("Event wrappers cannot be dereferenced");
    }

    @Override
    public CompletableFuture<Payload> future() {
        throw new IllegalDereferenceException("Event wrappers cannot be dereferenced");
    }

    public Key<UEvent<Payload>> eventType() {
        return this.eventType;
    }

    public abstract Payload emit(Payload payload);

    public abstract void listen(Function<Payload, InteractionResult> handler);

    public enum Priority {

        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST,
        ;
    }
}
