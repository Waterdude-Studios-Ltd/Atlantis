package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.entity;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.PokemonRetrievedEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle.PixelmonBattle;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Wraps a Pixelmon Pokémon entity.
 */
public class PixelmonPokemonEntity extends UPokemonEntity<PixelmonEntity> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokemon_entity");

    /**
     * Creates a wrapper for a Pixelmon entity.
     *
     * @param entity the entity to wrap
     */
    public PixelmonPokemonEntity(PixelmonEntity entity) {
        super(TYPE, entity);
    }

    /**
     * @return a new wrapper for the Pokémon of the entity
     */
    @Override
    public PixelmonPokemon pokemon() {
        return new PixelmonPokemon(this.entity().getPokemon());
    }

    /**
     * Gets the UUID of the owner of the entity.
     *
     * @return the UUID, or {@code null} if the entity has no owner
     */
    @Override
    public @Nullable UUID ownerUUID() {
        return this.entity().getOwnerUUID();
    }

    /**
     * Checks if the entity has a battle controller.
     *
     * @return {@code true} if the entity is in a battle
     */
    @Override
    public boolean isInBattle() {
        return this.entity().battleController != null;
    }

    /**
     * Gets the battle of the entity.
     *
     * @return the battle, or {@code null} if the entity is not in a battle
     */
    @Override
    public @Nullable PixelmonBattle battle() {
        var battle = this.entity().battleController;
        return battle == null ? null : new PixelmonBattle(battle);
    }

    /**
     * Forces the entity back into its Poké Ball. Does nothing if the Pokémon is wild.
     */
    @Override
    public void recall() {
        if (!this.isWild()) {
            this.entity().retrieve(PokemonRetrievedEvent.Constants.FORCED);
        }
    }
}
