package de.waterdu.atlantis.mod;

import com.mojang.brigadier.ParseResults;
import de.waterdu.atlantis.mod.impl.common.event.entity.ActuallyHurtEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.EntityJoinLevelEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.InteractEntityEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.ItemNameEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.LootTableEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.SetPlaceholderProviderEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.player.ChatEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.player.CommandEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Emits the loader-neutral events that mixins fire. Get the instance with {@link Mods#events()}.
 */
public final class ModEvents {

    public static final ModEvents INSTANCE = new ModEvents();

    /**
     * Creates the single instance.
     */
    private ModEvents() {}

    /**
     * @return the damage after the listeners change it, or 0 if a listener canceled the event
     */
    public float emitActuallyHurt(LivingEntity entity, DamageSource source, float damage) {
        var payload = Mods.modLoader().event(UEvent.ACTUALLY_HURT).emit(new ActuallyHurtEventPayload(entity, source, damage));
        return payload.cancelled() ? 0F : payload.amount();
    }

    /**
     * Emits LOOT_TABLE_PRE before a loot table makes its drops.
     *
     * @param context the loot context
     * @param consumer the consumer that receives the drops
     * @return the payload after all listeners ran
     */
    public LootTableEventPayload.Pre emitLootTablePre(LootContext context, Consumer<ItemStack> consumer) {
        return Mods.modLoader().event(UEvent.LOOT_TABLE_PRE).emit(new LootTableEventPayload.Pre(context, consumer));
    }

    /**
     * Emits LOOT_TABLE_POST after a loot table makes its drops.
     *
     * @param context the loot context
     * @param drops the drops of the loot table
     * @return the payload after all listeners ran
     */
    public LootTableEventPayload.Post emitLootTablePost(LootContext context, List<ItemStack> drops) {
        return Mods.modLoader().event(UEvent.LOOT_TABLE_POST).emit(new LootTableEventPayload.Post(context, drops));
    }

    /**
     * Emits ITEM_NAME when the tooltip of an item stack gets its name.
     *
     * @param stack the item stack
     * @param player the player that sees the name, or {@code null} if there is none
     * @param context the tooltip context
     * @param name holds the name. Listeners can replace it.
     * @param tooltipFlag the tooltip flag
     * @return the payload after all listeners ran
     */
    public ItemNameEventPayload emitItemName(ItemStack stack, @Nullable Player player, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        return Mods.modLoader().event(UEvent.ITEM_NAME).emit(new ItemNameEventPayload(stack, player, context, name, tooltipFlag));
    }

    /**
     * Emits SET_PLACEHOLDER_PROVIDER before a placeholder provider is set.
     *
     * @param provider the placeholder provider
     * @return the payload after all listeners ran. Its provider is the one to set.
     */
    public SetPlaceholderProviderEventPayload emitSetPlaceholderProvider(PlaceholderProvider provider) {
        return Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).emit(new SetPlaceholderProviderEventPayload(provider));
    }

    /**
     * @return {@code true} if a listener canceled the event
     */
    public boolean emitChat(ServerPlayer player, PlayerChatMessage message) {
        return Mods.modLoader().event(UEvent.CHAT).emit(new ChatEventPayload(player, message)).cancelled();
    }

    /**
     * @return {@code true} if a listener canceled the event
     */
    public boolean emitCommand(ParseResults<CommandSourceStack> parse, String command) {
        return Mods.modLoader().event(UEvent.COMMAND).emit(new CommandEventPayload(parse, command)).cancelled();
    }

    /**
     * @return {@code true} if a listener canceled the event
     */
    public boolean emitInteractEntity(ServerPlayer player, Entity entity, InteractEntityEventPayload.Action action, @Nullable InteractionHand hand) {
        return Mods.modLoader().event(UEvent.PLAYER_INTERACT_ENTITY).emit(new InteractEntityEventPayload(player, entity, action, hand)).cancelled();
    }

    /**
     * @return {@code true} if a listener canceled the event
     */
    public boolean emitEntityJoinLevel(Entity entity, ServerLevel level, boolean loadedFromDisk) {
        return Mods.modLoader().event(UEvent.ENTITY_JOIN_LEVEL).emit(new EntityJoinLevelEventPayload(entity, level, loadedFromDisk)).cancelled();
    }
}
