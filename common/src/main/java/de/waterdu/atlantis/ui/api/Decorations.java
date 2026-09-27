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

@FileBackedObject(mod = Atlantis.ID, path = "ui")
public class Decorations implements NamedData {

    private String classpath;
    private Map<Integer, ConfigurableItemStack> decorations;

    private final transient UUID uuid = UUID.randomUUID();
    private transient Set<Button> createdDecorations = null;
    private transient Class<? extends Page> clazz = null;

    public Decorations() {

    }

    public Decorations(String classpath) {
        this(classpath, new HashMap<>());
    }

    public Decorations(String classpath, Map<Integer, ConfigurableItemStack> decorations) {
        this.classpath = classpath;
        this.decorations = decorations;
    }

    public void setChanged() {
        this.createdDecorations = null;
    }

    public String classpath() {
        return this.classpath;
    }

    public Map<Integer, ConfigurableItemStack> decorations() {
        return this.decorations;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setDecorations(Map<Integer, ConfigurableItemStack> decorations) {
        this.decorations = decorations;
    }

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

    public Set<Button> buttons() {
        if (this.createdDecorations == null) {
            this.createdDecorations = new HashSet<>();
            for (var decoration : this.decorations.entrySet()) {
                this.createdDecorations.add(Decoration.from(decoration));
            }
        }
        return this.createdDecorations;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public void setUUID(UUID uuid) {

    }

    @Override
    public String identifier() {
        return this.classpath;
    }

    @Override
    public String name() {
        return this.classpath;
    }

    @Override
    public void setName(String name) {
        this.classpath = name;
    }

    @Override
    public int hashCode() {
        return this.defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.defaultEquals(obj);
    }

    @Override
    public String toString() {
        return "Decorations(" + this.classpath + ")";
    }
}
