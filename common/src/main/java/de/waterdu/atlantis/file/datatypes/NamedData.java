package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.java.interfaces.UniquelyNamed;

public interface NamedData extends Data, UniquelyNamed {

    String name();

    void setName(String name);

    default void setNameAndUpdate(String name) {
        var oldName = this.name();
        var oldFilename = this.config().filename(this);
        this.setName(name);
        this.config().updateName(this, oldName, oldFilename);
    }
}
