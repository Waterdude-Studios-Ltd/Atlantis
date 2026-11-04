package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link BrigadierUtils}.
 */
class BrigadierUtilsTest {

    /**
     * Gets the texts of the suggestions that a provider gives for an input. The command context is {@code null}.
     *
     * @param provider the suggestion provider
     * @param input the input to suggest for
     * @return the suggestion texts, in order
     * @throws Exception if the suggestions fail
     */
    private static List<String> suggestions(SuggestionProvider<Object> provider, String input) throws Exception {
        return provider.getSuggestions(null, new SuggestionsBuilder(input, 0)).get().getList().stream().map(Suggestion::getText).toList();
    }

    /**
     * Checks that {@link BrigadierUtils#suggest} suggests each value as a string.
     *
     * @throws Exception if the suggestions fail
     */
    @Test
    void suggestsEveryValueAsAString() throws Exception {
        assertThat(suggestions(BrigadierUtils.suggest("a", 1, true), "")).containsExactly("1", "a", "true");
    }

    /**
     * Checks that {@link BrigadierUtils#suggest} uses the given function to suggest each value of a collection or an
     * array.
     *
     * @throws Exception if the suggestions fail
     */
    @Test
    void suggestFromCollectionAndArray() throws Exception {
        SuggestionProvider<Object> fromList = BrigadierUtils.suggest(List.of("x", "y"), (builder, value) -> builder.suggest(value + "!"));
        SuggestionProvider<Object> fromArray = BrigadierUtils.suggest(new Integer[]{3, 4}, SuggestionsBuilder::suggest);

        assertThat(suggestions(fromList, "")).containsExactly("x!", "y!");
        assertThat(suggestions(fromArray, "")).containsExactly("3", "4");
    }

    /**
     * Checks that {@link BrigadierUtils#suggest} with no values gives no suggestions.
     *
     * @throws Exception if the suggestions fail
     */
    @Test
    void emptySuggestions() throws Exception {
        assertThat(suggestions(BrigadierUtils.suggest(), "")).isEmpty();
    }
}
