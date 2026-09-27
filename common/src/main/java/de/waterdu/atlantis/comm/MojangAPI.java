package de.waterdu.atlantis.comm;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.AtlantisData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MojangAPI {

    /**
     * Single point of access for requesting something from the Mojang API.
     * Uses {@link Query}, a type-bounded {@link Runnable}.
     * <p>
     * Available default queries:
     * {@link SkinQuery}, returns a {@link Skin}.
     * {@link UsernameQuery}, returns a {@link String}.
     * {@link UUIDQuery}, returns a {@link UUID}.
     * <p>
     * Information obtained this way is also automatically cached by Atlantis. See {@link AtlantisData}.
     *
     * @param query The {@link Query} to submit to Mojang.
     * @return A future which will be completed with the result of the {@link Query}.
     * If nothing comes back, null will be returned.
     * @param <T> The type returned by the {@link Query} used.
     */
    public static <T> CompletableFuture<T> query(Query<T> query) {
        Atlantis.THREAD_POOL.submit(query);
        return query.future();
    }
}
