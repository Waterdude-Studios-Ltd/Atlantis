package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType;
import com.cobblemon.mod.common.entity.npc.NPCPlayerTexture;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattle;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UTrainer;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.entity.PlayerSkins;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Cobblemon NPC entity that acts as a trainer.
 */
public class CobblemonTrainer extends UTrainer<NPCEntity> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "trainer");

    /**
     * Creates a wrapper for a Cobblemon NPC.
     *
     * @param trainer the NPC to wrap
     */
    public CobblemonTrainer(NPCEntity trainer) {
        super(TYPE, trainer);
    }

    /**
     * Gets a new wrapper for each Pokémon in the party of the NPC.
     *
     * @return the team, or an empty list if the NPC has no party
     */
    @Override
    public List<CobblemonPokemon> team() {
        var team = new ArrayList<CobblemonPokemon>();
        var party = this.dereference().getParty();
        if (party != null) {
            for (var pokemon : party) {
                team.add(new CobblemonPokemon(pokemon));
            }
        }
        return team;
    }

    /**
     * Gives the NPC a new party with copies of the first six Pokémon of the team.
     *
     * @param team the Cobblemon Pokémon to copy into the party
     */
    @Override
    public void setTeam(List<? extends UPokemon<?>> team) {
        var npc = this.dereference();
        var party = new NPCPartyStore(npc);
        for (var slot = 0; slot < Math.min(team.size(), 6); slot++) {
            party.set(slot, CobblemonPokemon.unwrap(team.get(slot).copy()));
        }
        npc.setParty(party);
    }

    /**
     * Looks up the skin of the player asynchronously, downloads it, then applies the texture and model aspect on the
     * server thread. If no skin is found, nothing changes. If the download fails, a warning is logged.
     *
     * @param playerName the name of the player whose skin to use
     */
    @Override
    public void setSkin(String playerName) {
        var npc = this.dereference();
        PlayerSkins.find(playerName).thenAccept(skin -> {
            if (skin == null) {
                return;
            }
            try {
                var texture = new NPCPlayerTexture(skin.download(), skin.slim() ? NPCPlayerModelType.SLIM : NPCPlayerModelType.DEFAULT);
                Concurrency.sync(() -> {
                    npc.getAppliedAspects().remove("model-default");
                    npc.getAppliedAspects().remove("model-slim");
                    npc.getAppliedAspects().add(skin.slim() ? "model-slim" : "model-default");
                    npc.getEntityData().set(NPCEntity.Companion.getNPC_PLAYER_TEXTURE(), texture);
                    npc.updateAspects();
                });
            } catch (Exception e) {
                AtlantisLogger.warn("Unable to download the skin of {}: {}", playerName, e.getMessage());
            }
        });
    }

    /**
     * Sets if the NPC cannot move.
     *
     * @param immovable {@code true} to stop the NPC from moving
     */
    @Override
    public void setImmovable(boolean immovable) {
        this.dereference().setMovable(!immovable);
    }

    /**
     * Sets if the NPC cannot take damage.
     *
     * @param invulnerable {@code true} to make the NPC invulnerable
     */
    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.dereference().setInvulnerable(Boolean.valueOf(invulnerable));
    }

    /**
     * Sets if the name tag of the NPC shows.
     *
     * @param visible {@code true} to show the name tag
     */
    @Override
    public void setNameVisible(boolean visible) {
        this.dereference().setHideNameTag(!visible);
    }

    /**
     * Gets the first battle of the NPC that has not ended.
     *
     * @return the battle, or {@code null} if the NPC is in no active battle
     */
    @Override
    public @Nullable CobblemonBattle battle() {
        for (var id : this.dereference().getBattleIds()) {
            var battle = BattleRegistry.getBattle(id);
            if (battle != null && !battle.getEnded()) {
                return new CobblemonBattle(battle);
            }
        }
        return null;
    }
}
