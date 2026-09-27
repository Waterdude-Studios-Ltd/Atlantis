package de.waterdu.atlantis.comm;

import com.google.gson.JsonParser;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UsernameQuery implements Query<String> {

    private AtlantisPlayer pd;
    private final UUID uuid;
    private final CompletableFuture<String> result;

    private UsernameQuery(UUID uuid) {
        this.uuid = uuid;
        this.result = new CompletableFuture<>();
    }

    public static UsernameQuery from(UUID uuid) {
        return new UsernameQuery(uuid);
    }

    public UsernameQuery to(AtlantisPlayer pd) {
        this.pd = pd;
        return this;
    }

    @Override
    public void run() {
        var name = "";
        try {
            var addr = "https://sessionserver.mojang.com/session/minecraft/profile/" + this.uuid.toString().replace("-", "") + "?unsigned=false";
            try (var input = new InputStreamReader(URI.create(addr).toURL().openStream()); var reader = new BufferedReader(input)) {
                name = JsonParser.parseReader(reader).getAsJsonObject().get("name").getAsString();
            }
        } catch (Exception e) {
            AtlantisLogger.warn("Username query failed: {}", e.getMessage());
        }

        if (!name.isEmpty()) {
            if (this.pd != null) {
                this.pd.setNameAndUpdate(name);
                this.pd.save();
            }
            this.result.complete(name);
        } else {
            this.result.complete("");
        }
    }

    @Override
    public CompletableFuture<String> future() {
        return this.result;
    }
}
