package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wraps a battle clause of the mod, such as a species clause.
 *
 * @param <T> the type of the wrapped clause
 */
public abstract class UBattleClause<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for the clause.
     *
     * @param type the type of the wrapper
     * @param clause the clause to wrap
     */
    protected UBattleClause(ModObjectKey type, T clause) {
        super(new ModObject(type, clause));
    }

    /**
     * @return the id of the clause
     */
    public abstract String id();

    /**
     * @return the name of the clause to show to players
     */
    public abstract Component displayName();

    /**
     * Checks a team against the clause. Returns one reason for each way that the team breaks the clause. An empty list
     * means that the team passes.
     */
    public abstract List<Component> validate(List<? extends UPokemon<?>> team);

    /**
     * Whether the clause also applies during the battle, and not only to the team. A sleep clause is an example.
     */
    public abstract boolean isEnforcedInBattle();
}
