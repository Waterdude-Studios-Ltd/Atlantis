package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrigadierUtilsTest {

    private static List<String> suggestions(SuggestionProvider<Object> provider, String input) throws Exception {
        return provider.getSuggestions(null, new SuggestionsBuilder(input, 0)).get().getList().stream().map(Suggestion::getText).toList();
    }

    @Test
    void suggestsEveryValueAsAString() throws Exception {
        assertThat(suggestions(BrigadierUtils.suggest("a", 1, true), "")).containsExactly("1", "a", "true");
    }

    @Test
    void suggestFromCollectionAndArray() throws Exception {
        SuggestionProvider<Object> fromList = BrigadierUtils.suggest(List.of("x", "y"), (builder, value) -> builder.suggest(value + "!"));
        SuggestionProvider<Object> fromArray = BrigadierUtils.suggest(new Integer[]{3, 4}, SuggestionsBuilder::suggest);

        assertThat(suggestions(fromList, "")).containsExactly("x!", "y!");
        assertThat(suggestions(fromArray, "")).containsExactly("3", "4");
    }

    @Test
    void emptySuggestions() throws Exception {
        assertThat(suggestions(BrigadierUtils.suggest(), "")).isEmpty();
    }
}
