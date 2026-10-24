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

public class PixelmonPokemonEntity extends UPokemonEntity<PixelmonEntity> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokemon_entity");

    public PixelmonPokemonEntity(PixelmonEntity entity) {
        super(TYPE, entity);
    }

    @Override
    public PixelmonPokemon pokemon() {
        return new PixelmonPokemon(this.entity().getPokemon());
    }

    @Override
    public @Nullable UUID ownerUUID() {
        return this.entity().getOwnerUUID();
    }

    @Override
    public boolean isInBattle() {
        return this.entity().battleController != null;
    }

    @Override
    public @Nullable PixelmonBattle battle() {
        var battle = this.entity().battleController;
        return battle == null ? null : new PixelmonBattle(battle);
    }

    @Override
    public void recall() {
        if (!this.isWild()) {
            this.entity().retrieve(PokemonRetrievedEvent.Constants.FORCED);
        }
    }
}
