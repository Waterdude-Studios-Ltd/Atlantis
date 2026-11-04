package de.waterdu.atlantis.level.level;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.registry.RegistryInjectEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.jspecify.annotations.Nullable;

/**
 * Describes a custom level that Atlantis registers. It holds the level key and the keys and arguments of its chunk
 * generator and dimension type.
 */
public final class AtlantisLevel {

    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("unknown", "unknown");

    private ConfigurableLevel level;

    private ResourceLocation chunkGenerator = AtlantisLevel.UNKNOWN;
    private String[] chunkGeneratorArgs = new String[0];
    private ResourceLocation dimensionType = AtlantisLevel.UNKNOWN;
    private String[] dimensionTypeArgs = new String[0];

    private transient Holder<LevelStem> levelStemHolder;
    private transient Holder<DimensionType> dimensionTypeHolder;

    /**
     * Creates a level with the key {@code atlantis:example}.
     */
    public AtlantisLevel() {
        this("example");
    }

    /**
     * Creates a level in the {@link Atlantis#ID} namespace.
     *
     * @param key the path of the level key
     */
    public AtlantisLevel(String key) {
        this(Atlantis.ID, key);
    }

    /**
     * Creates a level with the given key.
     *
     * @param namespace the namespace of the level key
     * @param key the path of the level key
     */
    public AtlantisLevel(String namespace, String key) {
        this.level = ConfigurableLevel.of(namespace, key);
    }

    /**
     * Listens for {@link UEvent#REGISTRY_INJECT} to register the dimension type and level stem of this level.
     */
    void register() {
        Mods.modLoader().event(UEvent.REGISTRY_INJECT).listen(this::onRegistryInject);
    }

    /**
     * Registers the dimension type of this level into the dimension type registry, and its level stem into the level
     * stem registry. Other registries are ignored.
     *
     * @param event the registry injection event
     * @return always {@link InteractionResult#PASS}
     * @throws RuntimeException if the chunk generator or the dimension type is not valid
     */
    private InteractionResult onRegistryInject(RegistryInjectEventPayload event) {
        try {
            if (event.isForRegistry(Registries.DIMENSION_TYPE)) {
                var dimensionType = this.makeDimensionType();
                if (dimensionType != null) {
                    this.dimensionTypeHolder = event.register(dimensionType.getA(), dimensionType.getB());
                }
            } else if (event.isForRegistry(Registries.LEVEL_STEM)) {
                var location = this.level.key().location();
                var key = ResourceKey.create(Registries.LEVEL_STEM, location);

                var chunkGenerator = this.makeChunkGenerator(event.lookup(Registries.BIOME).orElseThrow());

                if (chunkGenerator == null) {
                    throw new RuntimeException(new InvalidAtlantisLevelException(this, "ChunkGenerator", this.chunkGenerator.toString()));
                }

                if (this.dimensionTypeHolder == null) {
                    throw new RuntimeException(new InvalidAtlantisLevelException(this, "DimensionType", this.dimensionType.toString()));
                }

                this.levelStemHolder = event.register(key, new LevelStem(this.dimensionTypeHolder, chunkGenerator));
            }
        } catch (InvalidAtlantisLevelException e) {
            throw new RuntimeException(e);
        }
        return InteractionResult.PASS;
    }

    /**
     * @return the configurable level that holds the key of this level
     */
    public ConfigurableLevel asConfigurableLevel() {
        return this.level;
    }

    /**
     * Gets the server level of this level from its {@link ConfigurableLevel}.
     *
     * @return the server level
     */
    public ServerLevel asServerLevel() {
        return this.asConfigurableLevel().get();
    }

    /**
     * Sets the configurable level that holds the key of this level.
     *
     * @param level the new configurable level
     */
    public void setLevel(ConfigurableLevel level) {
        this.level = level;
    }

    /**
     * @return the level key of this level
     */
    public ResourceKey<Level> levelKey() {
        return this.level.key();
    }

    /**
     * @return the location of the level key as a string, with its namespace
     */
    public String id() {
        return this.levelKey().location().toString();
    }

    /**
     * @return null until the level stem registry is injected
     */
    public Holder<LevelStem> levelStem() {
        return this.levelStemHolder;
    }

    /**
     * @return null until the dimension type registry is injected
     */
    public Holder<DimensionType> dimensionType() {
        return this.dimensionTypeHolder;
    }

    /**
     * Creates the dimension type of this level from its dimension type key and arguments.
     *
     * @return the key and the dimension type, or {@code null} if the dimension type key is not registered
     * @throws InvalidAtlantisLevelException if the dimension type function fails
     */
    public @Nullable Pair<ResourceKey<DimensionType>, DimensionType> makeDimensionType() throws InvalidAtlantisLevelException {
        return AtlantisLevelRegistry.getDimensionType(this.dimensionType, this, this.dimensionTypeArgs);
    }

    /**
     * Creates the chunk generator of this level from its chunk generator key and arguments.
     *
     * @param biomes the biome registry info that the chunk generator uses
     * @return the chunk generator, or {@code null} if the chunk generator key is not registered
     * @throws InvalidAtlantisLevelException if the chunk generator function fails
     */
    public @Nullable ChunkGenerator makeChunkGenerator(RegistryOps.RegistryInfo<Biome> biomes) throws InvalidAtlantisLevelException {
        return AtlantisLevelRegistry.getChunkGenerator(this.chunkGenerator, this, biomes, this.chunkGeneratorArgs);
    }

    /**
     * @param obj an {@link AtlantisLevel}, a {@link Level}, a {@link ConfigurableLevel} or a {@link ResourceKey}. Other
     *     objects never match.
     * @return {@code true} if the object has the same level key as this level
     */
    public boolean matches(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AtlantisLevel atlantisLevel) {
            return atlantisLevel.levelKey().equals(this.levelKey());
        }
        if (obj instanceof Level actualLevel) {
            return actualLevel.dimension().equals(this.levelKey());
        }
        if (obj instanceof ConfigurableLevel configurableLevel) {
            return configurableLevel.key().equals(this.levelKey());
        }
        if (obj instanceof ResourceKey<?> key) {
            return key.equals(this.levelKey());
        }
        return false;
    }

    /**
     * Gets the hash code of the level key.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.levelKey().hashCode();
    }

    /**
     * Checks if the object is an {@link AtlantisLevel} with the same level key.
     *
     * @param obj the object to compare
     * @return {@code true} if the levels have the same level key
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AtlantisLevel other)) {
            return false;
        }
        return this.levelKey().equals(other.levelKey());
    }

    /**
     * @return {@code AtlantisLevel[}, the {@link #id()} and {@code ]}
     */
    @Override
    public String toString() {
        return "AtlantisLevel[" + this.id() + "]";
    }

    /**
     * Creates a builder for a new level.
     *
     * @param namespace the namespace of the level key
     * @param key the path of the level key
     * @return the new builder
     */
    public static Builder builder(String namespace, String key) {
        return new Builder(namespace, key);
    }

    /**
     * Builds an {@link AtlantisLevel}.
     */
    public static class Builder {

        private final AtlantisLevel level;

        /**
         * Creates a builder for a level with the given key.
         *
         * @param namespace the namespace of the level key
         * @param key the path of the level key
         */
        private Builder(String namespace, String key) {
            this.level = new AtlantisLevel(namespace, key);
        }

        /**
         * @param chunkGenerator a key from {@link AtlantisLevelRegistry#registerChunkGenerator}
         * @param args the arguments for the chunk generator function
         */
        public Builder chunkGenerator(ResourceLocation chunkGenerator, String... args) {
            this.level.chunkGenerator = chunkGenerator;
            this.level.chunkGeneratorArgs = args;
            return this;
        }

        /**
         * @param dimensionType a key from {@link AtlantisLevelRegistry#registerDimensionType}
         * @param args the arguments for the dimension type function
         */
        public Builder dimensionType(ResourceLocation dimensionType, String... args) {
            this.level.dimensionType = dimensionType;
            this.level.dimensionTypeArgs = args;
            return this;
        }

        /**
         * Sets the chunk generator from a string key.
         *
         * @param chunkGenerator a key from {@link AtlantisLevelRegistry#registerChunkGenerator}, parsed as a
         *     {@link ResourceLocation}
         * @param args the arguments for the chunk generator function
         * @return this builder
         */
        public Builder chunkGenerator(String chunkGenerator, String... args) {
            this.level.chunkGenerator = ResourceLocation.parse(chunkGenerator);
            this.level.chunkGeneratorArgs = args;
            return this;
        }

        /**
         * Sets the dimension type from a string key.
         *
         * @param dimensionType a key from {@link AtlantisLevelRegistry#registerDimensionType}, parsed as a
         *     {@link ResourceLocation}
         * @param args the arguments for the dimension type function
         * @return this builder
         */
        public Builder dimensionType(String dimensionType, String... args) {
            this.level.dimensionType = ResourceLocation.parse(dimensionType);
            this.level.dimensionTypeArgs = args;
            return this;
        }

        /**
         * {@link #build(boolean)} with {@code instantiate} set to {@code true}.
         */
        public AtlantisLevel build() {
            return this.build(true);
        }

        /**
         * @param instantiate adds the level to {@link AtlantisLevelStorage}. The level exists only after a server
         *     restart.
         */
        public AtlantisLevel build(boolean instantiate) {
            if (instantiate) {
                AtlantisLevelStorage.instance().addLevel(this.level);
            }
            return this.level;
        }
    }

    /**
     * Throws an exception when no server runs.
     */
    public static Registry<Biome> biomeRegistry() {
        return ServerUtils.registryAccess().registryOrThrow(Registries.BIOME);
    }
}
