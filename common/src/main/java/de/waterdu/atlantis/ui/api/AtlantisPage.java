package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.util.entity.PlayerReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A {@link Page} made with a {@link Builder} instead of a subclass.
 */
public class AtlantisPage implements Page {

    private final Set<Button> buttons = new HashSet<>();
    private final List<Function<PlayerReference, Button>> buttonFuncs = new ArrayList<>();
    private final Map<Integer, BiConsumer<PlayerReference, Click>> onButtonClickActions = new HashMap<>();

    private boolean allowExit = true;
    private Function<PlayerReference, Boolean> allowExitFunc = null;

    private int rows = 3;
    private Function<PlayerReference, Integer> rowsFunc = null;

    private String displayName = "";
    private Function<PlayerReference, String> displayNameFunc = null;

    private PageOptions options = PageOptions.defaults();

    /**
     * Creates a builder for a new page.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an {@link AtlantisPage}. Each method changes the same page and returns this builder.
     */
    public static class Builder {

        private final AtlantisPage page;

        /**
         * Creates a builder for a new page.
         */
        public Builder() {
            this.page = new AtlantisPage();
        }

        /**
         * Stores a button for the page. The page does not add stored buttons in {@link Page#addButtons}.
         *
         * @param button the button
         * @return this builder
         */
        public Builder addButton(Button button) {
            this.page.buttons.add(button);
            return this;
        }

        /**
         * Stores a function that makes a button for each player. The page does not add stored buttons in
         * {@link Page#addButtons}.
         *
         * @param function makes the button for a player
         * @return this builder
         */
        public Builder addButton(Function<PlayerReference, Button> function) {
            this.page.buttonFuncs.add(function);
            return this;
        }

        /**
         * @param index the slot index of the button
         * @param consumer runs on the server thread
         */
        public Builder addButtonAction(int index, BiConsumer<PlayerReference, Click> consumer) {
            this.page.onButtonClickActions.put(index, consumer);
            return this;
        }

        /**
         * @param allowExit false opens the page again when the player closes it
         */
        public Builder allowExit(boolean allowExit) {
            this.page.allowExit = allowExit;
            return this;
        }

        /**
         * Takes priority over {@link #allowExit(boolean)}.
         */
        public Builder allowExit(Function<PlayerReference, Boolean> function) {
            this.page.allowExitFunc = function;
            return this;
        }

        /**
         * Stores the number of rows. The page takes its rows from {@link #setPageOptions(PageOptions)} instead.
         *
         * @param rows the number of rows
         * @return this builder
         */
        public Builder setRows(int rows) {
            this.page.rows = rows;
            return this;
        }

        /**
         * Stores a function that gets the number of rows for each player. The page takes its rows from
         * {@link #setPageOptions(PageOptions)} instead.
         *
         * @param function gets the number of rows for a player
         * @return this builder
         */
        public Builder setRows(Function<PlayerReference, Integer> function) {
            this.page.rowsFunc = function;
            return this;
        }

        /**
         * Stores the display name. The page takes its title from {@link #setPageOptions(PageOptions)} instead.
         *
         * @param displayName the display name
         * @return this builder
         */
        public Builder setDisplayName(String displayName) {
            this.page.displayName = displayName;
            return this;
        }

        /**
         * Stores a function that gets the display name for each player. The page takes its title from
         * {@link #setPageOptions(PageOptions)} instead.
         *
         * @param function gets the display name for a player
         * @return this builder
         */
        public Builder setDisplayName(Function<PlayerReference, String> function) {
            this.page.displayNameFunc = function;
            return this;
        }

        /**
         * Sets the options of the page. The default is {@link PageOptions#defaults()}.
         *
         * @param options the page options
         * @return this builder
         */
        public Builder setPageOptions(PageOptions options) {
            this.page.options = options;
            return this;
        }

        /**
         * Gets the page. Later calls to this builder still change the same page.
         *
         * @return the page
         */
        public AtlantisPage build() {
            return this.page;
        }
    }

    /**
     * Creates an empty page. Use {@link #builder()} instead.
     */
    private AtlantisPage() {

    }

    /**
     * Gets the options from {@link Builder#setPageOptions(PageOptions)}, for every player.
     *
     * @param player the player that views the page
     * @return the page options
     */
    @Override
    public PageOptions options(PlayerReference player) {
        return this.options;
    }

    /**
     * Runs the action for the clicked slot, if there is one.
     *
     * @param player the player that clicked
     * @param index the index of the clicked slot
     * @param click the click
     */
    @Override
    public void onButtonClick(PlayerReference player, int index, Click click) {
        var consumer = this.onButtonClickActions.get(index);
        if (consumer != null) {
            consumer.accept(player, click);
        }
    }

    /**
     * Checks if the player may close the page. The function from {@link Builder#allowExit(Function)} takes
     * priority over the value from {@link Builder#allowExit(boolean)}.
     *
     * @param player the player that closes the page
     * @return {@code true} if the page closes, {@code false} if it opens again
     */
    @Override
    public boolean onClose(PlayerReference player) {
        return this.allowExitFunc != null ? this.allowExitFunc.apply(player) : this.allowExit;
    }
}
