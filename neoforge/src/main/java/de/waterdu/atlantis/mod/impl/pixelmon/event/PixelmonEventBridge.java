package de.waterdu.atlantis.mod.impl.pixelmon.event;

import com.pixelmonmod.pixelmon.api.enums.DeleteType;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.EggHatchEvent;
import com.pixelmonmod.pixelmon.api.events.EvolveEvent;
import com.pixelmonmod.pixelmon.api.events.ExperienceGainEvent;
import com.pixelmonmod.pixelmon.api.events.HeldItemChangedEvent;
import com.pixelmonmod.pixelmon.api.events.LevelUpEvent;
import com.pixelmonmod.pixelmon.api.events.MegaEvolutionEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonDeletedEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonFaintEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonTradeEvent;
import com.pixelmonmod.pixelmon.api.events.PokemonReceivedEvent;
import com.pixelmonmod.pixelmon.api.events.PokemonRetrievedEvent;
import com.pixelmonmod.pixelmon.api.events.PokemonSendOutEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.FriendshipChangedEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.SetNicknameEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import de.waterdu.atlantis.mod.Mods;
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
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattle;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattleParticipant;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattleResult;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity.PixelmonPokemonEntity;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonPokeBall;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleResult;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;

public final class PixelmonEventBridge {

    private PixelmonEventBridge() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register(IEventBus bus) {
        bus.addListener(CaptureEvent.StartCapture.class, event -> {
            var payload = emit(UEvent.POKEMON_CAPTURE_START, new CaptureEventPayload.Start(
                    event, event.getPlayer(), pokemon(event.getPokemon()), new PixelmonPokeBall(event.getPokeBall()), entity(event.getPokemon())
            ));
            if (payload.cancelled()) {
                event.setCanceled(true);
            }
        });
        bus.addListener(CaptureEvent.SuccessfulCapture.class, event -> emit(UEvent.POKEMON_CAPTURE_SUCCESS, new CaptureEventPayload.Success(
                event, event.getPlayer(), pokemon(event.getPokemon()), new PixelmonPokeBall(event.getPokeBall()), entity(event.getPokemon())
        )));
        bus.addListener(CaptureEvent.FailedCapture.class, event -> emit(UEvent.POKEMON_CAPTURE_FAIL, new CaptureEventPayload.Fail(
                event, event.getPlayer(), pokemon(event.getPokemon()), new PixelmonPokeBall(event.getPokeBall()), entity(event.getPokemon())
        )));

        bus.addListener(BattleStartedEvent.Pre.class, event -> {
            if (emit(UEvent.POKEMON_BATTLE_START_PRE, new BattleEventPayload.StartPre(event, new PixelmonBattle(event.getBattleController()))).cancelled()) {
                event.setCanceled(true);
            }
        });
        bus.addListener(BattleStartedEvent.Post.class, event -> emit(UEvent.POKEMON_BATTLE_START_POST, new BattleEventPayload.StartPost(event, new PixelmonBattle(event.getBattleController()))));
        bus.addListener(BattleEndEvent.class, event -> {
            var results = new LinkedHashMap<UBattleParticipant<?>, UBattleResult<?>>();
            event.getResults().forEach((participant, result) -> results.put(new PixelmonBattleParticipant(participant), new PixelmonBattleResult(result)));
            emit(UEvent.POKEMON_BATTLE_END, new BattleEventPayload.End(event, new PixelmonBattle(event.getBattleController()), results));
        });

        bus.addListener(EvolveEvent.Pre.class, event -> {
            if (emit(UEvent.POKEMON_EVOLVE_PRE, new EvolveEventPayload.Pre(event, event.getPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.setCanceled(true);
            }
        });
        bus.addListener(EvolveEvent.Post.class, event -> emit(UEvent.POKEMON_EVOLVE_POST, new EvolveEventPayload.Post(event, event.getPlayer(), pokemon(event.getPokemon()))));
        bus.addListener(MegaEvolutionEvent.Battle.class, event -> {
            var wrapper = event.getPixelmonWrapper();
            emit(UEvent.POKEMON_MEGA_EVOLVE, new MegaEvolveEventPayload(event, event.getPlayer(), pokemon(wrapper), wrapper.bc == null ? null : new PixelmonBattle(wrapper.bc)));
        });
        bus.addListener(MegaEvolutionEvent.Post.class, event -> emit(UEvent.POKEMON_MEGA_EVOLVE, new MegaEvolveEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()), null)));

        bus.addListener(LevelUpEvent.Pre.class, event -> {
            var payload = emit(UEvent.POKEMON_LEVEL_UP, new LevelUpEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()), event.getBeforeLevel(), event.getAfterLevel()));
            if (payload.cancelled()) {
                event.setCanceled(true);
            } else {
                event.setAfterLevel(payload.newLevel());
            }
        });
        bus.addListener(ExperienceGainEvent.class, event -> {
            var payload = emit(UEvent.POKEMON_EXPERIENCE_GAIN, new ExperienceGainEventPayload(
                    event, event.pokemon.getOwnerPlayer(), pokemon(event.pokemon), event.getExperience(), event.isFromBattle()
            ));
            if (payload.cancelled()) {
                event.setCanceled(true);
            } else {
                event.setExperience(payload.experience());
            }
        });
        bus.addListener(FriendshipChangedEvent.class, event -> emit(UEvent.POKEMON_FRIENDSHIP_CHANGE, new FriendshipEventPayload(
                event, event.getPokemon().getOwnerPlayer(), pokemon(event.getPokemon()), event.getPreviousFriendshipAmount(), event.getActualNextFriendshipAmount()
        )));
        bus.addListener(PixelmonFaintEvent.Post.class, event -> emit(UEvent.POKEMON_FAINT, new FaintEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))));
        bus.addListener(EggHatchEvent.Post.class, event -> emit(UEvent.POKEMON_EGG_HATCH, new EggHatchEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))));

        bus.addListener(PokemonSendOutEvent.Pre.class, event -> {
            if (emit(UEvent.POKEMON_SEND_OUT_PRE, new SendOutEventPayload.Pre(event, event.getPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.setCanceled(true);
            }
        });
        bus.addListener(PokemonSendOutEvent.Post.class, event -> emit(UEvent.POKEMON_SEND_OUT_POST, new SendOutEventPayload.Post(
                event, event.getPlayer(), pokemon(event.getPokemon()), new PixelmonPokemonEntity(event.getEntity())
        )));
        bus.addListener(PokemonRetrievedEvent.Post.class, event -> emit(UEvent.POKEMON_RECALL, new RecallEventPayload(
                event, event.getOwner() instanceof ServerPlayer player ? player : null, pokemon(event.getPokemon())
        )));
        bus.addListener(SpawnEvent.class, event -> {
            if (event.action.getOrCreateEntity() instanceof PixelmonEntity entity
                    && emit(UEvent.POKEMON_SPAWN, new SpawnEventPayload(event, new PixelmonPokemonEntity(entity))).cancelled()) {
                event.setCanceled(true);
            }
        });

        bus.addListener(PixelmonTradeEvent.Pre.class, event -> {
            var payload = emit(UEvent.POKEMON_TRADE_PRE, new TradeEventPayload.Pre(
                    event,
                    event.getPlayer1() instanceof ServerPlayer player1 ? player1 : null, pokemon(event.getPokemon1()),
                    event.getPlayer2() instanceof ServerPlayer player2 ? player2 : null, pokemon(event.getPokemon2())
            ));
            if (payload.cancelled()) {
                event.setCanceled(true);
            }
        });
        bus.addListener(PixelmonTradeEvent.Post.class, event -> emit(UEvent.POKEMON_TRADE_POST, new TradeEventPayload.Post(
                event,
                event.getPlayer1() instanceof ServerPlayer player1 ? player1 : null, pokemon(event.getPokemon1()),
                event.getPlayer2() instanceof ServerPlayer player2 ? player2 : null, pokemon(event.getPokemon2())
        )));

        bus.addListener(PokemonReceivedEvent.class, event -> {
            if (PokemonReceivedEvent.Constants.STARTER.equals(event.getCause())
                    && emit(UEvent.POKEMON_STARTER_CHOSEN, new StarterChosenEventPayload(event, event.getPlayer(), pokemon(event.getPokemon()))).cancelled()) {
                event.setCanceled(true);
                return;
            }
            emit(UEvent.POKEMON_RECEIVED, new ReceivedEventPayload(event, event.getPlayer().getUUID(), event.getPlayer(), pokemon(event.getPokemon()), event.getCause()));
        });
        bus.addListener(PixelmonDeletedEvent.class, event -> {
            if (event.deleteType == DeleteType.PC) {
                emit(UEvent.POKEMON_RELEASE, new ReleaseEventPayload(event, event.player, pokemon(event.pokemon)));
            }
        });
        bus.addListener(SetNicknameEvent.class, event -> {
            var payload = emit(UEvent.POKEMON_NICKNAME, new NicknameEventPayload(event, event.player, pokemon(event.pokemon), event.nickname));
            if (payload.cancelled()) {
                event.setCanceled(true);
            } else {
                event.nickname = payload.nickname() == null ? "" : payload.nickname();
            }
        });
        bus.addListener(HeldItemChangedEvent.Pre.class, event -> {
            var payload = emit(UEvent.POKEMON_HELD_ITEM_PRE, new HeldItemEventPayload.Pre(event, event.getPlayer(), pokemon(event.getPokemon()), event.getHeldItem()));
            if (payload.cancelled()) {
                event.setCanceled(true);
            } else {
                event.setHeldItem(payload.item());
            }
        });
        bus.addListener(HeldItemChangedEvent.Post.class, event -> emit(UEvent.POKEMON_HELD_ITEM_POST, new HeldItemEventPayload.Post(event, event.getPlayer(), pokemon(event.getPokemon()), event.getHeldItem())));
    }

    private static <P extends EventPayload> P emit(Key<UEvent<P>> key, P payload) {
        return Mods.modLoader().event(key).emit(payload);
    }

    private static PixelmonPokemon pokemon(Pokemon pokemon) {
        return new PixelmonPokemon(pokemon);
    }

    private static @Nullable PixelmonPokemonEntity entity(Pokemon pokemon) {
        return pokemon.getPixelmonEntity().map(PixelmonPokemonEntity::new).orElse(null);
    }
}
