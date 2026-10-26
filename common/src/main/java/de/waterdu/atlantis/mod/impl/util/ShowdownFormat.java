package de.waterdu.atlantis.mod.impl.util;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveset;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads and writes the Pokémon Showdown team text format through the {@link PokemonDelegate} wrappers.
 * <p>
 * Based on the team import and export in <a href="https://github.com/smogon/pokemon-showdown">Pokémon Showdown</a>,
 * Copyright Smogon, issued under <a href="https://github.com/smogon/pokemon-showdown/blob/master/LICENSE">The MIT License</a>.
 */
public final class ShowdownFormat {

    private static final String[] STAT_NAMES = {"HP", "Atk", "Def", "SpA", "SpD", "Spe"};
    private static final String[] STATS = {UStat.HP, UStat.ATTACK, UStat.DEFENCE, UStat.SPECIAL_ATTACK, UStat.SPECIAL_DEFENCE, UStat.SPEED};
    private static final int DEFAULT_LEVEL = 100;
    private static final int MAX_IV = 31;
    private static final int MAX_FRIENDSHIP = 255;

    private ShowdownFormat() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Parses every set in the text. Sets whose species can't be found are skipped.
     *
     * @param formAliases maps Showdown forme names to the mod's form names, matched case-insensitively
     */
    public static List<UPokemon<?>> read(PokemonDelegate delegate, String text, Map<String, String> formAliases) {
        var pokemon = new ArrayList<UPokemon<?>>();
        var block = new ArrayList<String>();
        for (var line : text.replace("\r", "").split("\n")) {
            var trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.equals("---") || trimmed.startsWith("===")) {
                read(delegate, block, formAliases, pokemon);
                block.clear();
            } else {
                block.add(trimmed);
            }
        }
        read(delegate, block, formAliases, pokemon);
        return pokemon;
    }

    private static void read(PokemonDelegate delegate, List<String> lines, Map<String, String> formAliases, List<UPokemon<?>> into) {
        if (lines.isEmpty()) {
            return;
        }
        var set = new ShowdownSet();
        set.readHeader(lines.getFirst());
        for (var i = 1; i < lines.size(); i++) {
            set.readLine(lines.get(i));
        }
        var pokemon = set.create(delegate, formAliases);
        if (pokemon != null) {
            into.add(pokemon);
        }
    }

    public static String write(List<? extends UPokemon<?>> team) {
        var out = new StringBuilder();
        for (var pokemon : team) {
            out.append(write(pokemon)).append('\n');
        }
        return out.toString();
    }

    public static String write(UPokemon<?> pokemon) {
        var out = new StringBuilder();
        var name = speciesName(pokemon);
        var nickname = pokemon.nickname();
        if (nickname != null && !nickname.isBlank() && !nickname.equals(pokemon.species().displayName().getString())) {
            out.append(nickname).append(" (").append(name).append(')');
        } else {
            out.append(name);
        }
        if (pokemon.gender().isMale()) {
            out.append(" (M)");
        } else if (pokemon.gender().isFemale()) {
            out.append(" (F)");
        }
        var held = pokemon.heldItem();
        if (!held.isEmpty()) {
            out.append(" @ ").append(held.getHoverName().getString());
        }
        out.append('\n');
        out.append("Ability: ").append(pokemon.ability().displayName().getString()).append('\n');
        if (pokemon.level() != DEFAULT_LEVEL) {
            out.append("Level: ").append(pokemon.level()).append('\n');
        }
        if (pokemon.isShiny()) {
            out.append("Shiny: Yes\n");
        }
        if (pokemon.friendship() != MAX_FRIENDSHIP) {
            out.append("Happiness: ").append(pokemon.friendship()).append('\n');
        }
        out.append("Poke Ball: ").append(pokemon.pokeBall().displayName().getString()).append('\n');
        if (pokemon.hasGigantamaxFactor()) {
            out.append("Gigantamax: Yes\n");
        }
        out.append("EVs: ").append(stats(pokemon.evs())).append('\n');
        out.append(pokemon.effectiveNature().displayName().getString()).append(" Nature\n");
        out.append("IVs: ").append(stats(pokemon.ivs())).append('\n');
        for (var move : pokemon.moveset().moves()) {
            out.append("- ").append(move.template().displayName().getString()).append('\n');
        }
        return out.toString();
    }

    private static String speciesName(UPokemon<?> pokemon) {
        var species = pokemon.species().displayName().getString();
        var form = pokemon.form();
        if (form.name().equalsIgnoreCase(pokemon.species().defaultForm().name()) || form.name().isBlank()) {
            return species;
        }
        return species + "-" + capitalise(form.name());
    }

    private static String stats(UStatsStore<?> store) {
        var parts = new ArrayList<String>();
        var delegate = Mods.pokemon();
        for (var i = 0; i < STATS.length; i++) {
            var stat = delegate.stat(STATS[i]);
            if (stat != null) {
                parts.add(store.get(stat) + " " + STAT_NAMES[i]);
            }
        }
        return String.join(" / ", parts);
    }

    private static String capitalise(String text) {
        var words = text.replace('_', '-').split("-");
        var out = new ArrayList<String>();
        for (var word : words) {
            out.add(word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join("-", out);
    }

    /**
     * Returns {@code text} and its common identifier spellings, such as {@code Swords Dance}, {@code swordsdance}
     * and {@code swords_dance}.
     */
    public static List<String> variants(String text) {
        var trimmed = text.trim();
        var lower = trimmed.toLowerCase(Locale.ROOT).replace("é", "e");
        var variants = new ArrayList<String>();
        variants.add(trimmed);
        variants.add(lower);
        variants.add(lower.replaceAll("[^a-z0-9]", ""));
        variants.add(lower.replaceAll("[^a-z0-9]+", "_"));
        variants.add(trimmed.replace(" ", ""));
        return variants.stream().distinct().toList();
    }

    public static <T> @Nullable T lookup(Function<String, @Nullable T> finder, String name) {
        for (var variant : variants(name)) {
            var found = finder.apply(variant);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static @Nullable UForm<?> findForm(USpecies<?> species, String forme, Map<String, String> formAliases) {
        var aliased = formAliases.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(forme)).map(Map.Entry::getValue).findFirst().orElse(forme);
        var form = lookup(species::form, aliased);
        if (form != null) {
            return form;
        }
        var wanted = aliased.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (var candidate : species.forms()) {
            if (candidate.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "").equals(wanted)) {
                return candidate;
            }
        }
        return null;
    }

    private static final class ShowdownSet {

        private String species = "";
        private @Nullable String nickname;
        private @Nullable String gender;
        private @Nullable String item;
        private @Nullable String ability;
        private int level = DEFAULT_LEVEL;
        private boolean shiny;
        private int friendship = MAX_FRIENDSHIP;
        private @Nullable String pokeBall;
        private @Nullable String nature;
        private boolean gigantamax;
        private int dynamaxLevel = -1;
        private final Map<String, Integer> evs = new LinkedHashMap<>();
        private final Map<String, Integer> ivs = new LinkedHashMap<>();
        private final List<String> moves = new ArrayList<>();

        private void readHeader(String line) {
            var parts = line.split(" @ ", 2);
            if (parts.length > 1) {
                this.item = parts[1].trim();
            }
            var name = parts[0].trim();
            if (name.endsWith(" (M)")) {
                this.gender = UGender.MALE;
                name = name.substring(0, name.length() - 4).trim();
            } else if (name.endsWith(" (F)")) {
                this.gender = UGender.FEMALE;
                name = name.substring(0, name.length() - 4).trim();
            }
            if (name.endsWith(")") && name.contains("(")) {
                var open = name.lastIndexOf('(');
                this.species = name.substring(open + 1, name.length() - 1).trim();
                this.nickname = name.substring(0, open).trim();
            } else {
                this.species = name;
            }
        }

        private void readLine(String line) {
            if (line.startsWith("Ability: ") || line.startsWith("Trait: ")) {
                this.ability = value(line);
            } else if (line.startsWith("Level: ")) {
                this.level = integer(value(line), this.level);
            } else if (line.startsWith("Shiny: ")) {
                this.shiny = value(line).equalsIgnoreCase("Yes");
            } else if (line.startsWith("Happiness: ")) {
                this.friendship = integer(value(line), this.friendship);
            } else if (line.startsWith("Poke Ball: ") || line.startsWith("Pokeball: ") || line.startsWith("Ball: ")) {
                this.pokeBall = value(line);
            } else if (line.startsWith("Gigantamax: ")) {
                this.gigantamax = value(line).equalsIgnoreCase("Yes");
            } else if (line.startsWith("Dynamax Level: ")) {
                this.dynamaxLevel = integer(value(line), -1);
            } else if (line.startsWith("EVs: ")) {
                readStats(value(line), this.evs);
            } else if (line.startsWith("IVs: ")) {
                readStats(value(line), this.ivs);
            } else if (line.startsWith("- ")) {
                var move = line.substring(2).trim();
                var bracket = move.indexOf('[');
                this.moves.add(bracket > 0 ? move.substring(0, bracket).trim() : move);
            } else if (line.endsWith(" Nature")) {
                this.nature = line.substring(0, line.length() - " Nature".length()).trim();
            }
        }

        private @Nullable UPokemon<?> create(PokemonDelegate delegate, Map<String, String> formAliases) {
            USpecies<?> species = null;
            UForm<?> form = null;
            var direct = lookup(name -> delegate.hasSpecies(name) ? delegate.species(name) : null, this.species);
            if (direct != null) {
                species = direct;
            } else {
                var split = this.species.split("-");
                for (var i = split.length - 1; i > 0 && species == null; i--) {
                    var speciesName = String.join("-", List.of(split).subList(0, i));
                    var found = lookup(name -> delegate.hasSpecies(name) ? delegate.species(name) : null, speciesName);
                    if (found != null) {
                        species = found;
                        form = findForm(found, String.join("-", List.of(split).subList(i, split.length)), formAliases);
                    }
                }
            }
            if (species == null) {
                return null;
            }
            if (form == null && this.gender != null) {
                form = species.form(this.gender);
            }

            var pokemon = species.create(this.level);
            if (form != null) {
                pokemon.setForm(form);
            }
            pokemon.setLevel(this.level);
            if (this.nickname != null && !this.nickname.isBlank()) {
                pokemon.setNickname(this.nickname);
            }
            if (this.gender != null) {
                var gender = delegate.gender(this.gender);
                if (gender != null) {
                    pokemon.setGender(gender);
                }
            }
            pokemon.setShiny(this.shiny);
            pokemon.setFriendship(this.friendship);
            if (this.item != null) {
                var item = delegate.showdownItem(this.item);
                if (!item.isEmpty()) {
                    pokemon.setHeldItem(item);
                }
            }
            if (this.ability != null) {
                var ability = lookup(delegate::ability, this.ability);
                if (ability != null) {
                    pokemon.setAbility(ability);
                }
            }
            if (this.nature != null) {
                var nature = lookup(delegate::nature, this.nature);
                if (nature != null) {
                    pokemon.setNature(nature);
                }
            }
            if (this.pokeBall != null) {
                var ball = lookup(delegate::pokeBall, this.pokeBall);
                if (ball != null) {
                    pokemon.setPokeBall(ball);
                }
            }
            if (this.gigantamax) {
                pokemon.setGigantamaxFactor(true);
            }
            if (this.dynamaxLevel >= 0) {
                pokemon.setDynamaxLevel(this.dynamaxLevel);
            }
            applyStats(delegate, pokemon.evs(), this.evs, 0);
            applyStats(delegate, pokemon.ivs(), this.ivs, MAX_IV);

            var moves = new ArrayList<UMoveTemplate<?>>();
            for (var name : this.moves) {
                var move = lookup(delegate::move, name);
                if (move != null) {
                    moves.add(move);
                }
            }
            if (!moves.isEmpty()) {
                var moveset = pokemon.moveset();
                for (var slot = 0; slot < UMoveset.SIZE; slot++) {
                    moveset.set(slot, slot < moves.size() ? moves.get(slot) : null);
                }
                moveset.restorePP();
            }
            pokemon.heal();
            return pokemon;
        }

        private static void applyStats(PokemonDelegate delegate, UStatsStore<?> store, Map<String, Integer> values, int fallback) {
            for (var i = 0; i < STATS.length; i++) {
                var stat = delegate.stat(STATS[i]);
                if (stat != null) {
                    store.set(stat, values.getOrDefault(STAT_NAMES[i].toLowerCase(Locale.ROOT), fallback));
                }
            }
        }

        private static void readStats(String text, Map<String, Integer> into) {
            for (var part : text.split("/")) {
                var pieces = part.trim().split(" ");
                if (pieces.length == 2) {
                    into.put(pieces[1].trim().toLowerCase(Locale.ROOT), integer(pieces[0], 0));
                }
            }
        }

        private static String value(String line) {
            return line.substring(line.indexOf(':') + 1).trim();
        }

        private static int integer(String text, int fallback) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }

    /**
     * Finds an item by its Showdown name in the given namespaces, such as {@code Choice Scarf} as
     * {@code cobblemon:choice_scarf}.
     */
    public static ItemStack item(String name, String... namespaces) {
        var path = name.trim().toLowerCase(Locale.ROOT).replace("é", "e").replaceAll("[^a-z0-9]+", "_");
        for (var namespace : namespaces) {
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, path));
            if (item.isPresent()) {
                return new ItemStack(item.get());
            }
        }
        return ItemStack.EMPTY;
    }
}
