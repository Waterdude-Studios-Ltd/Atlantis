package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Rules for a battle started through {@code PokemonDelegate}. Settings that the mod does not support are ignored.
 * {@code PokemonDelegate#supports} reports which settings the mod supports.
 */
public final class BattleSettings {

    public static final int NONE = -1;

    private static final BattleSettings DEFAULTS = builder().build();

    private final int levelCap;
    private final boolean raiseToCap;
    private final boolean fullHeal;
    private final boolean teamPreview;
    private final int teamSelectTime;
    private final int turnTime;
    private final UBattleFormat format;
    private final int teamSize;
    private final List<String> clauses;
    private final boolean experience;
    private final boolean cloneTeams;
    private final @Nullable String gimmick;

    /**
     * Creates the settings from the values of the builder. The list of clauses is copied.
     *
     * @param builder the builder to copy the values from
     */
    private BattleSettings(Builder builder) {
        this.levelCap = builder.levelCap;
        this.raiseToCap = builder.raiseToCap;
        this.fullHeal = builder.fullHeal;
        this.teamPreview = builder.teamPreview;
        this.teamSelectTime = builder.teamSelectTime;
        this.turnTime = builder.turnTime;
        this.format = builder.format;
        this.teamSize = builder.teamSize;
        this.clauses = List.copyOf(builder.clauses);
        this.experience = builder.experience;
        this.cloneTeams = builder.cloneTeams;
        this.gimmick = builder.gimmick;
    }

    /**
     * @return the settings that a new {@link Builder} builds
     */
    public static BattleSettings defaults() {
        return DEFAULTS;
    }

    /**
     * Creates a builder with the default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder with the values of these settings.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder()
                .levelCap(this.levelCap)
                .raiseToCap(this.raiseToCap)
                .fullHeal(this.fullHeal)
                .teamPreview(this.teamPreview)
                .teamSelectTime(this.teamSelectTime)
                .turnTime(this.turnTime)
                .format(this.format)
                .teamSize(this.teamSize)
                .clauses(this.clauses)
                .experience(this.experience)
                .cloneTeams(this.cloneTeams)
                .gimmick(this.gimmick);
    }

    /**
     * The highest level a Pokémon fights at, or {@link #NONE}. Pokémon above it are lowered to it for the battle.
     */
    public int levelCap() {
        return this.levelCap;
    }

    /**
     * Checks if the battle has a level cap.
     *
     * @return {@code true} if the level cap is more than {@code 0}
     */
    public boolean hasLevelCap() {
        return this.levelCap > 0;
    }

    /**
     * Whether Pokémon below the level cap are raised to it for the battle.
     */
    public boolean raiseToCap() {
        return this.raiseToCap;
    }

    /**
     * @return whether the Pokémon are fully healed for the battle
     */
    public boolean fullHeal() {
        return this.fullHeal;
    }

    /**
     * @return whether the players see the teams of the other side before the battle
     */
    public boolean teamPreview() {
        return this.teamPreview;
    }

    /**
     * Seconds that the players have to select their team in the mod's team selection screen, or {@link #NONE} to skip
     * the screen.
     */
    public int teamSelectTime() {
        return this.teamSelectTime;
    }

    /**
     * Seconds per turn, or {@link #NONE} for no limit.
     */
    public int turnTime() {
        return this.turnTime;
    }

    /**
     * @return the format of the battle
     */
    public UBattleFormat format() {
        return this.format;
    }

    /**
     * The maximum number of Pokémon that each side brings. Larger teams are reduced to this number.
     */
    public int teamSize() {
        return this.teamSize;
    }

    /**
     * Clause ids from {@code PokemonDelegate#battleClauses()}. Clauses that are enforced in battle apply to the battle.
     * Check team clauses with {@link UBattleClause#validate} before the battle starts.
     */
    public List<String> clauses() {
        return this.clauses;
    }

    /**
     * @return whether the Pokémon gain experience from the battle
     */
    public boolean experience() {
        return this.experience;
    }

    /**
     * Whether the battle uses copies of the teams. If true, events in the battle do not affect the real Pokémon.
     */
    public boolean cloneTeams() {
        return this.cloneTeams;
    }

    /**
     * One of {@code PokemonDelegate#battleGimmicks()}, or null for the mod's default.
     */
    public @Nullable String gimmick() {
        return this.gimmick;
    }

    /**
     * Builds {@link BattleSettings}. Get one with {@link BattleSettings#builder()} or {@link BattleSettings#toBuilder()}.
     */
    public static final class Builder {

        private int levelCap = NONE;
        private boolean raiseToCap = false;
        private boolean fullHeal = false;
        private boolean teamPreview = false;
        private int teamSelectTime = NONE;
        private int turnTime = NONE;
        private UBattleFormat format = UBattleFormat.SINGLES;
        private int teamSize = 6;
        private List<String> clauses = List.of();
        private boolean experience = true;
        private boolean cloneTeams = false;
        private @Nullable String gimmick = null;

        /**
         * Creates a builder with the default values.
         */
        private Builder() {}

        /**
         * Sets the level cap.
         *
         * @param levelCap the highest level a Pokémon fights at, or {@link #NONE}
         * @return this builder
         */
        public Builder levelCap(int levelCap) {
            this.levelCap = levelCap;
            return this;
        }

        /**
         * Sets whether Pokémon below the level cap are raised to it.
         *
         * @param raiseToCap {@code true} to raise Pokémon to the level cap
         * @return this builder
         */
        public Builder raiseToCap(boolean raiseToCap) {
            this.raiseToCap = raiseToCap;
            return this;
        }

        /**
         * Sets whether the Pokémon are fully healed for the battle.
         *
         * @param fullHeal {@code true} to fully heal the Pokémon
         * @return this builder
         */
        public Builder fullHeal(boolean fullHeal) {
            this.fullHeal = fullHeal;
            return this;
        }

        /**
         * Sets whether the players see the teams of the other side before the battle.
         *
         * @param teamPreview {@code true} to show the teams
         * @return this builder
         */
        public Builder teamPreview(boolean teamPreview) {
            this.teamPreview = teamPreview;
            return this;
        }

        /**
         * Sets the time to select a team.
         *
         * @param teamSelectTime the time in seconds, or {@link #NONE} to skip the team selection screen
         * @return this builder
         */
        public Builder teamSelectTime(int teamSelectTime) {
            this.teamSelectTime = teamSelectTime;
            return this;
        }

        /**
         * Sets the time per turn.
         *
         * @param turnTime the time in seconds, or {@link #NONE} for no limit
         * @return this builder
         */
        public Builder turnTime(int turnTime) {
            this.turnTime = turnTime;
            return this;
        }

        /**
         * Sets the format of the battle.
         *
         * @param format the format
         * @return this builder
         */
        public Builder format(UBattleFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the maximum number of Pokémon that each side brings.
         *
         * @param teamSize the maximum number of Pokémon
         * @return this builder
         */
        public Builder teamSize(int teamSize) {
            this.teamSize = teamSize;
            return this;
        }

        /**
         * Sets the clauses of the battle.
         *
         * @param clauses the clause ids
         * @return this builder
         */
        public Builder clauses(List<String> clauses) {
            this.clauses = clauses;
            return this;
        }

        /**
         * Sets whether the Pokémon gain experience from the battle.
         *
         * @param experience {@code true} to give experience
         * @return this builder
         */
        public Builder experience(boolean experience) {
            this.experience = experience;
            return this;
        }

        /**
         * Sets whether the battle uses copies of the teams.
         *
         * @param cloneTeams {@code true} to use copies of the teams
         * @return this builder
         */
        public Builder cloneTeams(boolean cloneTeams) {
            this.cloneTeams = cloneTeams;
            return this;
        }

        /**
         * Sets the battle gimmick.
         *
         * @param gimmick one of {@code PokemonDelegate#battleGimmicks()}, or {@code null} for the mod's default
         * @return this builder
         */
        public Builder gimmick(@Nullable String gimmick) {
            this.gimmick = gimmick;
            return this;
        }

        /**
         * Creates the settings from the values of this builder.
         *
         * @return the new settings
         */
        public BattleSettings build() {
            return new BattleSettings(this);
        }
    }
}
