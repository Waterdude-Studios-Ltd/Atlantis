package de.waterdu.atlantis.mod.object;

import net.minecraft.resources.ResourceLocation;

public record ModObjectKey(ResourceLocation key) implements ModType {

    public ModObjectKey(String namespace, String object) {
        this(ResourceLocation.fromNamespaceAndPath(namespace, object));
    }

    public String namespace() {
        return this.key.getNamespace();
    }

    public String object() {
        return this.key.getPath();
    }

    @Override
    public ModObjectKey type() {
        return this;
    }
}
