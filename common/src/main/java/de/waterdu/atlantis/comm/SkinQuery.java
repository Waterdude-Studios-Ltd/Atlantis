package de.waterdu.atlantis.comm;

import com.google.gson.JsonParser;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinQuery implements Query<Skin> {

    private AtlantisPlayer pd;
    private final UUID uuid;
    private final CompletableFuture<Skin> result;

    private SkinQuery(UUID uuid) {
        this.uuid = uuid;
        this.result = new CompletableFuture<>();
    }

    public static SkinQuery from(UUID uuid) {
        return new SkinQuery(uuid);
    }

    public SkinQuery to(AtlantisPlayer pd) {
        this.pd = pd;
        return this;
    }

    @Override
    public void run() {
        var skin = "";
        var signature = "";
        try {
            var addr = "https://sessionserver.mojang.com/session/minecraft/profile/" + this.uuid.toString().replace("-", "") + "?unsigned=false";
            try (var input = new InputStreamReader(URI.create(addr).toURL().openStream()); var reader = new BufferedReader(input)) {
                var obj = JsonParser.parseReader(reader).getAsJsonObject();
                var properties = obj.get("properties").getAsJsonArray();
                var value = "";
                for (var property : properties) {
                    if (!property.isJsonObject()) {
                        continue;
                    }
                    var propertyObject = property.getAsJsonObject();
                    if (!propertyObject.get("name").getAsString().equalsIgnoreCase("textures")) {
                        continue;
                    }
                    value = propertyObject.get("value").getAsString();
                    signature = propertyObject.get("signature").getAsString();
                    break;
                }
                if (!value.isEmpty()) {
                    obj = JsonParser.parseString(new String(Base64.getDecoder().decode(value))).getAsJsonObject();
                    var skinURL = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                    var skinByte = ("{\"signatureRequired\":" + (signature.isEmpty() ? "false" : "true") + ",\"textures\":{\"SKIN\":{\"url\":\"" + skinURL + "\"}}}").getBytes();
                    skin = new String(Base64.getEncoder().encode(skinByte));
                }
            }
        } catch (Exception e) {
            AtlantisLogger.warn("Skin query failed: {}", e.getMessage());
        }

        if (!skin.isEmpty()) {
            var skinObject = new Skin(skin, signature);
            if (this.pd != null) {
                this.pd.setSkinObject(skinObject);
                this.pd.save();
            }
            this.result.complete(skinObject);
        } else {
            this.result.complete(new Skin());
        }
    }

    @Override
    public CompletableFuture<Skin> future() {
        return this.result;
    }
}
