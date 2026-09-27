package de.waterdu.atlantis.util.codec;

import de.waterdu.atlantis.util.config.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimpleCodecPool<T> {

    private final Map<Key<? extends T>, SimpleCodec<T>> codecs = new HashMap<>();

    public SimpleCodec<T> add(SimpleCodec<T> codec) {
        this.codecs.put(codec.id(), codec);
        return codec;
    }

    public Optional<SimpleCodec<T>> get(Key<? extends T> id) {
        return Optional.ofNullable(this.codecs.get(id));
    }
}
