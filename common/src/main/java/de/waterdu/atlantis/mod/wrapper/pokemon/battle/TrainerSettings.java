package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import org.jspecify.annotations.Nullable;

/**
 * How a trainer battles.
 *
 * @param ai one of {@code PokemonDelegate#trainerAIs()}, or null for the mod's default
 * @param bossTier one of {@code PokemonDelegate#bossTiers()}, or null for none; ignored where boss tiers are not
 *     supported
 */
public record TrainerSettings(@Nullable String ai, @Nullable String bossTier) {

    private static final TrainerSettings DEFAULTS = new TrainerSettings(null, null);

    /**
     * @return the settings with the mod's default AI and no boss tier
     */
    public static TrainerSettings defaults() {
        return DEFAULTS;
    }
}
