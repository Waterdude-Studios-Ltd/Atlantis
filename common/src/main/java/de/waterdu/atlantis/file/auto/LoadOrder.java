package de.waterdu.atlantis.file.auto;

/**
 * Sets when a config loads, relative to the other configs of the same mod. Configs load from {@link #FIRST} to
 * {@link #LAST}.
 */
public enum LoadOrder {

    FIRST,
    EARLY,
    NORMAL,
    LATE,
    LAST,
}
