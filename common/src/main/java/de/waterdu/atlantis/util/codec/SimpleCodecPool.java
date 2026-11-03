package de.waterdu.atlantis.util.codec;

import de.waterdu.atlantis.util.config.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds {@link SimpleCodec}s by their ID.
 *
 * @param <T> the common type of the codecs
 */
public class SimpleCodecPool<T> {

    private final Map<Key<? extends T>, SimpleCodec<T>> codecs = new HashMap<>();

    /**
     * Adds the codec. It replaces a codec with the same ID.
     *
     * @param codec the codec to add
     * @return the codec
     */
    public SimpleCodec<T> add(SimpleCodec<T> codec) {
        this.codecs.put(codec.id(), codec);
        return codec;
    }

    /**
     * Gets the codec with the ID.
     *
     * @param id the ID of the codec
     * @return the codec, or an empty optional if the pool has no codec with the ID
     */
    public Optional<SimpleCodec<T>> get(Key<? extends T> id) {
        return Optional.ofNullable(this.codecs.get(id));
    }
}
