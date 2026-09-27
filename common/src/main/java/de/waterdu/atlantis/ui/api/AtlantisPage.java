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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AtlantisPage page;

        public Builder() {
            this.page = new AtlantisPage();
        }

        public Builder addButton(Button button) {
            this.page.buttons.add(button);
            return this;
        }

        public Builder addButton(Function<PlayerReference, Button> function) {
            this.page.buttonFuncs.add(function);
            return this;
        }

        public Builder addButtonAction(int index, BiConsumer<PlayerReference, Click> consumer) {
            this.page.onButtonClickActions.put(index, consumer);
            return this;
        }

        public Builder allowExit(boolean allowExit) {
            this.page.allowExit = allowExit;
            return this;
        }

        public Builder allowExit(Function<PlayerReference, Boolean> function) {
            this.page.allowExitFunc = function;
            return this;
        }

        public Builder setRows(int rows) {
            this.page.rows = rows;
            return this;
        }

        public Builder setRows(Function<PlayerReference, Integer> function) {
            this.page.rowsFunc = function;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.page.displayName = displayName;
            return this;
        }

        public Builder setDisplayName(Function<PlayerReference, String> function) {
            this.page.displayNameFunc = function;
            return this;
        }

        public Builder setPageOptions(PageOptions options) {
            this.page.options = options;
            return this;
        }

        public AtlantisPage build() {
            return this.page;
        }
    }

    private AtlantisPage() {

    }

    @Override
    public PageOptions options(PlayerReference player) {
        return this.options;
    }

    @Override
    public void onButtonClick(PlayerReference player, int index, Click click) {
        var consumer = this.onButtonClickActions.get(index);
        if (consumer != null) {
            consumer.accept(player, click);
        }
    }

    @Override
    public boolean onClose(PlayerReference player) {
        return this.allowExitFunc != null ? this.allowExitFunc.apply(player) : this.allowExit;
    }
}
