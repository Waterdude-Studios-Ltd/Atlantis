package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Creates Brigadier suggestion providers.
 */
public final class BrigadierUtils {

    private BrigadierUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Suggests the {@link String#valueOf(Object)} of each value. Does not filter the values by the typed text.
     */
    public static <S> SuggestionProvider<S> suggest(Object... suggestions) {
        return (context, builder) -> {
            for (var suggestion : suggestions) {
                builder.suggest(String.valueOf(suggestion));
            }
            return builder.buildFuture();
        };
    }

    /**
     * Creates a provider that calls the consumer for each element of the array.
     *
     * @param <S> the type of the command source
     * @param <T> the type of the elements
     * @param collection the elements to suggest
     * @param suggestion adds the suggestion for an element to the builder
     * @return the suggestion provider
     */
    public static <S, T> SuggestionProvider<S> suggest(T[] collection, BiConsumer<SuggestionsBuilder, T> suggestion) {
        return BrigadierUtils.suggest(Arrays.asList(collection), suggestion);
    }

    /**
     * Creates a provider that calls the consumer for each element of the collection.
     *
     * @param <S> the type of the command source
     * @param <T> the type of the elements
     * @param collection the elements to suggest
     * @param suggestion adds the suggestion for an element to the builder
     * @return the suggestion provider
     */
    public static <S, T> SuggestionProvider<S> suggest(Collection<T> collection, BiConsumer<SuggestionsBuilder, T> suggestion) {
        return (context, builder) -> {
            for (var element : collection) {
                suggestion.accept(builder, element);
            }
            return builder.buildFuture();
        };
    }
}
