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

public class PixelmonTrainer extends UTrainer<NPC> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "trainer");

    public PixelmonTrainer(NPC trainer) {
        super(TYPE, trainer);
    }

    @Override
    public List<PixelmonPokemon> team() {
        var team = new ArrayList<PixelmonPokemon>();
        for (var pokemon : this.dereference().getParty().getAllNonNull()) {
            team.add(new PixelmonPokemon(pokemon));
        }
        return team;
    }

    @Override
    public void setTeam(List<? extends UPokemon<?>> team) {
        var party = this.dereference().getParty();
        for (var slot = 0; slot < party.maxSize(); slot++) {
            party.set(slot, slot < team.size() ? PixelmonPokemon.unwrap(team.get(slot).copy()) : null);
        }
    }

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

    @Override
    public void setImmovable(boolean immovable) {
        this.dereference().setImmovable(immovable);
    }

    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.dereference().setInvulnerable(invulnerable);
    }

    @Override
    public void setNameVisible(boolean visible) {
        var npc = this.dereference();
        npc.setEntityProperties(npc.getProperties().withNamePlate(visible));
    }

    @Override
    public @Nullable PixelmonBattle battle() {
        var battle = BattleRegistry.getBattle(this.dereference());
        return battle == null || battle.battleEnded() ? null : new PixelmonBattle(battle);
    }
}
