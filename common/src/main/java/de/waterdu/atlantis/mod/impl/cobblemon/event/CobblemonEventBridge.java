package de.waterdu.atlantis.mod.impl.cobblemon.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.experience.BattleExperienceSource;
import com.cobblemon.mod.common.api.reactive.Observable;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.trade.PlayerTradeParticipant;
import com.cobblemon.mod.common.trade.TradeParticipant;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattle;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattleParticipant;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattleResult;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity.CobblemonPokemonEntity;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonPokeBall;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.BattleEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.CaptureEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.EggHatchEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.EvolveEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.ExperienceGainEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.FaintEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.FriendshipEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.HeldItemEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.LevelUpEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.MegaEvolveEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.NicknameEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.ReceivedEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.RecallEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.ReleaseEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.SendOutEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.SpawnEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.StarterChosenEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.pokemon.TradeEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleResult;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CobblemonEventBridge {

    private CobblemonEventBridge() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {
        on(CobblemonEvents.THROWN_POKEBALL_HIT, event -> {
            if (event.getPokeBall().getOwner() instanceof ServerPlayer player) {
                var entity = new CobblemonPokemonEntity(event.getPokemon());
                var payload = emit(UEvent.POKEMON_CAPTURE_START, new CaptureEventPayload.Start(event, player, entity.pokemon(), new CobblemonPokeBall(event.getPokeBall().getPokeBall()), entity));
                if (payload.cancelled()) {
                    event.cancel();
                }
            }
        });
        on(CobblemonEvents.POKEMON_CAPTURED, event -> emit(UEvent.POKEMON_CAPTURE_SUCCESS, new CaptureEventPayload.Success(
                event, event.getPlayer(), pokemon(event.getPokemon()), new CobblemonPokeBall(event.getPokeBallEntity().getPokeBall()), null
        )));
        on(CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED, event -> {
            if (!event.getCaptureResult().isSuccessfulCapture() && event.getThrower() instanceof ServerPlayer player) {
                var entity = new CobblemonPokemonEntity(event.getPokemonEntity());
                emit(UEvent.POKEMON_CAPTURE_FAIL, new CaptureEventPayload.Fail(event, player, entity.pokemon(), new CobblemonPokeBall(event.getPokeBallEntity().getPokeBall()), entity));
            }
        });

        on(CobblemonEvents.BATTLE_STARTED_PRE, event -> {
            if (emit(UEvent.POKEMON_BATTLE_START_PRE, new BattleEventPayload.StartPre(event, new CobblemonBattle(event.getBattle()))).cancelled()) {
                event.cancel();
            }
        });
        on(CobblemonEvents.BATTLE_STARTED_POST, event -> emit(UEvent.POKEMON_BATTLE_START_POST, new BattleEventPayload.StartPost(event, new CobblemonBattle(event.getBattle()))));
        on(CobblemonEvents.BATTLE_VICTORY, event -> emit(UEvent.POKEMON_BATTLE_END, new BattleEventPayload.End(event, new CobblemonBattle(event.getBattle()), results(event.getBattle(), actor -> {
            if (event.getWinners().contains(actor)) {
                return CobblemonBattleResult.VICTORY_RESULT;
            }
            return event.getLosers().contains(actor) ? CobblemonBattleResult.DEFEAT_RESULT : CobblemonBattleResult.DRAW_RESULT;
        }))));
        on(CobblemonEvents.BATTLE_FLED, event -> emit(UEvent.POKEMON_BATTLE_END, new BattleEventPayload.End(event, new CobblemonBattle(event.getBattle()), results(event.getBattle(),
                actor -> actor == event.getPlayer() ? CobblemonBattleResult.FLEE_RESULT : CobblemonBattleResult.DRAW_RESULT
        ))));

        on(CobblemonEvents.EVOLUTION_ACCEPTED, event -> {
            if (emit(UEvent.POKEMON_EVOLVE_PRE, new EvolveEventPayload.Pre(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.cancel();
            }
        });
        on(CobblemonEvents.EVOLUTION_COMPLETE, event -> emit(UEvent.POKEMON_EVOLVE_POST, new EvolveEventPayload.Post(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()))));
        on(CobblemonEvents.MEGA_EVOLUTION, event -> {
            var pokemon = event.getPokemon().getOriginalPokemon();
            emit(UEvent.POKEMON_MEGA_EVOLVE, new MegaEvolveEventPayload(event, pokemon.getOwnerPlayer(), pokemon(pokemon), new CobblemonBattle(event.getBattle())));
        });

        on(CobblemonEvents.LEVEL_UP_EVENT, event -> {
            var payload = emit(UEvent.POKEMON_LEVEL_UP, new LevelUpEventPayload(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getOldLevel(), event.getNewLevel()));
            event.setNewLevel(payload.newLevel());
        });
        on(CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE, event -> {
            var payload = emit(UEvent.POKEMON_EXPERIENCE_GAIN, new ExperienceGainEventPayload(
                    event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getExperience(), event.getSource() instanceof BattleExperienceSource
            ));
            if (payload.cancelled()) {
                event.cancel();
            } else {
                event.setExperience(payload.experience());
            }
        });
        on(CobblemonEvents.FRIENDSHIP_UPDATED, event -> emit(UEvent.POKEMON_FRIENDSHIP_CHANGE, new FriendshipEventPayload(
                event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getPokemon().getFriendship(), event.getNewFriendship()
        )));
        on(CobblemonEvents.POKEMON_FAINTED, event -> emit(UEvent.POKEMON_FAINT, new FaintEventPayload(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()))));
        on(CobblemonEvents.HATCH_EGG_POST, event -> emit(UEvent.POKEMON_EGG_HATCH, new EggHatchEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))));

        on(CobblemonEvents.POKEMON_SENT_PRE, event -> {
            if (emit(UEvent.POKEMON_SEND_OUT_PRE, new SendOutEventPayload.Pre(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.cancel();
            }
        });
        on(CobblemonEvents.POKEMON_SENT_POST, event -> emit(UEvent.POKEMON_SEND_OUT_POST, new SendOutEventPayload.Post(
                event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), new CobblemonPokemonEntity(event.getPokemonEntity())
        )));
        on(CobblemonEvents.POKEMON_RECALL_POST, event -> emit(UEvent.POKEMON_RECALL, new RecallEventPayload(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()))));
        on(CobblemonEvents.ENTITY_SPAWN, event -> {
            if (event.getEntity() instanceof PokemonEntity entity && emit(UEvent.POKEMON_SPAWN, new SpawnEventPayload(event, new CobblemonPokemonEntity(entity))).cancelled()) {
                event.cancel();
            }
        });

        on(CobblemonEvents.TRADE_EVENT_PRE, event -> {
            var payload = emit(UEvent.POKEMON_TRADE_PRE, new TradeEventPayload.Pre(
                    event,
                    player(event.getTradeParticipant1()), pokemon(event.getTradeParticipant1Pokemon()),
                    player(event.getTradeParticipant2()), pokemon(event.getTradeParticipant2Pokemon())
            ));
            if (payload.cancelled()) {
                event.cancel();
            }
        });
        on(CobblemonEvents.TRADE_EVENT_POST, event -> emit(UEvent.POKEMON_TRADE_POST, new TradeEventPayload.Post(
                event,
                player(event.getTradeParticipant1()), pokemon(event.getTradeParticipant1Pokemon()),
                player(event.getTradeParticipant2()), pokemon(event.getTradeParticipant2Pokemon())
        )));

        on(CobblemonEvents.STARTER_CHOSEN, event -> {
            if (emit(UEvent.POKEMON_STARTER_CHOSEN, new StarterChosenEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.cancel();
            }
        });
        on(CobblemonEvents.POKEMON_GAINED, event -> {
            var server = ServerUtils.server();
            var player = server == null ? null : server.getPlayerList().getPlayer(event.getPlayerId());
            emit(UEvent.POKEMON_RECEIVED, new ReceivedEventPayload(event, event.getPlayerId(), player, pokemon(event.getPokemon()), ReceivedEventPayload.UNKNOWN_CAUSE));
        });
        on(CobblemonEvents.POKEMON_RELEASED_EVENT_POST, event -> emit(UEvent.POKEMON_RELEASE, new ReleaseEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))));
        on(CobblemonEvents.POKEMON_NICKNAMED, event -> {
            var nickname = event.getNickname();
            var payload = emit(UEvent.POKEMON_NICKNAME, new NicknameEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()), nickname == null ? null : nickname.getString()));
            if (payload.cancelled()) {
                event.cancel();
            } else {
                var changed = payload.nickname();
                event.setNickname(changed == null || changed.isEmpty() ? null : Component.literal(changed));
            }
        });
        on(CobblemonEvents.HELD_ITEM_PRE, event -> {
            var payload = emit(UEvent.POKEMON_HELD_ITEM_PRE, new HeldItemEventPayload.Pre(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getReceiving()));
            if (payload.cancelled()) {
                event.cancel();
            } else {
                event.setReceiving(payload.item());
            }
        });
        on(CobblemonEvents.HELD_ITEM_POST, event -> emit(UEvent.POKEMON_HELD_ITEM_POST, new HeldItemEventPayload.Post(event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getReceived())));
    }

    private static <T> void on(Observable<T> observable, Consumer<T> handler) {
        observable.subscribe(Priority.NORMAL, handler);
    }

    private static <P extends EventPayload> P emit(Key<UEvent<P>> key, P payload) {
        return Mods.modLoader().event(key).emit(payload);
    }

    private static CobblemonPokemon pokemon(Pokemon pokemon) {
        return new CobblemonPokemon(pokemon);
    }

    private static @Nullable ServerPlayer player(TradeParticipant participant) {
        return participant instanceof PlayerTradeParticipant player ? player.getPlayer() : null;
    }

    private static Map<UBattleParticipant<?>, UBattleResult<?>> results(PokemonBattle battle, Function<BattleActor, UBattleResult<?>> result) {
        var results = new LinkedHashMap<UBattleParticipant<?>, UBattleResult<?>>();
        for (var actor : battle.getActors()) {
            results.put(new CobblemonBattleParticipant(actor), result.apply(actor));
        }
        return results;
    }
}
