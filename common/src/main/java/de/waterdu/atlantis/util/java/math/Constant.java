package de.waterdu.atlantis.util.java.math;

/**
 * Lists mathematical and physical constants, each with the names that refer to it in a formula.
 */
public enum Constant {

    PI(Math.PI, "pi", "π"),
    TAU(Math.PI * 2D, "tau", "τ"),
    E(Math.E, "e"),
    SPEED_OF_LIGHT(299792458, "light"),
    G(9.80665, "g", "gravity"),
    PHI(1.61803398874989484820, "phi", "φ"),
    SQRT2(1.41421356237309504880, "sqrt2", "pythagoras"),
    SQRT3(1.73205080756887729352, "sqrt3", "theodorus"),
    LN2(0.69314718055994530941, "ln2"),
    OMEGA(0.56714329040978387299, "omega", "Ω"),
    ;

    private final double value;
    private final String[] representations;

    /**
     * Creates a constant with its value and names.
     *
     * @param value the value of the constant
     * @param representations the names that refer to the constant
     */
    Constant(double value, String... representations) {
        this.value = value;
        this.representations = representations;
    }

    /**
     * @return the value of the constant
     */
    public double value() {
        return this.value;
    }

    /**
     * @return the names that refer to the constant
     */
    public String[] representations() {
        return this.representations;
    }

    /**
     * @param name one of the representations, in any case
     * @return {@code null} if no constant matches
     */
    public static Constant get(String name) {
        for (var constant : Constant.values()) {
            for (var representation : constant.representations) {
                if (representation.equalsIgnoreCase(name)) {
                    return constant;
                }
            }
        }
        return null;
    }
}
