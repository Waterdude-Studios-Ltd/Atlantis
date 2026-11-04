package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.model.type.PlayerRenderingHandler;
import com.pixelmonmod.pixelmon.api.texture.type.PlayerSkinURLTextureProvider;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.npcs.NPC;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattle;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UTrainer;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.entity.PlayerSkins;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wraps a Pixelmon NPC that acts as a trainer.
 */
public class PixelmonTrainer extends UTrainer<NPC> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "trainer");

    /**
     * Creates a wrapper for a Pixelmon NPC.
     *
     * @param trainer the NPC to wrap
     */
    public PixelmonTrainer(NPC trainer) {
        super(TYPE, trainer);
    }

    /**
     * Gets a new wrapper for each Pokémon in the party of the NPC.
     *
     * @return the team
     */
    @Override
    public List<PixelmonPokemon> team() {
        var team = new ArrayList<PixelmonPokemon>();
        for (var pokemon : this.dereference().getParty().getAllNonNull()) {
            team.add(new PixelmonPokemon(pokemon));
        }
        return team;
    }

    /**
     * Fills the party of the NPC with copies of the team. Slots after the end of the team become empty.
     *
     * @param team the Pixelmon Pokémon to copy into the party
     */
    @Override
    public void setTeam(List<? extends UPokemon<?>> team) {
        var party = this.dereference().getParty();
        for (var slot = 0; slot < party.maxSize(); slot++) {
            party.set(slot, slot < team.size() ? PixelmonPokemon.unwrap(team.get(slot).copy()) : null);
        }
    }

    /**
     * Looks up the skin of the player asynchronously, then sets a player model with that skin on the server thread.
     * If no skin is found, nothing changes.
     *
     * @param playerName the name of the player whose skin to use
     */
    @Override
    public void setSkin(String playerName) {
        var npc = this.dereference();
        PlayerSkins.find(playerName).thenAccept(skin -> {
            if (skin != null) {
                var target = ResourceLocation.fromNamespaceAndPath(Atlantis.ID, "trainer_skins/" + playerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
                Concurrency.sync(() -> npc.setRenderingHandler(new PlayerRenderingHandler(skin.slim(), new PlayerSkinURLTextureProvider(skin.url(), target))));
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
        this.dereference().setImmovable(immovable);
    }

    /**
     * Sets if the NPC cannot take damage.
     *
     * @param invulnerable {@code true} to make the NPC invulnerable
     */
    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.dereference().setInvulnerable(invulnerable);
    }

    /**
     * Sets if the name plate of the NPC shows.
     *
     * @param visible {@code true} to show the name plate
     */
    @Override
    public void setNameVisible(boolean visible) {
        var npc = this.dereference();
        npc.setEntityProperties(npc.getProperties().withNamePlate(visible));
    }

    /**
     * Gets the battle of the NPC from the Pixelmon battle registry.
     *
     * @return the battle, or {@code null} if the NPC is in no battle or the battle has ended
     */
    @Override
    public @Nullable PixelmonBattle battle() {
        var battle = BattleRegistry.getBattle(this.dereference());
        return battle == null || battle.battleEnded() ? null : new PixelmonBattle(battle);
    }
}
