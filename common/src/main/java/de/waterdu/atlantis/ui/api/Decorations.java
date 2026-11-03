package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores the decoration items of a {@link Page} class, by slot index. The files are in the {@code ui} folder of
 * Atlantis.
 */
@FileBackedObject(mod = Atlantis.ID, path = "ui")
public class Decorations implements NamedData {

    private String classpath;
    private Map<Integer, ConfigurableItemStack> decorations;

    private final transient UUID uuid = UUID.randomUUID();
    private transient Set<Button> createdDecorations = null;
    private transient Class<? extends Page> clazz = null;

    /**
     * Creates empty decorations with no class path and no decoration map.
     */
    public Decorations() {

    }

    /**
     * Creates decorations with no items for a page class.
     *
     * @param classpath the fully qualified name of the page class
     */
    public Decorations(String classpath) {
        this(classpath, new HashMap<>());
    }

    /**
     * Creates decorations for a page class.
     *
     * @param classpath the fully qualified name of the page class
     * @param decorations the items, by slot index
     */
    public Decorations(String classpath, Map<Integer, ConfigurableItemStack> decorations) {
        this.classpath = classpath;
        this.decorations = decorations;
    }

    /**
     * Clears the cached buttons, so {@link #buttons()} creates them again.
     */
    public void setChanged() {
        this.createdDecorations = null;
    }

    /**
     * @return the fully qualified name of the {@link Page} class. It must match the exact runtime class of the page.
     */
    public String classpath() {
        return this.classpath;
    }

    /**
     * @return the items, by slot index
     */
    public Map<Integer, ConfigurableItemStack> decorations() {
        return this.decorations;
    }

    /**
     * Sets the page class. It does not clear the cached class from {@link #getClassDef()}.
     *
     * @param classpath the fully qualified name of the page class
     */
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    /**
     * Replaces the items. Call {@link #setChanged()} after this to clear the cached buttons.
     *
     * @param decorations the items, by slot index
     */
    public void setDecorations(Map<Integer, ConfigurableItemStack> decorations) {
        this.decorations = decorations;
    }

    /**
     * @return the page class, or null if the class cannot be loaded. In that case, this method logs an error.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Page> getClassDef() {
        if (this.clazz == null) {
            try {
                this.clazz = (Class<? extends Page>) Class.forName(this.classpath);
            } catch (Exception e) {
                AtlantisLogger.error("Failed to read UIDef for classpath {}: {}", this.classpath, e.getMessage());
            }
        }
        return this.clazz;
    }

    /**
     * Caches the result. Call {@link #setChanged()} after you change the decorations.
     */
    public Set<Button> buttons() {
        if (this.createdDecorations == null) {
            this.createdDecorations = new HashSet<>();
            for (var decoration : this.decorations.entrySet()) {
                this.createdDecorations.add(Decoration.from(decoration));
            }
        }
        return this.createdDecorations;
    }

    /**
     * @return a random UUID that is made when this instance is created. It is not saved.
     */
    @Override
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * Does nothing, because the UUID is not saved.
     *
     * @param uuid the UUID, which is ignored
     */
    @Override
    public void setUUID(UUID uuid) {

    }

    /**
     * @return the fully qualified name of the page class
     */
    @Override
    public String identifier() {
        return this.classpath;
    }

    /**
     * @return the fully qualified name of the page class
     */
    @Override
    public String name() {
        return this.classpath;
    }

    /**
     * Sets the page class.
     *
     * @param name the fully qualified name of the page class
     */
    @Override
    public void setName(String name) {
        this.classpath = name;
    }

    /**
     * Gets a hash code from the UUID.
     *
     * @return the hash code of the UUID
     */
    @Override
    public int hashCode() {
        return this.defaultHashCode();
    }

    /**
     * Compares by UUID.
     *
     * @param obj the object to compare
     * @return {@code true} if the object is this instance, or decorations with the same UUID
     */
    @Override
    public boolean equals(Object obj) {
        return this.defaultEquals(obj);
    }

    /**
     * @return {@code Decorations(}, the class path and {@code )}
     */
    @Override
    public String toString() {
        return "Decorations(" + this.classpath + ")";
    }
}
