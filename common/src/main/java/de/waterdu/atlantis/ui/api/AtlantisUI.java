package de.waterdu.atlantis.ui.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.ext.player.PlayerExt;
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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h1>AtlantisUI</h1>
 * Chest UIs made in Forge directly.
 *
 * @author      Sam Plummer sam@waterdu.de
 * @version     1.0
 * @since       02/03/2019
 */
public final class AtlantisUI {

    public static final Map<UUID, Page> FUTURE_OPEN = new ConcurrentHashMap<>();
    public static final Set<UUID> LOCKED = Sets.newConcurrentHashSet();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private AtlantisUI() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {
        if (!AtlantisUI.REGISTERED.getAndSet(true)) {
            InventoryListener.register();
        }
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @return                  Whether this action was successful.
     */
    public static boolean open(PlayerReference player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @return                  Whether this action was successful.
     */
    public static boolean open(Player player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null);
    }

    /**
     * Updates a UI page. Retains page and group positioning while updating button contents.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @return                  Whether this action was successful.
     */
    public static boolean update(PlayerReference player, @Nullable Page page) {
        return AtlantisUI.open(player, page);
    }

    /**
     * Updates a UI page. Retains page and group positioning while updating button contents.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @return                  Whether this action was successful.
     */
    public static boolean update(Player player, @Nullable Page page) {
        return AtlantisUI.open(player, page, null, false, true);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * Can provide a {@link Buttons} to override what is being shown on this page.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @param       buttons     A {@link Buttons} object to apply to this page.
     * @return                  Whether this action was successful.
     */
    public static boolean open(PlayerReference player, @Nullable Page page, @Nullable Buttons buttons) {
        return AtlantisUI.open(player, page, buttons, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * Can provide a {@link Buttons} to override what is being shown on this page.
     *
     * @param       player      The user to display the page to.
     * @param       page        The page to display. null closes any currently open page.
     * @param       buttons     A {@link Buttons} object to apply to this page.
     * @return                  Whether this action was successful.
     */
    public static boolean open(Player player, @Nullable Page page, @Nullable Buttons buttons) {
        return AtlantisUI.open(player, page, buttons, false, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * Can make the page wait for the player to close their current page before showing itself.
     *
     * @param       player          The user to display the page to.
     * @param       page            The page to display. null closes any currently open page.
     * @param       waitForScreen   Whether to wait for the player to close their current page.
     * @return                      Whether this action was successful.
     */
    public static boolean open(PlayerReference player, @Nullable Page page, boolean waitForScreen) {
        return AtlantisUI.open(player, page, null, waitForScreen);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     * Can make the page wait for the player to close their current page before showing itself.
     *
     * @param       player          The user to display the page to.
     * @param       page            The page to display. null closes any currently open page.
     * @param       waitForScreen   Whether to wait for the player to close their current page.
     * @return                      Whether this action was successful.
     */
    public static boolean open(Player player, @Nullable Page page, boolean waitForScreen) {
        return AtlantisUI.open(player, page, null, waitForScreen, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param       player          The user to display the page to.
     * @param       page            The page to display. null closes any currently open page.
     * @param       buttons         A {@link Buttons} object to apply to this page.
     * @param       waitForScreen   Whether to wait for the player to close their current page.
     * @return                      Whether this action was successful.
     */
    public static boolean open(PlayerReference player, @Nullable Page page, @Nullable Buttons buttons,
                               boolean waitForScreen) {
        return AtlantisUI.open(player.entityDirect(), page, buttons, waitForScreen);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param       player          The user to display the page to.
     * @param       page            The page to display. null closes any currently open page.
     * @param       buttons         A {@link Buttons} object to apply to this page.
     * @param       waitForScreen   Whether to wait for the player to close their current page.
     * @return                      Whether this action was successful.
     */
    public static boolean open(Player player, @Nullable Page page, @Nullable Buttons buttons,
                               boolean waitForScreen) {
        return AtlantisUI.open(player, page, buttons, waitForScreen, false);
    }

    /**
     * Opens a UI page. All UI pages must be instances of a class implementing {@link Page}.
     *
     * @param       player          The user to display the page to.
     * @param       page            The page to display. null closes any currently open page.
     * @param       buttons         A {@link Buttons} object to apply to this page.
     * @param       waitForScreen   Whether to wait for the player to close their current page.
     * @param       persistence     Whether to copy current page placements and update task states over.
     * @return                      Whether this action was successful.
     */
    public static boolean open(Player player, @Nullable Page page, @Nullable Buttons buttons,
                               boolean waitForScreen, boolean persistence) {
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
                        var ff = options.formFactor();
                        var slots = ff.hasRows() ? options.rows() * 9 : ff.slots();
                        var old = AtlantisUI.currentContainer(player).map(c -> c.getAquaUIInventory().buttons()).orElse(null);
                        var i = buttons != null ?
                                    new AtlantisInventoryInternal(page, serverPlayer, buttons, slots) :
                                    new AtlantisInventoryInternal(page, serverPlayer, slots);
                        if (old != null && persistence) {
                            i.buttons().persist(old);
                        }
                        var event = Mods.modLoader().event(UEvent.CONSTRUCT_PAGE).emit(new PageEventPayload.Construct(reference, page, i.buttons(), options));
                        if (!event.cancelled()) {
                            i.setButtons(event.buttons());
                            var c = new AtlantisContainerMenuInternal(i, serverPlayer);
                            i.onUpdate(serverPlayer, c);
                            serverPlayer.containerMenu = c;
                            serverPlayer.setContainerCounter(1);
                            serverPlayer.connection.send(new ClientboundOpenScreenPacket(serverPlayer.getContainerCounter(), ff.getType(options), i.displayName()));
                            serverPlayer.containerMenu.broadcastChanges();
                            ((PlayerExt) serverPlayer).initMenu(c);
                            c.updateDisplay(reference, true);
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
     * Closes a currently open UI page.
     *
     * @param       player          The user to display the page to.
     * @return                      Whether this action was successful.
     */
    public static boolean close(PlayerReference player) {
        return AtlantisUI.open(player, null);
    }

    /**
     * Closes a currently open UI page.
     *
     * @param       player          The user to display the page to.
     * @return                      Whether this action was successful.
     */
    public static boolean close(Player player) {
        return AtlantisUI.open(player, null);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(PlayerReference player) {
        return AtlantisUI.changePage(player, 0, 1, true);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(Player player) {
        return AtlantisUI.changePage(player, 0, 1, true);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(PlayerReference player) {
        return AtlantisUI.changePage(player, 0, -1, true);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(Player player) {
        return AtlantisUI.changePage(player, 0, -1, true);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(PlayerReference player, boolean looped) {
        return AtlantisUI.nextPage(player.entityDirect(), 0, looped);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(Player player, boolean looped) {
        return AtlantisUI.changePage(player, 0, 1, looped);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(PlayerReference player, boolean looped) {
        return AtlantisUI.prevPage(player.entityDirect(), 0, looped);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(Player player, boolean looped) {
        return AtlantisUI.changePage(player, 0, -1, looped);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(PlayerReference player, int groupID) {
        return AtlantisUI.changePage(player, groupID, 1, true);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(Player player, int groupID) {
        return AtlantisUI.changePage(player, groupID, 1, true);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(PlayerReference player, int groupID) {
        return AtlantisUI.changePage(player, groupID, -1, true);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(Player player, int groupID) {
        return AtlantisUI.changePage(player, groupID, -1, true);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(PlayerReference player, int groupID, boolean looped) {
        return AtlantisUI.nextPage(player.entityDirect(), groupID, looped);
    }

    /**
     * Attempts to go to the next page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean nextPage(Player player, int groupID, boolean looped) {
        return AtlantisUI.changePage(player, groupID, 1, looped);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(PlayerReference player, int groupID, boolean looped) {
        return AtlantisUI.prevPage(player.entityDirect(), groupID, looped);
    }

    /**
     * Attempts to go to the previous page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean prevPage(Player player, int groupID, boolean looped) {
        return AtlantisUI.changePage(player, groupID, -1, looped);
    }

    /**
     * Attempts to change page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       change          The number of pages to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean changePage(PlayerReference player, int groupID, int change, boolean looped) {
        return AtlantisUI.changePage(player.entityDirect(), groupID, change, looped);
    }

    /**
     * Attempts to change page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to change.
     * @param       change          The number of pages to change.
     * @param       looped          Whether to attempt to loop around or not.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean changePage(Player player, int groupID, int change, boolean looped) {
        var optional = AtlantisUI.currentContainer(player);
        if (optional.isPresent()) {
            var container = optional.get();
            var group = container.getAquaUIInventory().group(groupID);
            var nextPage = looped ? group.resolvePage(group.currentPageID() + change) : group.currentPageID() + change;
            if (nextPage >= 0 && nextPage < group.pageCount()) {
                group.setCurrentPage(nextPage);
                container.updateDisplay(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to directly set page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       pageID          The page to switch to.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean setPage(PlayerReference player, int pageID) {
        return AtlantisUI.setPage(player.entityDirect(), 0, pageID);
    }

    /**
     * Attempts to directly set page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       pageID          The page to switch to.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean setPage(Player player, int pageID) {
        return AtlantisUI.setPage(player, 0, pageID);
    }

    /**
     * Attempts to directly set page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to switch in.
     * @param       pageID          The page to switch to.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean setPage(PlayerReference player, int groupID, int pageID) {
        return AtlantisUI.setPage(player.entityDirect(), groupID, pageID);
    }

    /**
     * Attempts to directly set page via the pagination system.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to switch in.
     * @param       pageID          The page to switch to.
     * @return                      Whether this action succeeded or not.
     */
    public static boolean setPage(Player player, int groupID, int pageID) {
        var optional = AtlantisUI.currentContainer(player);
        if (optional.isPresent()) {
            var container = optional.get();
            var group = container.getAquaUIInventory().group(groupID);
            var nextPage = group.resolvePage(pageID);
            if (nextPage >= 0 && nextPage < group.pageCount()) {
                group.setCurrentPage(nextPage);
                container.updateDisplay(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a next page is present or not.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Whether a next page exists.
     */
    public static boolean hasNextPage(PlayerReference player, int groupID) {
        return AtlantisUI.hasNextPage(player.entityDirect(), groupID);
    }

    /**
     * Returns whether a next page is present or not.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Whether a next page exists.
     */
    public static boolean hasNextPage(Player player, int groupID) {
        var optional = AtlantisUI.currentContainer(player);
        if (optional.isPresent()) {
            var group = optional.get().getAquaUIInventory().group(groupID);
            var nextPage = group.currentPageID() + 1;
            return nextPage < group.pageCount();
        }
        return false;
    }

    /**
     * Returns whether a prev page is present or not.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Whether a prev page exists.
     */
    public static boolean hasPrevPage(PlayerReference player, int groupID) {
        return AtlantisUI.hasPrevPage(player.entityDirect(), groupID);
    }

    /**
     * Returns whether a prev page is present or not.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Whether a prev page exists.
     */
    public static boolean hasPrevPage(Player player, int groupID) {
        var optional = AtlantisUI.currentContainer(player);
        return optional.filter(atlantisContainerMenuInternal -> (atlantisContainerMenuInternal.getAquaUIInventory().group(groupID).currentPageID() - 1) > 0).isPresent();
    }

    /**
     * Returns the current page of a player.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Their current page.
     */
    public static int currentPage(PlayerReference player, int groupID) {
        return AtlantisUI.currentPage(player.entityDirect(), groupID);
    }

    /**
     * Returns the current page of a player.
     *
     * @param       player          The user with a page open.
     * @param       groupID         The pagination group to check.
     * @return                      Their current page.
     */
    public static int currentPage(Player player, int groupID) {
        return AtlantisUI.currentContainer(player).map(atlantisContainerMenuInternal -> atlantisContainerMenuInternal.getAquaUIInventory().group(groupID).currentPageID()).orElse(-1);
    }

    /**
     * Returns the current AtlantisUI container of a player, if present.
     *
     * @param       player          The user with a page open.
     * @return                      Optional with their current AtlantisUI container.
     */
    public static Optional<AtlantisContainerMenuInternal> currentContainer(PlayerReference player) {
        return AtlantisUI.currentContainer(player.entityDirect());
    }

    /**
     * Returns the current AtlantisUI container of a player, if present.
     *
     * @param       player          The user with a page open.
     * @return                      Optional with their current AtlantisUI container.
     */
    public static Optional<AtlantisContainerMenuInternal> currentContainer(Player player) {
        if (player instanceof ServerPlayer && player.containerMenu instanceof AtlantisContainerMenuInternal menu) {
            return Optional.of(menu);
        }
        return Optional.empty();
    }

    /**
     * Attempts to update a property of a page. This is for things such as progress bars, etc.
     *
     * @param       player          The user who is being shown a page.
     * @param       element         The ID of the property to update.
     * @param       value           The value to set the property to.
     */
    public static void setProperty(PlayerReference player, int element, int value) {
        AtlantisUI.setProperty(player.entityDirect(), element, value);
    }

    /**
     * Attempts to update a property of a page. This is for things such as progress bars, etc.
     *
     * @param       player          The user who is being shown a page.
     * @param       element         The ID of the property to update.
     * @param       value           The value to set the property to.
     */
    public static void setProperty(Player player, int element, int value) {
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu instanceof AtlantisContainerMenuInternal) {
            serverPlayer.connection.send(new ClientboundContainerSetDataPacket(serverPlayer.getContainerCounter(), element, value));
        }
    }

    /**
     * Create an anonymous page.
     *
     * @param       options         The options of the page.
     * @param       buttons         The buttons to be added to the page.
     * @return The page.
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