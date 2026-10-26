package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Rules for a battle started through {@code PokemonDelegate}. Settings a mod doesn't support, as reported by
 * {@code PokemonDelegate#supports}, are ignored.
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

    public static BattleSettings defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public boolean hasLevelCap() {
        return this.levelCap > 0;
    }

    /**
     * Whether Pokémon below the level cap are raised to it for the battle.
     */
    public boolean raiseToCap() {
        return this.raiseToCap;
    }

    public boolean fullHeal() {
        return this.fullHeal;
    }

    public boolean teamPreview() {
        return this.teamPreview;
    }

    /**
     * Seconds the players get to pick their team in the mod's own team selection screen, or {@link #NONE} to skip it.
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

    public UBattleFormat format() {
        return this.format;
    }

    /**
     * The most Pokémon each side brings. Larger teams are cut down to this many.
     */
    public int teamSize() {
        return this.teamSize;
    }

    /**
     * Clause ids from {@code PokemonDelegate#battleClauses()}. Clauses that are enforced in battle are applied to it;
     * team clauses should be checked with {@link UBattleClause#validate} before the battle starts.
     */
    public List<String> clauses() {
        return this.clauses;
    }

    public boolean experience() {
        return this.experience;
    }

    /**
     * Whether the battle uses copies of the teams, so that nothing that happens in it affects the real Pokémon.
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

        private Builder() {}

        public Builder levelCap(int levelCap) {
            this.levelCap = levelCap;
            return this;
        }

        public Builder raiseToCap(boolean raiseToCap) {
            this.raiseToCap = raiseToCap;
            return this;
        }

        public Builder fullHeal(boolean fullHeal) {
            this.fullHeal = fullHeal;
            return this;
        }

        public Builder teamPreview(boolean teamPreview) {
            this.teamPreview = teamPreview;
            return this;
        }

        public Builder teamSelectTime(int teamSelectTime) {
            this.teamSelectTime = teamSelectTime;
            return this;
        }

        public Builder turnTime(int turnTime) {
            this.turnTime = turnTime;
            return this;
        }

        public Builder format(UBattleFormat format) {
            this.format = format;
            return this;
        }

        public Builder teamSize(int teamSize) {
            this.teamSize = teamSize;
            return this;
        }

        public Builder clauses(List<String> clauses) {
            this.clauses = clauses;
            return this;
        }

        public Builder experience(boolean experience) {
            this.experience = experience;
            return this;
        }

        public Builder cloneTeams(boolean cloneTeams) {
            this.cloneTeams = cloneTeams;
            return this;
        }

        public Builder gimmick(@Nullable String gimmick) {
            this.gimmick = gimmick;
            return this;
        }

        public BattleSettings build() {
            return new BattleSettings(this);
        }
    }
}
