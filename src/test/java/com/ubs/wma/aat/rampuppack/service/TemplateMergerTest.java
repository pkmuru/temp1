package com.ubs.wma.aat.rampuppack.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateMergerTest {

    @Test
    void replacesPlaceholdersIncludingNamesWithSpaces() {
        String merged = TemplateMerger.merge(
                "Hi {FA Name}, you have {packCount} packs.",
                Map.of("FA Name", "Alex Advisor", "packCount", "3"));

        assertThat(merged).isEqualTo("Hi Alex Advisor, you have 3 packs.");
    }

    @Test
    void leavesUnknownPlaceholdersVisible() {
        String merged = TemplateMerger.merge("Hi {faName}, see {unknownField}.",
                Map.of("faName", "Alex"));

        assertThat(merged).isEqualTo("Hi Alex, see {unknownField}.");
    }

    @Test
    void returnsTemplateUnchangedWhenNoFields() {
        assertThat(TemplateMerger.merge("Hi {faName}", Map.of())).isEqualTo("Hi {faName}");
    }

    @Test
    void handlesNullAndEmptyTemplates() {
        assertThat(TemplateMerger.merge(null, Map.of("a", "b"))).isNull();
        assertThat(TemplateMerger.merge("", Map.of("a", "b"))).isEmpty();
    }
}
