package de.waterdu.atlantis.comm;

import com.google.gson.JsonParser;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;
import de.waterdu.atlantis.util.java.UUIDUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gets the UUID of a player name from the Mojang API.
 */
public class UUIDQuery implements Query<UUID> {

    private AtlantisPlayer pd;
    private final String name;
    private final CompletableFuture<UUID> result;

    /**
     * Creates a query for the UUID of a player name.
     *
     * @param name the name of the player
     */
    private UUIDQuery(String name) {
        this.name = name;
        this.result = new CompletableFuture<>();
    }

    /**
     * The query completes with null if the request fails.
     */
    public static UUIDQuery from(String name) {
        return new UUIDQuery(name);
    }

    /**
     * Sets the UUID in the player data and saves it when the request succeeds.
     */
    public UUIDQuery to(AtlantisPlayer pd) {
        this.pd = pd;
        return this;
    }

    /**
     * Requests the profile of the name from the Mojang API and completes the future with its {@code id}.
     */
    @Override
    public void run() {
        UUID uuid = null;
        try {
            var addr = "https://api.mojang.com/users/profiles/minecraft/" + this.name;
            try (var input = new InputStreamReader(URI.create(addr).toURL().openStream()); var reader = new BufferedReader(input)) {
                uuid = UUIDUtils.fromString(JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString());
            }
        } catch (Exception e) {
            AtlantisLogger.warn("UUID query failed: {}", e.getMessage());
        }

        if (uuid != null) {
            if (this.pd != null) {
                this.pd.setUUID(uuid);
                this.pd.save();
            }
            this.result.complete(uuid);
        } else {
            this.result.complete(null);
        }
    }

    /**
     * @return the future that completes with the UUID, or with {@code null} if the request fails
     */
    @Override
    public CompletableFuture<UUID> future() {
        return this.result;
    }
}
