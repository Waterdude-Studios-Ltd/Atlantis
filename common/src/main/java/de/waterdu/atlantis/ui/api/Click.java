package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

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

    public Click(Click click, Button button, Style style) {
        this(click.player, click.buttons, click.index, click.page, click.group, button, style, Rate.SINGLE, System.currentTimeMillis());
    }

    public Click(PlayerReference player, Buttons buttons, int index, int page, int group, Button button, Style style) {
        this(player, buttons, index, page, group, button, style, Rate.SINGLE, System.currentTimeMillis());
    }

    public Click(Click click, Rate rate, long time) {
        this(click.player, click.buttons, click.index, click.page, click.group, click.button, click.style, rate, time);
    }

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

    private static boolean doubleClickTest(Click click, Click previous) {
        return click.isSameButton(previous) && click.time - previous.time <= Settings.get().getDoubleClickDelay();
    }

    private static boolean doubleClickTest(
            Button button, Style style, Rate rate, long time,
            Button previousButton, Style previousStyle, Rate previousRate, long previousTime
    ) {
        return button == previousButton &&
                style == previousStyle &&
                rate == previousRate &&
                time - previousTime <= Settings.get().getDoubleClickDelay();
    }

    public boolean isSameButton(Click click) {
        return this.button == click.button && this.style == click.style && this.rate == click.rate;
    }

    public Click tryDoubleClick(Click previous) {
        if (doubleClickTest(this, previous)) {
            return new Click(this, Rate.DOUBLE, this.time);
        }
        return this;
    }

    public void open(Page page) {
        this.open(page, false);
    }

    public void open(Page page, boolean waitForScreen) {
        this.open(page, waitForScreen, null);
    }

    public void open(Page page, Buttons buttons) {
        this.open(page, false, buttons);
    }

    public void open(Page page, boolean waitForScreen, Buttons buttons) {
        AtlantisUI.open(this.player, page, buttons, waitForScreen);
    }

    public void updatePage(Page page) {
        AtlantisUI.open(this.player.entityDirect(), page, null, false, true);
    }

    public void closePage() {
        AtlantisUI.close(this.player);
    }

    public void next() {
        this.next(true);
    }

    public void next(boolean looped) {
        this.next(this.group, looped);
    }

    public void previous() {
        this.previous(true);
    }

    public void previous(boolean looped) {
        this.previous(this.group, looped);
    }

    public void next(int group) {
        this.next(group, true);
    }

    public void next(int group, boolean looped) {
        AtlantisUI.nextPage(this.player, group, looped);
    }

    public void previous(int group) {
        this.previous(group, true);
    }

    public void previous(int group, boolean looped) {
        AtlantisUI.prevPage(this.player, group, looped);
    }

    public void setPage(int page) {
        this.setPage(this.group, page);
    }

    public void setPage(int group, int page) {
        AtlantisUI.setPage(this.player, group, page);
    }

    public void change(int group, int change) {
        this.change(group, change, true);
    }

    public void change(int group, int change, boolean looped) {
        AtlantisUI.changePage(this.player, group, change, looped);
    }

    public void setProperty(int property, int value) {
        AtlantisUI.setProperty(this.player, property, value);
    }

    public ServerPlayer entity() {
        return this.player.entityDirect();
    }

    public boolean matches(int index) {
        return this.index == index;
    }

    public boolean matches(int index, Button button) {
        return this.matches(index) && this.button == button;
    }

    public boolean matches(int index, Button button, Style style) {
        return this.matches(index, button) && this.style == style;
    }

    public enum Button {

        LEFT,
        RIGHT,
        MIDDLE,
        UNKNOWN;

        private static final Button[] VALUES_AND_NULL = new Button[]{LEFT, RIGHT, MIDDLE, UNKNOWN, null};

        public static Button[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

        public static Button get(int button) {
            if (button >= 0 && button <= 2) {
                return Button.values()[button];
            }
            return Button.UNKNOWN;
        }

        public boolean isLeft() {
            return this == LEFT;
        }

        public boolean isRight() {
            return this == RIGHT;
        }

        public boolean isMiddle() {
            return this == MIDDLE;
        }

        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    public enum Style {

        NORMAL,
        SHIFT,
        DRAG,
        UNKNOWN;

        private static final Style[] VALUES_AND_NULL = new Style[]{NORMAL, SHIFT, DRAG, UNKNOWN, null};

        public static Style[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

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

        public boolean isNormal() {
            return this == NORMAL;
        }

        public boolean isShift() {
            return this == SHIFT;
        }

        public boolean isDrag() {
            return this == DRAG;
        }

        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    public enum Rate {

        SINGLE,
        DOUBLE,
        ;

        private static final Rate[] VALUES_AND_NULL = new Rate[]{SINGLE, DOUBLE, null};

        public static Rate[] valuesAndNull() {
            return VALUES_AND_NULL;
        }

        public boolean isSingle() {
            return this == SINGLE;
        }

        public boolean isDouble() {
            return this == DOUBLE;
        }
    }
}
