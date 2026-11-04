package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRule;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleClause;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Wraps a registered Pixelmon battle rule.
 */
public class PixelmonBattleClause extends UBattleClause<Holder.Reference<BattleRule>> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_clause");

    /**
     * Creates a wrapper for a Pixelmon battle rule.
     *
     * @param rule the registry holder of the rule to wrap
     */
    public PixelmonBattleClause(Holder.Reference<BattleRule> rule) {
        super(TYPE, rule);
    }

    /**
     * The rule's id. For Pixelmon's own rules, the id has no namespace, for example {@code species_clause}.
     */
    @Override
    public String id() {
        var location = this.dereference().key().location();
        return location.getNamespace().equals(Pixelmon.MODID) ? location.getPath() : location.toString();
    }

    /**
     * Gets a name made from the path of the rule. Slashes and underscores become spaces, and each word is capitalized.
     *
     * @return the display name
     */
    @Override
    public Component displayName() {
        var path = this.dereference().key().location().getPath();
        return Component.literal(TextUtils.capitalizeEachWord(path.replace('/', ' ').replace('_', ' ')));
    }

    /**
     * Checks the team against the rule.
     *
     * @param team the Pixelmon Pokémon to check
     * @return the reasons why the team fails the rule, or an empty list if it passes
     */
    @Override
    public List<Component> validate(List<? extends UPokemon<?>> team) {
        return this.dereference().value().getFailureReasons(team.stream().map(PixelmonPokemon::unwrap).toList());
    }

    /**
     * Checks if the rule has modifiers, which apply during the battle.
     *
     * @return {@code true} if the rule has at least one modifier
     */
    @Override
    public boolean isEnforcedInBattle() {
        return !this.dereference().value().modifiers().isEmpty();
    }
}
