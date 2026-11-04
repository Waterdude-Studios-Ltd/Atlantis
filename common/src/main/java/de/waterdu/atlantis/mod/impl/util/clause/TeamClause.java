package de.waterdu.atlantis.mod.impl.util.clause;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleClause;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.item.ItemUtils;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Battle clauses written against the wrappers, for Pokémon mods that do not have their own clause system. Showdown
 * also enforces a clause that has a Showdown rule during the battle.
 */
public final class TeamClause extends UBattleClause<String> {

    public static final ModObjectKey TYPE = new ModObjectKey(Atlantis.ID, "team_clause");

    public static final List<TeamClause> DEFAULTS = List.of(
            new TeamClause("species_clause", "Species Clause", "Species Clause", TeamClause::species),
            new TeamClause("item_clause", "Item Clause", "Item Clause", TeamClause::items),
            new TeamClause("ohko_clause", "OHKO Clause", "OHKO Clause", moves("fissure", "guillotine", "horndrill", "sheercold")),
            new TeamClause("evasion_clause", "Evasion Clause", "Evasion Moves Clause", moves("doubleteam", "minimize")),
            new TeamClause("baton_pass_clause", "Baton Pass Clause", "Baton Pass Clause", moves("batonpass")),
            new TeamClause("moody_clause", "Moody Clause", "Moody Clause", abilities("moody")),
            new TeamClause("sleep_clause", "Sleep Clause", "Sleep Clause Mod", null),
            new TeamClause("freeze_clause", "Freeze Clause", "Freeze Clause Mod", null),
            new TeamClause("endless_battle_clause", "Endless Battle Clause", "Endless Battle Clause", null)
    );

    private final String name;
    private final @Nullable String showdownRule;
    private final @Nullable Function<List<? extends UPokemon<?>>, List<Component>> validator;

    /**
     * Creates a team clause.
     *
     * @param id the ID of the clause
     * @param name the display name of the clause
     * @param showdownRule the Showdown rule name, or {@code null} if Showdown has no matching rule
     * @param validator returns a message for each problem in a team, or {@code null} if the clause does not check
     *     teams
     */
    private TeamClause(String id, String name, @Nullable String showdownRule, @Nullable Function<List<? extends UPokemon<?>>, List<Component>> validator) {
        super(TYPE, id);
        this.name = name;
        this.showdownRule = showdownRule;
        this.validator = validator;
    }

    /**
     * @return the ID of this clause, such as {@code species_clause}
     */
    @Override
    public String id() {
        return this.dereference();
    }

    /**
     * @return the name of this clause as a literal component
     */
    @Override
    public Component displayName() {
        return Component.literal(this.name);
    }

    /**
     * Checks the team with the validator of this clause.
     *
     * @param team the team to check
     * @return a message for each problem, or an empty list if this clause has no validator
     */
    @Override
    public List<Component> validate(List<? extends UPokemon<?>> team) {
        return this.validator == null ? List.of() : this.validator.apply(team);
    }

    /**
     * Checks if Showdown enforces this clause during the battle.
     *
     * @return {@code true} if this clause has a Showdown rule
     */
    @Override
    public boolean isEnforcedInBattle() {
        return this.showdownRule != null;
    }

    /**
     * The Showdown rule name, such as {@code Sleep Clause Mod}, or null if Showdown has no matching rule.
     */
    public @Nullable String showdownRule() {
        return this.showdownRule;
    }

    /**
     * Converts the text to lower case and removes every character that is not a letter or a digit.
     *
     * @param text the text to normalize
     * @return the normalized text
     */
    private static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Checks that no two Pokémon in the team have the same Pokédex number.
     *
     * @param team the team to check
     * @return a message for each Pokémon whose species is already in the team
     */
    private static List<Component> species(List<? extends UPokemon<?>> team) {
        var seen = new HashSet<Integer>();
        var failures = new ArrayList<Component>();
        for (var pokemon : team) {
            if (!seen.add(pokemon.species().dex())) {
                failures.add(Component.literal("Only one " + pokemon.species().displayName().getString() + " is allowed."));
            }
        }
        return failures;
    }

    /**
     * Checks that no two Pokémon in the team hold the same item. Pokémon without an item are ignored.
     *
     * @param team the team to check
     * @return a message for each held item that another Pokémon in the team already holds
     */
    private static List<Component> items(List<? extends UPokemon<?>> team) {
        var seen = new HashSet<String>();
        var failures = new ArrayList<Component>();
        for (var pokemon : team) {
            var item = pokemon.heldItem();
            if (!item.isEmpty() && !seen.add(ItemUtils.id(item.getItem()))) {
                failures.add(Component.literal("Only one " + item.getHoverName().getString() + " is allowed."));
            }
        }
        return failures;
    }

    /**
     * Creates a validator that rejects Pokémon that know a banned move.
     *
     * @param banned the normalized names of the banned moves, such as {@code batonpass}
     * @return the validator, which returns a message for each banned move in the team
     */
    private static Function<List<? extends UPokemon<?>>, List<Component>> moves(String... banned) {
        var set = Set.of(banned);
        return team -> {
            var failures = new ArrayList<Component>();
            for (var pokemon : team) {
                for (var move : pokemon.moveset().moves()) {
                    if (set.contains(normalise(move.name()))) {
                        failures.add(Component.literal(pokemon.displayName().getString() + " can't use " + move.template().displayName().getString() + "."));
                    }
                }
            }
            return failures;
        };
    }

    /**
     * Creates a validator that rejects Pokémon that have a banned ability.
     *
     * @param banned the normalized names of the banned abilities, such as {@code moody}
     * @return the validator, which returns a message for each Pokémon with a banned ability
     */
    private static Function<List<? extends UPokemon<?>>, List<Component>> abilities(String... banned) {
        var set = Set.of(banned);
        return team -> {
            var failures = new ArrayList<Component>();
            for (var pokemon : team) {
                if (set.contains(normalise(pokemon.ability().name()))) {
                    failures.add(Component.literal(pokemon.displayName().getString() + " can't have " + pokemon.ability().displayName().getString() + "."));
                }
            }
            return failures;
        };
    }
}
