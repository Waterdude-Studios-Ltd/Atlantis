package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import org.jspecify.annotations.Nullable;

/**
 * How a trainer battles.
 *
 * @param ai       one of {@code PokemonDelegate#trainerAIs()}, or null for the mod's default
 * @param bossTier one of {@code PokemonDelegate#bossTiers()}, or null for none; ignored where boss tiers aren't supported
 */
public record TrainerSettings(@Nullable String ai, @Nullable String bossTier) {

    private static final TrainerSettings DEFAULTS = new TrainerSettings(null, null);

    public static TrainerSettings defaults() {
        return DEFAULTS;
    }
}
