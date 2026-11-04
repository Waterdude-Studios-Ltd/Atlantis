package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.java.interfaces.UniquelyNamed;

/**
 * Marks data that also has a unique name. A config can find its instances by name.
 */
public interface NamedData extends Data, UniquelyNamed {

    /**
     * @return the unique name of this instance
     */
    String name();

    /**
     * Sets the name of this instance. This does not update the config. See {@link #setNameAndUpdate(String)}.
     *
     * @param name the new name
     */
    void setName(String name);

    /**
     * Use this instead of {@link #setName(String)} on an instance that is in a config.
     * It also updates the name and file name of the instance in the memory of the config.
     */
    default void setNameAndUpdate(String name) {
        var oldName = this.name();
        var oldFilename = this.config().filename(this);
        this.setName(name);
        this.config().updateName(this, oldName, oldFilename);
    }
}
