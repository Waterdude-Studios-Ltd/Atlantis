package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleResult;
import de.waterdu.atlantis.util.config.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Base class of the payloads for Pokémon battle events.
 */
public abstract class BattleEventPayload extends PokemonModEventPayload {

    private final UBattle<?> battle;

    /**
     * Creates the payload for a battle event.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param battle the battle of the event
     */
    protected BattleEventPayload(Object source, UBattle<?> battle) {
        super(source);
        this.battle = battle;
    }

    /**
     * @return the battle of the event
     */
    public UBattle<?> battle() {
        return this.battle;
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_BATTLE_START_PRE}, which fires before a battle starts.
     */
    public static class StartPre extends BattleEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload for a battle that is about to start.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param battle the battle that is about to start
         */
        public StartPre(Object source, UBattle<?> battle) {
            super(source, battle);
        }

        /**
         * @return {@link UEvent#POKEMON_BATTLE_START_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_START_PRE;
        }

        /**
         * Cancels the event. The Pokémon mod then cancels its own event, and the battle does not start.
         */
        @Override
        public void cancel() {
            this.cancelled = true;
        }

        /**
         * @return {@code true} if the event is cancelled
         */
        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_BATTLE_START_POST}, which fires after a battle starts.
     */
    public static class StartPost extends BattleEventPayload {

        /**
         * Creates the payload for a battle that has started.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param battle the battle that has started
         */
        public StartPost(Object source, UBattle<?> battle) {
            super(source, battle);
        }

        /**
         * @return {@link UEvent#POKEMON_BATTLE_START_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_START_POST;
        }
    }

    /**
     * A battle has ended. Every participant has a result. If a battle ends by force or because a player flees, the
     * remaining participants get a draw.
     */
    public static class End extends BattleEventPayload {

        private final Map<UBattleParticipant<?>, UBattleResult<?>> results;

        /**
         * Creates the payload for a battle that has ended.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param battle the battle that has ended
         * @param results the result of each participant
         */
        public End(Object source, UBattle<?> battle, Map<UBattleParticipant<?>, UBattleResult<?>> results) {
            super(source, battle);
            this.results = results;
        }

        /**
         * @return {@link UEvent#POKEMON_BATTLE_END}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_END;
        }

        /**
         * @return the result of each participant
         */
        public Map<UBattleParticipant<?>, UBattleResult<?>> results() {
            return this.results;
        }

        /**
         * Gets the participants whose result matches a test.
         *
         * @param result the test for the result of a participant
         * @return a new list of the matching participants
         */
        public List<UBattleParticipant<?>> participants(Predicate<UBattleResult<?>> result) {
            var participants = new ArrayList<UBattleParticipant<?>>();
            this.results.forEach((participant, value) -> {
                if (result.test(value)) {
                    participants.add(participant);
                }
            });
            return participants;
        }

        /**
         * @return a new list of the participants whose result is a victory
         */
        public List<UBattleParticipant<?>> winners() {
            return this.participants(UBattleResult::isVictory);
        }

        /**
         * @return a new list of the participants whose result is a defeat
         */
        public List<UBattleParticipant<?>> losers() {
            return this.participants(UBattleResult::isDefeat);
        }
    }
}
