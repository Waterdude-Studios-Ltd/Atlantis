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

/**
 * Wraps a Cobblemon Pokémon entity.
 */
public class CobblemonPokemonEntity extends UPokemonEntity<PokemonEntity> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokemon_entity");

    /**
     * Creates a wrapper for a Cobblemon Pokémon entity.
     *
     * @param entity the entity to wrap
     */
    public CobblemonPokemonEntity(PokemonEntity entity) {
        super(TYPE, entity);
    }

    /**
     * @return a new wrapper for the Pokémon of the entity
     */
    @Override
    public CobblemonPokemon pokemon() {
        return new CobblemonPokemon(this.entity().getPokemon());
    }

    /**
     * Gets the UUID of the owner of the entity.
     *
     * @return the owner UUID, or {@code null} if the entity has no owner
     */
    @Override
    public @Nullable UUID ownerUUID() {
        return this.entity().getOwnerUUID();
    }

    /**
     * Checks if the entity is in a battle.
     *
     * @return {@code true} if the entity is battling
     */
    @Override
    public boolean isInBattle() {
        return this.entity().isBattling();
    }

    /**
     * Gets the battle of the entity from the {@link BattleRegistry}.
     *
     * @return the battle, or {@code null} if the entity has no battle ID or the registry has no battle with that ID
     */
    @Override
    public @Nullable CobblemonBattle battle() {
        var id = this.entity().getBattleId();
        if (id == null) {
            return null;
        }
        var battle = BattleRegistry.getBattle(id);
        return battle == null ? null : new CobblemonBattle(battle);
    }

    /**
     * Recalls the entity with the recall animation. Does nothing for a wild Pokémon.
     */
    @Override
    public void recall() {
        if (!this.isWild()) {
            this.entity().recallWithAnimation();
        }
    }
}
