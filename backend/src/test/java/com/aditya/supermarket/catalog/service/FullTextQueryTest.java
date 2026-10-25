package com.aditya.supermarket.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FullTextQueryTest {

    @Test
    void turnsWordsIntoRequiredPrefixTerms() {
        assertThat(FullTextQuery.toBooleanMode("red shirt")).contains("+red* +shirt*");
    }

    @Test
    void splitsOnPunctuationAndDropsSingleCharacters() {
        assertThat(FullTextQuery.toBooleanMode("  t-shirt  ")).contains("+shirt*");
    }

    @Test
    void stripsBooleanOperatorsTypedByTheUser() {
        assertThat(FullTextQuery.toBooleanMode("red +blue -green ~x \"quoted\" (a)"))
            .contains("+red* +blue* +green* +quoted*");
    }

    @Test
    void returnsEmptyWhenNothingSearchable() {
        assertThat(FullTextQuery.toBooleanMode(null)).isEmpty();
        assertThat(FullTextQuery.toBooleanMode("   ")).isEmpty();
        assertThat(FullTextQuery.toBooleanMode("\"()<>~*")).isEmpty();
    }

    @Test
    void capsNumberOfTerms() {
        String result = FullTextQuery.toBooleanMode("aa bb cc dd ee ff gg hh ii jj").orElseThrow();
        assertThat(result.split(" ")).hasSize(8);
    }

    @Test
    void supportsNonLatinLetters() {
        assertThat(FullTextQuery.toBooleanMode("कुर्ता")).isPresent();
    }
}
