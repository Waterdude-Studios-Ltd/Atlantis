package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

public final class BrigadierUtils {

    private BrigadierUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static <S> SuggestionProvider<S> suggest(Object... suggestions) {
        return (context, builder) -> {
            for (var suggestion : suggestions) {
                builder.suggest(String.valueOf(suggestion));
            }
            return builder.buildFuture();
        };
    }

    public static <S, T> SuggestionProvider<S> suggest(T[] collection, BiConsumer<SuggestionsBuilder, T> suggestion) {
        return BrigadierUtils.suggest(Arrays.asList(collection), suggestion);
    }

    public static <S, T> SuggestionProvider<S> suggest(Collection<T> collection, BiConsumer<SuggestionsBuilder, T> suggestion) {
        return (context, builder) -> {
            for (var element : collection) {
                suggestion.accept(builder, element);
            }
            return builder.buildFuture();
        };
    }
}
