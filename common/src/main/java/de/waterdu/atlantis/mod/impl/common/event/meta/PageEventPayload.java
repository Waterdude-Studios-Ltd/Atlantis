package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.Click;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.entity.PlayerReference;

import java.util.Set;

/**
 * Holds the player and the page of a UI page event.
 */
public abstract class PageEventPayload implements EventPayload, Cancellable {

    private final PlayerReference player;
    private final Page page;

    private boolean cancelled = false;

    /**
     * Creates a page payload.
     *
     * @param player the player who sees the page
     * @param page the page
     */
    protected PageEventPayload(PlayerReference player, Page page) {
        this.player = player;
        this.page = page;
    }

    /**
     * @return the player who sees the page
     */
    public PlayerReference player() {
        return this.player;
    }

    /**
     * @return the page of the event
     */
    public Page page() {
        return this.page;
    }

    /**
     * Marks this payload as cancelled.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Checks if a listener cancelled this payload.
     *
     * @return {@code true} if the payload is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    /**
     * Fires before a page opens. Listeners can change its buttons and options. If a listener cancels it, the page does
     * not open.
     */
    public static class Construct extends PageEventPayload {

        private Buttons buttons;
        private PageOptions options;

        /**
         * Creates a payload for {@link UEvent#CONSTRUCT_PAGE}.
         *
         * @param player the player who sees the page
         * @param page the page that opens
         * @param buttons the buttons of the page
         * @param options the options of the page
         */
        public Construct(PlayerReference player, Page page, Buttons buttons, PageOptions options) {
            super(player, page);
            this.buttons = buttons;
            this.options = options;
        }

        /**
         * @return {@link UEvent#CONSTRUCT_PAGE}
         */
        @Override
        public Key<?> type() {
            return UEvent.CONSTRUCT_PAGE;
        }

        /**
         * @return the buttons of the page
         */
        public Buttons buttons() {
            return this.buttons;
        }

        /**
         * Replaces the buttons of the page.
         *
         * @param buttons the new buttons
         */
        public void setButtons(Buttons buttons) {
            this.buttons = buttons;
        }

        /**
         * Puts the button into {@link #buttons()} at its own index, group and page. It replaces a button that is
         * already there.
         *
         * @param button the button to add
         */
        public void addButton(Button button) {
            this.buttons.setButton(button.index(), button.group(), button.page(), button);
        }

        /**
         * @return the options of the page
         */
        public PageOptions options() {
            return this.options;
        }

        /**
         * Replaces the options of the page.
         *
         * @param options the new options
         */
        public void setOptions(PageOptions options) {
            this.options = options;
        }
    }

    /**
     * Fires when the decorations of a page are collected. If a listener cancels it, the page gets no decorations.
     */
    public static class Decorate extends PageEventPayload {

        private final Decorations instance;
        private Set<Button> decorations;

        /**
         * Creates a payload for {@link UEvent#DECORATE_PAGE}.
         *
         * @param player the player who sees the page
         * @param page the page to decorate
         * @param instance the decorations of the page type
         * @param decorations the decoration buttons to add
         */
        public Decorate(PlayerReference player, Page page, Decorations instance, Set<Button> decorations) {
            super(player, page);
            this.instance = instance;
            this.decorations = decorations;
        }

        /**
         * @return {@link UEvent#DECORATE_PAGE}
         */
        @Override
        public Key<?> type() {
            return UEvent.DECORATE_PAGE;
        }

        /**
         * @return the decorations of the page type
         */
        public Decorations instance() {
            return this.instance;
        }

        /**
         * The decorations of the page. By default, this is the shared set of {@link #instance()}. Changes to that set
         * stay for every later page of this type. To change only this page, give a new set to
         * {@link #setDecorations(Set)}.
         */
        public Set<Button> decorations() {
            return this.decorations;
        }

        /**
         * Adds the button to {@link #decorations()}. If that is still the shared set, the button stays for every later
         * page of this type.
         */
        public void addDecoration(Button button) {
            this.decorations.add(button);
        }

        /**
         * Replaces the decorations of this page only.
         *
         * @param decorations the new decorations
         */
        public void setDecorations(Set<Button> decorations) {
            this.decorations = decorations;
        }
    }

    /**
     * Fires when a player closes a page. If a listener cancels it, the page opens again.
     */
    public static class Destruct extends PageEventPayload {

        /**
         * Creates a payload for {@link UEvent#DESTRUCT_PAGE}.
         *
         * @param player the player who closes the page
         * @param page the page that closes
         */
        public Destruct(PlayerReference player, Page page) {
            super(player, page);
        }

        /**
         * @return {@link UEvent#DESTRUCT_PAGE}
         */
        @Override
        public Key<?> type() {
            return UEvent.DESTRUCT_PAGE;
        }
    }

    /**
     * Fires when a player presses a button on a page. If a listener cancels it, the button does not run its actions.
     */
    public static class Press extends PageEventPayload {

        private final Button button;
        private final Click click;

        /**
         * Creates a payload for {@link UEvent#PRESS_PAGE}.
         *
         * @param player the player who presses the button
         * @param page the page of the button
         * @param button the pressed button
         * @param click the type of click
         */
        public Press(PlayerReference player, Page page, Button button, Click click) {
            super(player, page);
            this.button = button;
            this.click = click;
        }

        /**
         * @return {@link UEvent#PRESS_PAGE}
         */
        @Override
        public Key<?> type() {
            return UEvent.PRESS_PAGE;
        }

        /**
         * @return the pressed button
         */
        public Button button() {
            return this.button;
        }

        /**
         * @return the type of click
         */
        public Click click() {
            return this.click;
        }
    }
}
