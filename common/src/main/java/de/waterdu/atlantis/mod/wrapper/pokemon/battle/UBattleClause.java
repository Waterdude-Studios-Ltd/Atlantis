package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class UBattleClause<T> extends ModWrapper<T> {

    protected UBattleClause(ModObjectKey type, T clause) {
        super(new ModObject(type, clause));
    }

    public abstract String id();

    public abstract Component displayName();

    /**
     * Checks a team against the clause, returning a reason for each way it breaks it. An empty list means it passes.
     */
    public abstract List<Component> validate(List<? extends UPokemon<?>> team);

    /**
     * Whether the clause is also enforced during the battle, such as a sleep clause, rather than only on the team.
     */
    public abstract boolean isEnforcedInBattle();
}
