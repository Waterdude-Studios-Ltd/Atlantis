package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public class AtlantisInventoryInternal extends SimpleContainer {

    protected Page page;
    protected Buttons buttons;

    protected final ButtonCollector collector = ButtonCollector.of(button ->
            this.buttons.setButton(button.index(), button.group(), button.page(), button)
    );

    protected PageOptions options;
    protected boolean draggable;
    protected boolean closed = false;

    protected PlayerReference reference;
    protected AtlantisContainerMenuInternal container;

    public AtlantisInventoryInternal(Page page, ServerPlayer player, int slots) {
        this(page, player, slots, null);
    }

    public AtlantisInventoryInternal(Page page, ServerPlayer player, int slots, Set<Button> buttons) {
        super(slots);
        this.page = page;
        this.reference = PlayerReference.get(player);
        this.options = page.options(this.reference);
        this.buttons = new Buttons(this.options, slots, 1, player);
        if (buttons != null) {
            this.collector.collect(buttons);
        } else {
            page.addButtons(this.reference, this.collector);
        }
        this.draggable = this.options().isDraggable();
        var def = AtlantisData.decorations(page);
        if (def != null) {
            var event = Mods.modLoader().event(UEvent.DECORATE_PAGE).emit(new PageEventPayload.Decorate(this.reference, page, def, def.buttons()));
            if (!event.cancelled()) {
                this.collector.collect(event.decorations());
            }
        }
        this.collector.unlock();
    }

    public AtlantisInventoryInternal(Page page, ServerPlayer player, Buttons buttons, int slots) {
        super(slots);
        this.reference = PlayerReference.get(player);
        this.options = page.options(this.reference);
        this.draggable = this.options().isDraggable();
        this.buttons = buttons;
        this.page = page;
        this.collector.unlock();
    }

    public void setContainer(AtlantisContainerMenuInternal container) {
        this.container = container;
    }

    public PageOptions options() {
        return this.options;
    }

    public int rows() {
        return this.options().rows();
    }

    public void onUpdate(ServerPlayer player, AtlantisContainerMenuInternal container) {
        Atlantis.THREAD_POOL.submit(() -> {
            if (this.collector.isUnlocked()) {
                long tick = player.level().getGameTime();
                this.buttons = this.page.onUpdate(tick, this.reference, this.buttons);
                this.buttons.invokeUpdateTasks(tick);
            }
            container.updateDisplay(this.reference, false);
            container.updateDisplayEnqueued(this.reference);
        });
    }

    @Override
    public int getContainerSize() {
        PageOptions.FormFactor ff = this.options().formFactor();
        return ff.hasRows() ? this.options().rows() * 9 : ff.slots();
    }

    @Override
    public boolean isEmpty() {
        if (this.draggable) {
            return super.isEmpty();
        }
        return false;
    }

    @Override
    public @NonNull ItemStack getItem(int index) {
        if (this.draggable) {
            return super.getItem(index);
        }
        var button = this.buttons.button(index);
        return button == null ? ItemStack.EMPTY : button.display(this.reference);
    }

    @Override
    public @NonNull ItemStack removeItem(int index, int count) {
        if (this.draggable) {
            return super.removeItem(index, count);
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
        return ItemStack.EMPTY;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int index) {
        if (this.draggable) {
            return super.removeItemNoUpdate(index);
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, @NonNull ItemStack stack) {
        if (this.draggable) {
            super.setItem(index, stack);
            return;
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
    }

    @Override
    public int getMaxStackSize() {
        if (this.draggable) {
            return super.getMaxStackSize();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public void setChanged() {
        if (this.draggable) {
            super.setChanged();
            if (!this.container.enqueueUpdate) {
                this.page.onChanged(this.reference, this, false);
            }
        }
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        if (this.draggable) {
            return super.stillValid(player);
        }
        return false;
    }

    @Override
    public void startOpen(@NonNull Player player) {
        if (this.draggable) {
            super.startOpen(player);
        }
    }

    @Override
    public void stopOpen(@NonNull Player player) {
        if (this.draggable) {
            this.page.onChanged(this.reference, this, true);
            super.stopOpen(player);
        }
    }

    @Override
    public boolean canPlaceItem(int index, @NonNull ItemStack stack) {
        return this.draggable;
    }

    @Override
    public void clearContent() {
        if (this.draggable) {
            super.clearContent();
        }
    }

    public int resolveGroup(int groupID) {
        return this.buttons.resolveGroup(groupID);
    }

    public Buttons.ButtonPageGroup group(int groupID) {
        return this.buttons.group(groupID);
    }

    public Component name() {
        return this.options().title().compile();
    }

    public MutableComponent displayName() {
        var title = this.options().title();
        if (Settings.get().isUsePlaceholdersInPages()) {
            title = AtlantisPlaceholderProxy.replace(this.reference.entityDirect(), title);
        }
        return title.compile();
    }

    public Page page() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public PlayerReference player() {
        return this.reference;
    }

    public void setPlayer(PlayerReference reference) {
        this.reference = reference;
    }

    public Buttons buttons() {
        return this.buttons;
    }

    public void setButtons(Buttons buttons) {
        this.buttons = buttons;
    }

    public boolean isDraggable() {
        return this.draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void setMaxStackSize(int size) {
        // Arclight hack
    }
}