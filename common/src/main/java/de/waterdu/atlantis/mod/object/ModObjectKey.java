package de.waterdu.atlantis.mod.object;

import net.minecraft.resources.ResourceLocation;

/**
 * Identifies the type of an object from another mod by a resource location.
 *
 * @param key the resource location of the type
 */
public record ModObjectKey(ResourceLocation key) implements ModType {

    /**
     * Creates a key from a namespace and a path.
     *
     * @param namespace the namespace, usually the mod ID
     * @param object the path, which names the object type
     */
    public ModObjectKey(String namespace, String object) {
        this(ResourceLocation.fromNamespaceAndPath(namespace, object));
    }

    /**
     * @return the namespace of the key
     */
    public String namespace() {
        return this.key.getNamespace();
    }

    /**
     * @return the path of the key
     */
    public String object() {
        return this.key.getPath();
    }

    /**
     * @return this key
     */
    @Override
    public ModObjectKey type() {
        return this;
    }
}
