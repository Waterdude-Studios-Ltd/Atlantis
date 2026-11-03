package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Describes a click of a player on a page slot. It also has shortcuts to open, close and change pages.
 *
 * @param player the player who clicked
 * @param buttons the buttons of the open page
 * @param index the slot index that was clicked
 * @param page the current page of the pagination group
 * @param group the pagination group
 * @param button the mouse button
 * @param style the click style
 * @param rate whether the click is a single or a double click
 * @param time the time of the click, in milliseconds since the epoch
 */
public record Click(
        PlayerReference player,
        Buttons buttons,
        int index,
        int page,
        int group,
        Button button,
        Style style,
        Rate rate,
        long time
) {

    public static final Consumer<Click> PREVIOUS_PAGE = Click::previous;
    public static final Consumer<Click> NEXT_PAGE = Click::next;
    public static final Consumer<Click> CLOSE_PAGE = Click::closePage;

    /**
     * Copies a click with a different mouse button and style. The copy is a single click at the current time.
     *
     * @param click the click to copy
     * @param button the mouse button
     * @param style the click style
     */
    public Click(Click click, Button button, Style style) {
        this(click.player, click.buttons, click.index, click.page, click.group, button, style, Rate.SINGLE, System.currentTimeMillis());
    }

    /**
     * Creates a single click at the current time.
     *
     * @param player the player who clicked
     * @param buttons the buttons of the open page
     * @param index the slot index that was clicked
     * @param page the current page of the pagination group
     * @param group the pagination group
     * @param button the mouse button
     * @param style the click style
     */
    public Click(PlayerReference player, Buttons buttons, int index, int page, int group, Button button, Style style) {
        this(player, buttons, index, page, group, button, style, Rate.SINGLE, System.currentTimeMillis());
    }

    /**
     * Copies a click with a different rate and time.
     *
     * @param click the click to copy
     * @param rate whether the click is a single or a double click
     * @param time the time of the click, in milliseconds since the epoch
     */
    public Click(Click click, Rate rate, long time) {
        this(click.player, click.buttons, click.index, click.page, click.group, click.button, click.style, rate, time);
    }

    /**
     * @param previous the previous click of the player. Can be null. The result is a {@link Rate#DOUBLE} click if
     *     {@code previous} is a single click with the same mouse button and style within the double-click delay.
     */
    public static Click make(
            PlayerReference player, Buttons buttons, int index, int page, int group, Button button, Style style,
            @Nullable Click previous
    ) {
        var time = System.currentTimeMillis();
        if (previous != null && doubleClickTest(button, style, Rate.SINGLE, time, previous.button, previous.style, previous.rate, previous.time)) {
            return new Click(player, buttons, index, page, group, button, style, Rate.DOUBLE, time);
        }
        return new Click(player, buttons, index, page, group, button, style, Rate.SINGLE, time);
    }

    /**
     * Checks if two clicks make a double click.
     *
     * @param click the new click
     * @param previous the previous click
     * @return {@code true} if {@link #isSameButton(Click)} is true and the clicks are within the double-click delay
     */
    private static boolean doubleClickTest(Click click, Click previous) {
        return click.isSameButton(previous) && click.time - previous.time <= Settings.get().getDoubleClickDelay();
    }

    /**
     * Checks if two clicks make a double click.
     *
     * @param button the mouse button of the new click
     * @param style the style of the new click
     * @param rate the rate of the new click
     * @param time the time of the new click, in milliseconds
     * @param previousButton the mouse button of the previous click
     * @param previousStyle the style of the previous click
     * @param previousRate the rate of the previous click
     * @param previousTime the time of the previous click, in milliseconds
     * @return {@code true} if the button, style and rate are the same and the clicks are within the double-click
     *     delay
     */
    private static boolean doubleClickTest(
            Button button, Style style, Rate rate, long time,
            Button previousButton, Style previousStyle, Rate previousRate, long previousTime
    ) {
        return button == previousButton &&
                style == previousStyle &&
                rate == previousRate &&
                time - previousTime <= Settings.get().getDoubleClickDelay();
    }

    /**
     * Checks if the other click has the same mouse button, style and rate. The slot index is not compared.
     *
     * @param click the click to compare
     * @return {@code true} if the mouse button, style and rate are the same
     */
    public boolean isSameButton(Click click) {
        return this.button == click.button && this.style == click.style && this.rate == click.rate;
    }

    /**
     * Makes this click a double click if it follows the previous click within the double-click delay.
     *
     * @param previous the previous click of the player
     * @return a {@link Rate#DOUBLE} copy of this click, or this click
     */
    public Click tryDoubleClick(Click previous) {
        if (doubleClickTest(this, previous)) {
            return new Click(this, Rate.DOUBLE, this.time);
        }
        return this;
    }

    /**
     * Opens a page for the player at once.
     *
     * @param page the page to open
     */
    public void open(Page page) {
        this.open(page, false);
    }

    /**
     * Opens a page for the player.
     *
     * @param page the page to open
     * @param waitForScreen whether to wait until the player closes their current page
     */
    public void open(Page page, boolean waitForScreen) {
        this.open(page, waitForScreen, null);
    }

    /**
     * Opens a page for the player at once, with the given buttons.
     *
     * @param page the page to open
     * @param buttons the {@link Buttons} to apply to the page
     */
    public void open(Page page, Buttons buttons) {
        this.open(page, false, buttons);
    }

    /**
     * Opens a page for the player with the given buttons. See {@link AtlantisUI#open}.
     *
     * @param page the page to open
     * @param waitForScreen whether to wait until the player closes their current page
     * @param buttons the {@link Buttons} to apply to the page, or {@code null}
     */
    public void open(Page page, boolean waitForScreen, Buttons buttons) {
        AtlantisUI.open(this.player, page, buttons, waitForScreen);
    }

    /**
     * Keeps the current page positions and update task states. See {@link AtlantisUI#update}.
     */
    public void updatePage(Page page) {
        AtlantisUI.open(this.player.entityDirect(), page, null, false, true);
    }

    /**
     * Closes the page of the player.
     */
    public void closePage() {
        AtlantisUI.close(this.player);
    }

    /**
     * Goes to the next page of the pagination group of this click. It loops around after the last page.
     */
    public void next() {
        this.next(true);
    }

    /**
     * Goes to the next page of the pagination group of this click.
     *
     * @param looped whether to loop around after the last page
     */
    public void next(boolean looped) {
        this.next(this.group, looped);
    }

    /**
     * Goes to the previous page of the pagination group of this click. It loops around before the first page.
     */
    public void previous() {
        this.previous(true);
    }

    /**
     * Goes to the previous page of the pagination group of this click.
     *
     * @param looped whether to loop around before the first page
     */
    public void previous(boolean looped) {
        this.previous(this.group, looped);
    }

    /**
     * Goes to the next page of a pagination group. It loops around after the last page.
     *
     * @param group the pagination group to change
     */
    public void next(int group) {
        this.next(group, true);
    }

    /**
     * Goes to the next page of a pagination group.
     *
     * @param group the pagination group to change
     * @param looped whether to loop around after the last page
     */
    public void next(int group, boolean looped) {
        AtlantisUI.nextPage(this.player, group, looped);
    }

    /**
     * Goes to the previous page of a pagination group. It loops around before the first page.
     *
     * @param group the pagination group to change
     */
    public void previous(int group) {
        this.previous(group, true);
    }

    /**
     * Goes to the previous page of a pagination group.
     *
     * @param group the pagination group to change
     * @param looped whether to loop around before the first page
     */
    public void previous(int group, boolean looped) {
        AtlantisUI.prevPage(this.player, group, looped);
    }

    /**
     * Sets the page of the pagination group of this click.
     *
     * @param page the page to go to
     */
    public void setPage(int page) {
        this.setPage(this.group, page);
    }

    /**
     * Sets the page of a pagination group.
     *
     * @param group the pagination group to change
     * @param page the page to go to
     */
    public void setPage(int group, int page) {
        AtlantisUI.setPage(this.player, group, page);
    }

    /**
     * Changes the page of a pagination group by a number of pages. It loops around at the ends.
     *
     * @param group the pagination group to change
     * @param change the number of pages to change
     */
    public void change(int group, int change) {
        this.change(group, change, true);
    }

    /**
     * Changes the page of a pagination group by a number of pages.
     *
     * @param group the pagination group to change
     * @param change the number of pages to change
     * @param looped whether to loop around at the ends
     */
    public void change(int group, int change, boolean looped) {
        AtlantisUI.changePage(this.player, group, change, looped);
    }

    /**
     * Updates a property of the open page, for example a progress bar.
     *
     * @param property the ID of the property to update
     * @param value the value to set the property to
     */
    public void setProperty(int property, int value) {
        AtlantisUI.setProperty(this.player, property, value);
    }

    /**
     * @return the server player who clicked
     */
    public ServerPlayer entity() {
        return this.player.entityDirect();
    }

    /**
     * Checks if the click is on a slot.
     *
     * @param index the slot index
     * @return {@code true} if the click is on the slot
     */
    public boolean matches(int index) {
        return this.index == index;
    }

    /**
     * Checks if the click is on a slot with a mouse button.
     *
     * @param index the slot index
     * @param button the mouse button
     * @return {@code true} if the slot and the mouse button are the same
     */
    public boolean matches(int index, Button button) {
        return this.matches(index) && this.button == button;
    }

    /**
     * Checks if the click is on a slot with a mouse button and style.
     *
     * @param index the slot index
     * @param button the mouse button
     * @param style the click style
     * @return {@code true} if the slot, the mouse button and the style are the same
     */
    public boolean matches(int index, Button button, Style style) {
        return this.matches(index, button) && this.style == style;
    }

    /**
     * Identifies the mouse button of a click.
     */
    public enum Button {

        LEFT,
        RIGHT,
        MIDDLE,
        UNKNOWN;

        private static final Button[] VALUES_AND_NULL = new Button[]{LEFT, RIGHT, MIDDLE, UNKNOWN, null};

        /**
         * Returns a shared array. Do not change it. The last element is null.
         */
        public static Button[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

        /**
         * Gets the mouse button for a button number.
         *
         * @param button the button number. 0 is left, 1 is right and 2 is middle.
         * @return the mouse button, or {@link #UNKNOWN} for other numbers
         */
        public static Button get(int button) {
            if (button >= 0 && button <= 2) {
                return Button.values()[button];
            }
            return Button.UNKNOWN;
        }

        /**
         * @return {@code true} if this is {@link #LEFT}
         */
        public boolean isLeft() {
            return this == LEFT;
        }

        /**
         * @return {@code true} if this is {@link #RIGHT}
         */
        public boolean isRight() {
            return this == RIGHT;
        }

        /**
         * @return {@code true} if this is {@link #MIDDLE}
         */
        public boolean isMiddle() {
            return this == MIDDLE;
        }

        /**
         * @return {@code true} if this is {@link #UNKNOWN}
         */
        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    /**
     * Identifies the style of a click: normal, shift or drag.
     */
    public enum Style {

        NORMAL,
        SHIFT,
        DRAG,
        UNKNOWN;

        private static final Style[] VALUES_AND_NULL = new Style[]{NORMAL, SHIFT, DRAG, UNKNOWN, null};

        /**
         * Returns a shared array. Do not change it. The last element is null.
         */
        public static Style[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

        /**
         * Gets the style for a Minecraft click type.
         *
         * @param type the click type
         * @return {@link #NORMAL} for {@code PICKUP} and {@code CLONE}, {@link #SHIFT} for {@code QUICK_MOVE},
         *     {@link #DRAG} for {@code QUICK_CRAFT}, or {@link #UNKNOWN} for other types
         */
        public static Style get(ClickType type) {
            if (type == ClickType.CLONE || type == ClickType.PICKUP) {
                return Style.NORMAL;
            } else if (type == ClickType.QUICK_MOVE) {
                return Style.SHIFT;
            } else if (type == ClickType.QUICK_CRAFT) {
                return Style.DRAG;
            }
            return Style.UNKNOWN;
        }

        /**
         * @return {@code true} if this is {@link #NORMAL}
         */
        public boolean isNormal() {
            return this == NORMAL;
        }

        /**
         * @return {@code true} if this is {@link #SHIFT}
         */
        public boolean isShift() {
            return this == SHIFT;
        }

        /**
         * @return {@code true} if this is {@link #DRAG}
         */
        public boolean isDrag() {
            return this == DRAG;
        }

        /**
         * @return {@code true} if this is {@link #UNKNOWN}
         */
        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    /**
     * Identifies a click as a single or a double click.
     */
    public enum Rate {

        SINGLE,
        DOUBLE,
        ;

        private static final Rate[] VALUES_AND_NULL = new Rate[]{SINGLE, DOUBLE, null};

        /**
         * Returns a shared array. Do not change it. The last element is null.
         */
        public static Rate[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

        /**
         * @return {@code true} if this is {@link #SINGLE}
         */
        public boolean isSingle() {
            return this == SINGLE;
        }

        /**
         * @return {@code true} if this is {@link #DOUBLE}
         */
        public boolean isDouble() {
            return this == DOUBLE;
        }
    }
}
