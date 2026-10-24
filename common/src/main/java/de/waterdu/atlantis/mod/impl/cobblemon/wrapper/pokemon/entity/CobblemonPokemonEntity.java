package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle.CobblemonBattle;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class CobblemonPokemonEntity extends UPokemonEntity<PokemonEntity> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokemon_entity");

    public CobblemonPokemonEntity(PokemonEntity entity) {
        super(TYPE, entity);
    }

    @Override
    public CobblemonPokemon pokemon() {
        return new CobblemonPokemon(this.entity().getPokemon());
    }

    @Override
    public @Nullable UUID ownerUUID() {
        return this.entity().getOwnerUUID();
    }

    @Override
    public boolean isInBattle() {
        return this.entity().isBattling();
    }

    @Override
    public @Nullable CobblemonBattle battle() {
        var id = this.entity().getBattleId();
        if (id == null) {
            return null;
        }
        var battle = BattleRegistry.getBattle(id);
        return battle == null ? null : new CobblemonBattle(battle);
    }

    @Override
    public void recall() {
        if (!this.isWild()) {
            this.entity().recallWithAnimation();
        }
    }
}
