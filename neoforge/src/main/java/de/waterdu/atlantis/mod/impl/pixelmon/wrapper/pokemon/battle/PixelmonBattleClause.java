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

public class PixelmonBattleClause extends UBattleClause<Holder.Reference<BattleRule>> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_clause");

    public PixelmonBattleClause(Holder.Reference<BattleRule> rule) {
        super(TYPE, rule);
    }

    /**
     * The rule's id, without the namespace for Pixelmon's own rules, such as {@code species_clause}.
     */
    @Override
    public String id() {
        var location = this.dereference().key().location();
        return location.getNamespace().equals(Pixelmon.MODID) ? location.getPath() : location.toString();
    }

    @Override
    public Component displayName() {
        var path = this.dereference().key().location().getPath();
        return Component.literal(TextUtils.capitalizeEachWord(path.replace('/', ' ').replace('_', ' ')));
    }

    @Override
    public List<Component> validate(List<? extends UPokemon<?>> team) {
        return this.dereference().value().getFailureReasons(team.stream().map(PixelmonPokemon::unwrap).toList());
    }

    @Override
    public boolean isEnforcedInBattle() {
        return !this.dereference().value().modifiers().isEmpty();
    }
}
