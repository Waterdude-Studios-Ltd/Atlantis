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

/**
 * Holds a particle as text in the format of the {@code /particle} command, together with named number parameters.
 */
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

    /**
     * Creates a particle with no parameters.
     *
     * @param particle a particle in the format of the {@code /particle} command
     */
    private Particle(String particle) {
        this.particle = particle;
    }

    /**
     * @param particle a particle in the format of the {@code /particle} command, for example
     *     {@code dust{color:[1.0,0.0,0.0],scale:1.0}}
     */
    public static Particle of(String particle) {
        return new Particle(particle);
    }

    /**
     * Sets a parameter, for example {@link #SPEED}.
     *
     * @param key the name of the parameter
     * @param value the value of the parameter
     * @return this particle
     */
    public Particle withParameter(String key, double value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * Sets all the given parameters. Other parameters stay.
     *
     * @param parameters the parameters, by name
     * @return this particle
     */
    public Particle withParameters(Map<String, Double> parameters) {
        this.parameters.putAll(parameters);
        return this;
    }

    /**
     * Parses the particle on the first call and caches the result. Parsing fails if no server is running.
     *
     * @return null if parsing fails
     */
    public ParticleOptions data() {
        return this.data.get();
    }

    /**
     * Gets the type of the parsed particle. Throws a {@link NullPointerException} if parsing fails.
     *
     * @return the particle type
     */
    public ParticleType<?> type() {
        return this.data().getType();
    }

    /**
     * @return the particle in the format of the {@code /particle} command
     */
    public String particle() {
        return this.particle;
    }

    /**
     * Gets a parameter.
     *
     * @param key the name of the parameter
     * @return the value, or 0 if the parameter is not set
     */
    public double parameter(String key) {
        return this.parameter(key, 0D);
    }

    /**
     * Gets a parameter.
     *
     * @param key the name of the parameter
     * @param defaultValue the value to give if the parameter is not set
     * @return the value, or {@code defaultValue} if the parameter is not set
     */
    public double parameter(String key, double defaultValue) {
        return this.parameters.getOrDefault(key, defaultValue);
    }

    /**
     * @return the mutable map of the parameters, by name
     */
    public Map<String, Double> parameters() {
        return this.parameters;
    }
}
