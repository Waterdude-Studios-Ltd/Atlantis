package de.waterdu.atlantis.util.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up player skins by name through the server's Mojang session service, off the main thread.
 */
public final class PlayerSkins {

    private static final Map<String, Skin> CACHE = new ConcurrentHashMap<>();

    private PlayerSkins() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Finds the skin of the player with this name. The future completes with null if there is no such player or
     * the lookup fails, and always completes off the main thread.
     */
    public static CompletableFuture<@Nullable Skin> find(String name) {
        return CompletableFuture.supplyAsync(() -> lookup(name), Atlantis.THREAD_POOL.getPool());
    }

    private static @Nullable Skin lookup(String name) {
        var cached = CACHE.get(name.toLowerCase());
        if (cached != null) {
            return cached;
        }
        var server = ServerUtils.server();
        var result = new Skin[1];
        server.getProfileRepository().findProfilesByNames(new String[]{name}, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                var full = server.getSessionService().fetchProfile(profile.getId(), false);
                if (full == null) {
                    return;
                }
                var skin = server.getSessionService().getTextures(full.profile()).skin();
                if (skin != null) {
                    result[0] = new Skin(skin.getUrl(), "slim".equals(skin.getMetadata("model")));
                }
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
                AtlantisLogger.warn("Unable to find the skin of {}: {}", profileName, exception.getMessage());
            }
        });
        if (result[0] != null) {
            CACHE.put(name.toLowerCase(), result[0]);
        }
        return result[0];
    }

    public record Skin(String url, boolean slim) {

        /**
         * Downloads the skin image. Blocks, so call it off the main thread.
         */
        public byte[] download() throws IOException {
            try (InputStream stream = URI.create(this.url).toURL().openStream()) {
                return stream.readAllBytes();
            }
        }
    }
}
