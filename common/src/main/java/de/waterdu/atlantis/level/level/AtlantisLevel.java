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
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

public final class AtlantisLevel {

    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("unknown", "unknown");

    private ConfigurableLevel level;

    private ResourceLocation chunkGenerator = AtlantisLevel.UNKNOWN;
    private String[] chunkGeneratorArgs = new String[0];
    private ResourceLocation dimensionType = AtlantisLevel.UNKNOWN;
    private String[] dimensionTypeArgs = new String[0];

    private transient Holder<LevelStem> levelStemHolder;
    private transient Holder<DimensionType> dimensionTypeHolder;

    public AtlantisLevel() {
        this("example");
    }

    public AtlantisLevel(String key) {
        this(Atlantis.ID, key);
    }

    public AtlantisLevel(String namespace, String key) {
        this.level = ConfigurableLevel.of(namespace, key);
    }

    void register() {
        Mods.modLoader().event(UEvent.REGISTRY_INJECT).listen(this::onRegistryInject);
    }

    private InteractionResult onRegistryInject(RegistryInjectEventPayload event) {
        try {
            if (event.isForRegistry(Registries.DIMENSION_TYPE)) {
                this.makeDimensionType().ifPresent(dimensionType ->
                        this.dimensionTypeHolder = event.register(dimensionType.getA(), dimensionType.getB())
                );
            } else if (event.isForRegistry(Registries.LEVEL_STEM)) {
                var location = this.level.key().location();
                var key = ResourceKey.create(Registries.LEVEL_STEM, location);

                var chunkGenerator = this.makeChunkGenerator(event.lookup(Registries.BIOME).orElseThrow());
                var dimensionType = this.makeDimensionType();

                if (chunkGenerator.isEmpty()) {
                    throw new RuntimeException(new InvalidAtlantisLevelException(this, "ChunkGenerator", this.chunkGenerator.toString()));
                }

                if (dimensionType.isEmpty()) {
                    throw new RuntimeException(new InvalidAtlantisLevelException(this, "DimensionType", this.dimensionType.toString()));
                }

                this.levelStemHolder = event.register(key, new LevelStem(this.dimensionTypeHolder, chunkGenerator.get()));
            }
        } catch (InvalidAtlantisLevelException e) {
            throw new RuntimeException(e);
        }
        return InteractionResult.PASS;
    }

    public ConfigurableLevel asConfigurableLevel() {
        return this.level;
    }

    public ServerLevel asServerLevel() {
        return this.asConfigurableLevel().get();
    }

    public void setLevel(ConfigurableLevel level) {
        this.level = level;
    }

    public ResourceKey<Level> levelKey() {
        return this.level.key();
    }

    public String id() {
        return this.levelKey().location().toString();
    }

    public Holder<LevelStem> levelStem() {
        return this.levelStemHolder;
    }

    public Holder<DimensionType> dimensionType() {
        return this.dimensionTypeHolder;
    }

    public Optional<Pair<ResourceKey<DimensionType>, DimensionType>> makeDimensionType() throws InvalidAtlantisLevelException {
        return AtlantisLevelRegistry.getDimensionType(this.dimensionType, this, this.dimensionTypeArgs);
    }

    public Optional<ChunkGenerator> makeChunkGenerator(RegistryOps.RegistryInfo<Biome> biomes) throws InvalidAtlantisLevelException {
        return AtlantisLevelRegistry.getChunkGenerator(this.chunkGenerator, this, biomes, this.chunkGeneratorArgs);
    }

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

    @Override
    public int hashCode() {
        return this.levelKey().hashCode();
    }

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

    @Override
    public String toString() {
        return "AtlantisLevel[" + this.id() + "]";
    }

    public static Builder builder(String namespace, String key) {
        return new Builder(namespace, key);
    }

    public static class Builder {

        private final AtlantisLevel level;

        private Builder(String namespace, String key) {
            this.level = new AtlantisLevel(namespace, key);
        }

        public Builder chunkGenerator(ResourceLocation chunkGenerator, String... args) {
            this.level.chunkGenerator = chunkGenerator;
            this.level.chunkGeneratorArgs = args;
            return this;
        }

        public Builder dimensionType(ResourceLocation dimensionType, String... args) {
            this.level.dimensionType = dimensionType;
            this.level.dimensionTypeArgs = args;
            return this;
        }

        public Builder chunkGenerator(String chunkGenerator, String... args) {
            this.level.chunkGenerator = ResourceLocation.parse(chunkGenerator);
            this.level.chunkGeneratorArgs = args;
            return this;
        }

        public Builder dimensionType(String dimensionType, String... args) {
            this.level.dimensionType = ResourceLocation.parse(dimensionType);
            this.level.dimensionTypeArgs = args;
            return this;
        }

        public AtlantisLevel build() {
            return this.build(true);
        }

        public AtlantisLevel build(boolean instantiate) {
            if (instantiate) {
                AtlantisLevelStorage.instance().addLevel(this.level);
            }
            return this.level;
        }
    }

    public static Registry<Biome> biomeRegistry() {
        return ServerUtils.registryAccess().registryOrThrow(Registries.BIOME);
    }
}
