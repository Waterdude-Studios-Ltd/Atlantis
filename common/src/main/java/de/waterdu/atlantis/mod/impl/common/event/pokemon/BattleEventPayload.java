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

public abstract class BattleEventPayload extends PokemonModEventPayload {

    private final UBattle<?> battle;

    protected BattleEventPayload(Object source, UBattle<?> battle) {
        super(source);
        this.battle = battle;
    }

    public UBattle<?> battle() {
        return this.battle;
    }

    public static class StartPre extends BattleEventPayload implements Cancellable {

        private boolean cancelled = false;

        public StartPre(Object source, UBattle<?> battle) {
            super(source, battle);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_START_PRE;
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

    public static class StartPost extends BattleEventPayload {

        public StartPost(Object source, UBattle<?> battle) {
            super(source, battle);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_START_POST;
        }
    }

    /**
     * A battle has ended. Every participant has a result; a battle ended by force or by a player fleeing gives the
     * remaining participants a draw.
     */
    public static class End extends BattleEventPayload {

        private final Map<UBattleParticipant<?>, UBattleResult<?>> results;

        public End(Object source, UBattle<?> battle, Map<UBattleParticipant<?>, UBattleResult<?>> results) {
            super(source, battle);
            this.results = results;
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_BATTLE_END;
        }

        public Map<UBattleParticipant<?>, UBattleResult<?>> results() {
            return this.results;
        }

        public List<UBattleParticipant<?>> participants(Predicate<UBattleResult<?>> result) {
            var participants = new ArrayList<UBattleParticipant<?>>();
            this.results.forEach((participant, value) -> {
                if (result.test(value)) {
                    participants.add(participant);
                }
            });
            return participants;
        }

        public List<UBattleParticipant<?>> winners() {
            return this.participants(UBattleResult::isVictory);
        }

        public List<UBattleParticipant<?>> losers() {
            return this.participants(UBattleResult::isDefeat);
        }
    }
}
