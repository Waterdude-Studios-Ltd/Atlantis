package de.waterdu.atlantis.ui.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.internal.AtlantisContainerMenuInternal;
import de.waterdu.atlantis.ui.internal.AtlantisInventoryInternal;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.ui.internal.InventoryListener;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h1>AtlantisUI</h1>
 * Chest UIs made in Forge directly.
 *
 * @author Sam Plummer sam@waterdu.de
 * @version 1.0
 * @since 02/03/2019
 */
public final class AtlantisUI {

    /**
     * Pages that open when the player has no other screen open. The server tick opens and removes them.
     * {@link Page#CLOSURE_PAGE} closes the current page instead.
     */
    public static final Map<UUID, Page> FUTURE_OPEN = new ConcurrentHashMap<>();
    /**
     * Players whose button presses have no effect. A button press adds the player.
     * The next display update or page open removes the player.
     */
    public static final Set<UUID> LOCKED = Sets.newConcurrentHashSet();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private AtlantisUI() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Registers the server tick listener that updates and opens pages. Only the first call has an effect.
     */
    public static void register() {
        if (!AtlantisUI.REGISTERED.getAndSet(true)) {
            InventoryListener.register();
        }
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @return whether this action was successful
     */
    public static boolean open(PlayerReference player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @return whether this action was successful
     */
    public static boolean open(Player player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null);
    }

    /**
     * Updates a UI page. Keeps the page and group positions and updates the button contents.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @return whether this action was successful
     */
    public static boolean update(PlayerReference player, @Nullable Page page) {
        return AtlantisUI.open(player, page);
    }

    /**
     * Updates a UI page. Keeps the page and group positions and updates the button contents.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @return whether this action was successful
     */
    public static boolean update(Player player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null, false, true);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * You can supply a {@link Buttons} object to override the contents of this page.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param buttons a {@link Buttons} object to apply to this page
     * @return whether this action was successful
     */
    public static boolean open(PlayerReference player, @Nullable Page page, @Nullable Buttons buttons) {
        return AtlantisUI.open(player, page, buttons, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * You can supply a {@link Buttons} object to override the contents of this page.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param buttons a {@link Buttons} object to apply to this page
     * @return whether this action was successful
     */
    public static boolean open(Player player, @Nullable Page page, @Nullable Buttons buttons) {
        return AtlantisUI.open(player, page, buttons, false, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * The page can wait until the player closes their current page. Then it opens.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param waitForScreen whether to wait until the player closes their current page
     * @return whether this action was successful
     */
    public static boolean open(PlayerReference player, @Nullable Page page, boolean waitForScreen) {
        return AtlantisUI.open(player, page, null, waitForScreen);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * The page can wait until the player closes their current page. Then it opens.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param waitForScreen whether to wait until the player closes their current page
     * @return whether this action was successful
     */
    public static boolean open(Player player, @Nullable Page page, boolean waitForScreen) {
        return AtlantisUI.open(player, page, null, waitForScreen, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param buttons a {@link Buttons} object to apply to this page
     * @param waitForScreen whether to wait until the player closes their current page
     * @return whether this action was successful
     */
    public static boolean open(
            PlayerReference player,
            @Nullable Page page,
            @Nullable Buttons buttons,
            boolean waitForScreen
    ) {
        return AtlantisUI.open(player.entityDirect(), page, buttons, waitForScreen);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param buttons a {@link Buttons} object to apply to this page
     * @param waitForScreen whether to wait until the player closes their current page
     * @return whether this action was successful
     */
    public static boolean open(
            Player player,
            @Nullable Page page,
            @Nullable Buttons buttons,
            boolean waitForScreen
    ) {
        return AtlantisUI.open(player, page, buttons, waitForScreen, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param player the user to display the page to
     * @param page the page to display. null closes any currently open page.
     * @param buttons a {@link Buttons} object to apply to this page
     * @param waitForScreen whether to wait until the player closes their current page
     * @param persistence whether to copy the current page placements and update task states to the new page
     * @return whether this action was successful
     */
    public static boolean open(
            Player player,
            @Nullable Page page,
            @Nullable Buttons buttons,
            boolean waitForScreen,
            boolean persistence
    ) {
        var result = false;
        if (player instanceof ServerPlayer serverPlayer) {
            var reference = PlayerReference.get(serverPlayer);
            result = true;
            if (waitForScreen) {
                AtlantisUI.FUTURE_OPEN.put(serverPlayer.getUUID(), page);
            } else {
                if (page != null) {
                    Atlantis.THREAD_POOL.submit(() -> {
                        var options = page.options(reference);
                        var formFactor = options.formFactor();
                        var slots = formFactor.hasRows() ? options.rows() * 9 : formFactor.slots();
                        var container = AtlantisUI.currentContainer(player);
                        var old = container != null ? container.atlantisInventory().buttons() : null;
                        var inventory = AtlantisInventoryInternal.create(page, serverPlayer, buttons, slots);

                        if (old != null && persistence) {
                            inventory.buttons().persist(old);
                        }

                        var event = Mods.modLoader().event(UEvent.CONSTRUCT_PAGE).emit(
                                new PageEventPayload.Construct(reference, page, inventory.buttons(), options)
                        );
                        if (!event.cancelled()) {
                            inventory.setButtons(event.buttons());
                            var newContainer = new AtlantisContainerMenuInternal(inventory, serverPlayer);
                            inventory.onUpdate(serverPlayer, newContainer);
                            serverPlayer.containerMenu = newContainer;
                            serverPlayer.setContainerCounter(1);
                            serverPlayer.connection.send(new ClientboundOpenScreenPacket(serverPlayer.getContainerCounter(), formFactor.getType(options), inventory.displayName()));
                            serverPlayer.containerMenu.broadcastChanges();
                            serverPlayer.initMenu(newContainer);
                            newContainer.updateDisplay(reference, true);
                        }

                        AtlantisUI.LOCKED.remove(serverPlayer.getUUID());
                    });
                } else {
                    AtlantisUI.FUTURE_OPEN.put(serverPlayer.getUUID(), Page.CLOSURE_PAGE);
                }
            }
        }
        return result;
    }

    /**
     * Closes the UI page that is currently open.
     *
     * @param player the user to display the page to
     * @return whether this action was successful
     */
    public static boolean close(PlayerReference player) {
        return AtlantisUI.open(player, null);
    }

    /**
     * Closes the UI page that is currently open.
     *
     * @param player the user to display the page to
     * @return whether this action was successful
     */
    public static boolean close(Player player) {
        return AtlantisUI.open(player, null);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @return whether this action succeeded
     */
    public static boolean nextPage(PlayerReference player) {
        return AtlantisUI.changePage(player, 0, 1, true);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @return whether this action succeeded
     */
    public static boolean nextPage(Player player) {
        return AtlantisUI.changePage(player, 0, 1, true);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @return whether this action succeeded
     */
    public static boolean prevPage(PlayerReference player) {
        return AtlantisUI.changePage(player, 0, -1, true);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @return whether this action succeeded
     */
    public static boolean prevPage(Player player) {
        return AtlantisUI.changePage(player, 0, -1, true);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean nextPage(PlayerReference player, boolean looped) {
        return AtlantisUI.nextPage(player.entityDirect(), 0, looped);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean nextPage(Player player, boolean looped) {
        return AtlantisUI.changePage(player, 0, 1, looped);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean prevPage(PlayerReference player, boolean looped) {
        return AtlantisUI.prevPage(player.entityDirect(), 0, looped);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean prevPage(Player player, boolean looped) {
        return AtlantisUI.changePage(player, 0, -1, looped);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @return whether this action succeeded
     */
    public static boolean nextPage(PlayerReference player, int groupID) {
        return AtlantisUI.changePage(player, groupID, 1, true);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @return whether this action succeeded
     */
    public static boolean nextPage(Player player, int groupID) {
        return AtlantisUI.changePage(player, groupID, 1, true);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @return whether this action succeeded
     */
    public static boolean prevPage(PlayerReference player, int groupID) {
        return AtlantisUI.changePage(player, groupID, -1, true);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @return whether this action succeeded
     */
    public static boolean prevPage(Player player, int groupID) {
        return AtlantisUI.changePage(player, groupID, -1, true);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean nextPage(PlayerReference player, int groupID, boolean looped) {
        return AtlantisUI.nextPage(player.entityDirect(), groupID, looped);
    }

    /**
     * Attempts to go to the next page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupId the pagination group to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean nextPage(Player player, int groupId, boolean looped) {
        return AtlantisUI.changePage(player, groupId, 1, looped);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupId the pagination group to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean prevPage(PlayerReference player, int groupId, boolean looped) {
        return AtlantisUI.prevPage(player.entityDirect(), groupId, looped);
    }

    /**
     * Attempts to go to the previous page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupId the pagination group to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean prevPage(Player player, int groupId, boolean looped) {
        return AtlantisUI.changePage(player, groupId, -1, looped);
    }

    /**
     * Attempts to change the page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @param change the number of pages to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean changePage(PlayerReference player, int groupID, int change, boolean looped) {
        return AtlantisUI.changePage(player.entityDirect(), groupID, change, looped);
    }

    /**
     * Attempts to change the page with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to change
     * @param change the number of pages to change
     * @param looped whether to try to loop around
     * @return whether this action succeeded
     */
    public static boolean changePage(Player player, int groupID, int change, boolean looped) {
        var container = AtlantisUI.currentContainer(player);
        if (container == null) {
            return false;
        }

        var group = container.atlantisInventory().group(groupID);
        var nextPage = looped ? group.resolvePage(group.currentPageId() + change) : group.currentPageId() + change;
        if (nextPage >= 0 && nextPage < group.pageCount()) {
            group.setCurrentPage(nextPage);
            container.updateDisplay(true);
            return true;
        }
        return false;
    }

    /**
     * Attempts to set the page directly with the pagination system.
     *
     * @param player the user with a page open
     * @param pageID the page to switch to
     * @return whether this action succeeded
     */
    public static boolean setPage(PlayerReference player, int pageID) {
        return AtlantisUI.setPage(player.entityDirect(), 0, pageID);
    }

    /**
     * Attempts to set the page directly with the pagination system.
     *
     * @param player the user with a page open
     * @param pageID the page to switch to
     * @return whether this action succeeded
     */
    public static boolean setPage(Player player, int pageID) {
        return AtlantisUI.setPage(player, 0, pageID);
    }

    /**
     * Attempts to set the page directly with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to switch in
     * @param pageID the page to switch to
     * @return whether this action succeeded
     */
    public static boolean setPage(PlayerReference player, int groupID, int pageID) {
        return AtlantisUI.setPage(player.entityDirect(), groupID, pageID);
    }

    /**
     * Attempts to set the page directly with the pagination system.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to switch in
     * @param pageID the page to switch to
     * @return whether this action succeeded
     */
    public static boolean setPage(Player player, int groupID, int pageID) {
        var container = AtlantisUI.currentContainer(player);
        if (container == null) {
            return false;
        }

        var group = container.atlantisInventory().group(groupID);
        var nextPage = group.resolvePage(pageID);
        if (nextPage >= 0 && nextPage < group.pageCount()) {
            group.setCurrentPage(nextPage);
            container.updateDisplay(true);
            return true;
        }
        return false;
    }

    /**
     * Returns whether a next page exists.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return whether a next page exists
     */
    public static boolean hasNextPage(PlayerReference player, int groupID) {
        return AtlantisUI.hasNextPage(player.entityDirect(), groupID);
    }

    /**
     * Returns whether a next page exists.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return whether a next page exists
     */
    public static boolean hasNextPage(Player player, int groupID) {
        var container = AtlantisUI.currentContainer(player);
        if (container == null) {
            return false;
        }

        var group = container.atlantisInventory().group(groupID);
        var nextPage = group.currentPageId() + 1;
        return nextPage < group.pageCount();
    }

    /**
     * Returns whether a previous page exists.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return whether a previous page exists
     */
    public static boolean hasPrevPage(PlayerReference player, int groupID) {
        return AtlantisUI.hasPrevPage(player.entityDirect(), groupID);
    }

    /**
     * Returns whether a previous page exists.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return whether a previous page exists
     */
    public static boolean hasPrevPage(Player player, int groupID) {
        var container = AtlantisUI.currentContainer(player);
        if (container == null) {
            return false;
        }

        return container.atlantisInventory().group(groupID).currentPageId() > 0;
    }

    /**
     * Returns the current page of a player.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return their current page
     */
    public static int currentPage(PlayerReference player, int groupID) {
        return AtlantisUI.currentPage(player.entityDirect(), groupID);
    }

    /**
     * Returns the current page of a player.
     *
     * @param player the user with a page open
     * @param groupID the pagination group to check
     * @return their current page
     */
    public static int currentPage(Player player, int groupID) {
        var container = AtlantisUI.currentContainer(player);
        if (container == null) {
            return -1;
        }

        return container.atlantisInventory().group(groupID).currentPageId();
    }

    /**
     * Returns the current AtlantisUI container of a player, if present.
     *
     * @param player the user with a page open
     * @return current AtlantisUI container
     */
    public static @Nullable AtlantisContainerMenuInternal currentContainer(PlayerReference player) {
        return AtlantisUI.currentContainer(player.entityDirect());
    }

    /**
     * Returns the current AtlantisUI container of a player, if present.
     *
     * @param player the user with a page open
     * @return current AtlantisUI container
     */
    public static @Nullable AtlantisContainerMenuInternal currentContainer(Player player) {
        if (player instanceof ServerPlayer && player.containerMenu instanceof AtlantisContainerMenuInternal menu) {
            return menu;
        }
        return null;
    }

    /**
     * Attempts to update a property of a page, for example a progress bar.
     *
     * @param player the user who sees the page
     * @param element the ID of the property to update
     * @param value the value to set the property to
     */
    public static void setProperty(PlayerReference player, int element, int value) {
        AtlantisUI.setProperty(player.entityDirect(), element, value);
    }

    /**
     * Attempts to update a property of a page, for example a progress bar.
     *
     * @param player the user who sees the page
     * @param element the ID of the property to update
     * @param value the value to set the property to
     */
    public static void setProperty(Player player, int element, int value) {
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu instanceof AtlantisContainerMenuInternal) {
            serverPlayer.connection.send(new ClientboundContainerSetDataPacket(serverPlayer.getContainerCounter(), element, value));
        }
    }

    /**
     * Creates an anonymous page.
     *
     * @param options the options of the page
     * @param buttons the buttons to add to the page
     * @return the page
     */
    public static Page createPage(final PageOptions options, final Button... buttons) {

        return new Page() {
            @Override
            public PageOptions options(PlayerReference player) {
                return options;
            }

            @Override
            public void addButtons(PlayerReference player, ButtonCollector buttonsOut) {
                buttonsOut.collect(Lists.newArrayList(buttons));
            }
        };
    }
}