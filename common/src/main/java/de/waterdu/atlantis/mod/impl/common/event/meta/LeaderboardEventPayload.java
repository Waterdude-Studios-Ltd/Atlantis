package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.config.Leaderboard;

import java.util.UUID;

public abstract class LeaderboardEventPayload<T extends Data, V extends Comparable<V>> implements EventPayload {

    private final Leaderboard<T, V> leaderboard;

    protected LeaderboardEventPayload(Leaderboard<T, V> leaderboard) {
        this.leaderboard = leaderboard;
    }

    public Leaderboard<T, V> leaderboard() {
        return this.leaderboard;
    }

    public static class Update<T extends Data, V extends Comparable<V>> extends LeaderboardEventPayload<T, V> implements EventPayload, Cancellable {

        private final T data;
        private final UUID key;
        private final Comparable<V> value;

        private boolean cancelled = false;

        public Update(Leaderboard<T, V> leaderboard, T data, UUID key, Comparable<V> value) {
            super(leaderboard);
            this.data = data;
            this.key = key;
            this.value = value;
        }

        @Override
        public Key<?> type() {
            return UEvent.UPDATE_LEADERBOARD;
        }

        public T data() {
            return this.data;
        }

        public UUID key() {
            return this.key;
        }

        public Comparable<V> value() {
            return this.value;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }

    public abstract static class Recalculate<T extends Data, V extends Comparable<V>> extends LeaderboardEventPayload<T, V> implements EventPayload {

        protected Recalculate(Leaderboard<T, V> leaderboard) {
            super(leaderboard);
        }

        public static class PreAll<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload, Cancellable {

            private boolean cancelled = false;

            public PreAll(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_PRE_ALL;
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }

            @Override
            public boolean cancelled() {
                return this.cancelled;
            }
        }

        public static class PostAll<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload {

            public PostAll(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_POST_ALL;
            }
        }

        public static class Pre<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload, Cancellable {

            private boolean cancelled = false;

            public Pre(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_PRE;
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }

            @Override
            public boolean cancelled() {
                return this.cancelled;
            }
        }

        public static class Post<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload {

            public Post(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_POST;
            }
        }
    }
}
