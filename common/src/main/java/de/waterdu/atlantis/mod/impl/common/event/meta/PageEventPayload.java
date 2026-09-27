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

public abstract class PageEventPayload implements EventPayload, Cancellable {

    private final PlayerReference player;
    private final Page page;

    private boolean cancelled = false;

    protected PageEventPayload(PlayerReference player, Page page) {
        this.player = player;
        this.page = page;
    }

    public PlayerReference player() {
        return this.player;
    }

    public Page page() {
        return this.page;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    public static class Construct extends PageEventPayload {

        private Buttons buttons;
        private PageOptions options;

        public Construct(PlayerReference player, Page page, Buttons buttons, PageOptions options) {
            super(player, page);
            this.buttons = buttons;
            this.options = options;
        }

        @Override
        public Key<?> type() {
            return UEvent.CONSTRUCT_PAGE;
        }

        public Buttons buttons() {
            return this.buttons;
        }

        public void setButtons(Buttons buttons) {
            this.buttons = buttons;
        }

        public void addButton(Button button) {
            this.buttons.setButton(button.index(), button.group(), button.page(), button);
        }

        public PageOptions options() {
            return this.options;
        }

        public void setOptions(PageOptions options) {
            this.options = options;
        }
    }

    public static class Decorate extends PageEventPayload {

        private final Decorations instance;
        private Set<Button> decorations;

        public Decorate(PlayerReference player, Page page, Decorations instance, Set<Button> decorations) {
            super(player, page);
            this.instance = instance;
            this.decorations = decorations;
        }

        @Override
        public Key<?> type() {
            return UEvent.DECORATE_PAGE;
        }

        public Decorations instance() {
            return this.instance;
        }

        public Set<Button> decorations() {
            return this.decorations;
        }

        public void addDecoration(Button button) {
            this.decorations.add(button);
        }

        public void setDecorations(Set<Button> decorations) {
            this.decorations = decorations;
        }
    }

    public static class Destruct extends PageEventPayload {

        public Destruct(PlayerReference player, Page page) {
            super(player, page);
        }

        @Override
        public Key<?> type() {
            return UEvent.DESTRUCT_PAGE;
        }
    }

    public static class Press extends PageEventPayload {

        private final Button button;
        private final Click click;

        public Press(PlayerReference player, Page page, Button button, Click click) {
            super(player, page);
            this.button = button;
            this.click = click;
        }

        @Override
        public Key<?> type() {
            return UEvent.PRESS_PAGE;
        }

        public Button button() {
            return this.button;
        }

        public Click click() {
            return this.click;
        }
    }
}
