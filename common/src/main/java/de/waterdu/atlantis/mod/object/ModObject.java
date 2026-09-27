package de.waterdu.atlantis.mod.object;

public record ModObject(ModObjectKey type, Object reference) implements ModType {

    @SuppressWarnings("unchecked")
    public <T> T dereference() {
        return (T) this.reference;
    }

    @SuppressWarnings("unchecked")
    public <T> T dereference(Class<T> clazz) {
        return (T) this.reference;
    }
}
