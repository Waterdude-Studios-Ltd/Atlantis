package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.config.Leaderboard;

import java.util.UUID;

/**
 * Holds the leaderboard of a leaderboard event.
 *
 * @param <T> the data type that the leaderboard ranks
 * @param <V> the type of the ranked value
 */
public abstract class LeaderboardEventPayload<T extends Data, V extends Comparable<V>> implements EventPayload {

    private final Leaderboard<T, V> leaderboard;

    /**
     * Creates a leaderboard payload.
     *
     * @param leaderboard the leaderboard, or {@code null} for all leaderboards
     */
    protected LeaderboardEventPayload(Leaderboard<T, V> leaderboard) {
        this.leaderboard = leaderboard;
    }

    /**
     * The leaderboard, or null for {@link Recalculate.PreAll} and {@link Recalculate.PostAll}.
     */
    public Leaderboard<T, V> leaderboard() {
        return this.leaderboard;
    }

    /**
     * Fires before a new value for an entry is stored in the leaderboard. If a listener cancels it, the value is not
     * stored.
     *
     * @param <T> the data type that the leaderboard ranks
     * @param <V> the type of the ranked value
     */
    public static class Update<T extends Data, V extends Comparable<V>> extends LeaderboardEventPayload<T, V> implements EventPayload, Cancellable {

        private final T data;
        private final UUID key;
        private final Comparable<V> value;

        private boolean cancelled = false;

        /**
         * Creates a payload for {@link UEvent#UPDATE_LEADERBOARD}.
         *
         * @param leaderboard the leaderboard that updates
         * @param data the data that the value comes from
         * @param key the UUID of the entry
         * @param value the new value
         */
        public Update(Leaderboard<T, V> leaderboard, T data, UUID key, Comparable<V> value) {
            super(leaderboard);
            this.data = data;
            this.key = key;
            this.value = value;
        }

        /**
         * @return {@link UEvent#UPDATE_LEADERBOARD}
         */
        @Override
        public Key<?> type() {
            return UEvent.UPDATE_LEADERBOARD;
        }

        /**
         * @return the data that the value comes from
         */
        public T data() {
            return this.data;
        }

        /**
         * @return the UUID of the entry
         */
        public UUID key() {
            return this.key;
        }

        /**
         * @return the new value
         */
        public Comparable<V> value() {
            return this.value;
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
    }

    /**
     * Holds the leaderboard of a recalculation event.
     *
     * @param <T> the data type that the leaderboard ranks
     * @param <V> the type of the ranked value
     */
    public abstract static class Recalculate<T extends Data, V extends Comparable<V>> extends LeaderboardEventPayload<T, V> implements EventPayload {

        /**
         * Creates a recalculation payload.
         *
         * @param leaderboard the leaderboard, or {@code null} for all leaderboards
         */
        protected Recalculate(Leaderboard<T, V> leaderboard) {
            super(leaderboard);
        }

        /**
         * Fires before all leaderboards recalculate. If a listener cancels it, no leaderboard recalculates.
         *
         * @param <T> the data type that the leaderboard ranks
         * @param <V> the type of the ranked value
         */
        public static class PreAll<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload, Cancellable {

            private boolean cancelled = false;

            /**
             * Creates a payload for {@link UEvent#RECALCULATE_LEADERBOARD_PRE_ALL}.
             *
             * @param leaderboard the leaderboard. Atlantis passes {@code null}.
             */
            public PreAll(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            /**
             * @return {@link UEvent#RECALCULATE_LEADERBOARD_PRE_ALL}
             */
            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_PRE_ALL;
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
        }

        /**
         * Fires on the server thread after all leaderboards recalculated.
         *
         * @param <T> the data type that the leaderboard ranks
         * @param <V> the type of the ranked value
         */
        public static class PostAll<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload {

            /**
             * Creates a payload for {@link UEvent#RECALCULATE_LEADERBOARD_POST_ALL}.
             *
             * @param leaderboard the leaderboard. Atlantis passes {@code null}.
             */
            public PostAll(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            /**
             * @return {@link UEvent#RECALCULATE_LEADERBOARD_POST_ALL}
             */
            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_POST_ALL;
            }
        }

        /**
         * Fires before one leaderboard recalculates. If a listener cancels it, the leaderboard does not recalculate.
         *
         * @param <T> the data type that the leaderboard ranks
         * @param <V> the type of the ranked value
         */
        public static class Pre<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload, Cancellable {

            private boolean cancelled = false;

            /**
             * Creates a payload for {@link UEvent#RECALCULATE_LEADERBOARD_PRE}.
             *
             * @param leaderboard the leaderboard that recalculates
             */
            public Pre(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            /**
             * @return {@link UEvent#RECALCULATE_LEADERBOARD_PRE}
             */
            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_PRE;
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
        }

        /**
         * Fires after one leaderboard recalculated its rankings.
         *
         * @param <T> the data type that the leaderboard ranks
         * @param <V> the type of the ranked value
         */
        public static class Post<T extends Data, V extends Comparable<V>> extends Recalculate<T, V> implements EventPayload {

            /**
             * Creates a payload for {@link UEvent#RECALCULATE_LEADERBOARD_POST}.
             *
             * @param leaderboard the leaderboard that recalculated
             */
            public Post(Leaderboard<T, V> leaderboard) {
                super(leaderboard);
            }

            /**
             * @return {@link UEvent#RECALCULATE_LEADERBOARD_POST}
             */
            @Override
            public Key<?> type() {
                return UEvent.RECALCULATE_LEADERBOARD_POST;
            }
        }
    }
}
