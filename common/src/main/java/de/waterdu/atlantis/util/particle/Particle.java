package de.waterdu.atlantis.util.particle;

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Particle {

    public static final Type PARAMETERS_MAP_TYPE = TypeToken.getParameterized(Map.class, String.class, Double.class).getType();

    public static final String RED = "red";
    public static final String GREEN = "green";
    public static final String BLUE = "blue";
    public static final String SPEED = "speed";
    public static final String RADIUS = "radius";

    private final String particle;
    private final Map<String, Double> parameters = new HashMap<>();

    private final Lazy<ParticleOptions> data = Lazy.of(() -> {
        try {
            return ParticleArgument.readParticle(new StringReader(this.particle()), ServerUtils.registryAccess());
        } catch (CommandSyntaxException e) {
            AtlantisLogger.error("Failed to get particle from string {}", this.particle());
            return null;
        }
    });

    private Particle(String particle) {
        this.particle = particle;
    }

    public static Particle of(String particle) {
        return new Particle(particle);
    }

    public Particle withParameter(String key, double value) {
        this.parameters.put(key, value);
        return this;
    }

    public Particle withParameters(Map<String, Double> parameters) {
        this.parameters.putAll(parameters);
        return this;
    }

    public ParticleOptions data() {
        return this.data.get();
    }

    public ParticleType<?> type() {
        return this.data().getType();
    }

    public String particle() {
        return this.particle;
    }

    public double parameter(String key) {
        return this.parameter(key, 0D);
    }

    public double parameter(String key, double defaultValue) {
        return this.parameters.getOrDefault(key, defaultValue);
    }

    public Map<String, Double> parameters() {
        return this.parameters;
    }
}
