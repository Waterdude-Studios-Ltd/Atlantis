package de.waterdu.atlantis.comm;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.AtlantisData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MojangAPI {

    /**
     * Single point of access for requests to the Mojang API.
     * Uses {@link Query}, a type-bounded {@link Runnable}.
     * <p>
     * Available default queries:
     * {@link SkinQuery}, returns a {@link Skin}.
     * {@link UsernameQuery}, returns a {@link String}.
     * {@link UUIDQuery}, returns a {@link UUID}.
     * <p>
     * Atlantis also caches the information from these requests automatically. See {@link AtlantisData}.
     *
     * @param query The {@link Query} to submit to Mojang.
     * @return A future that completes with the result of the {@link Query}.
     * If nothing comes back, the result is null.
     * @param <T> The type returned by the {@link Query} used.
     */
    public static <T> CompletableFuture<T> query(Query<T> query) {
        Atlantis.THREAD_POOL.submit(query);
        return query.future();
    }
}
