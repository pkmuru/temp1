package com.ubs.wma.aat.rampuppack.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the size-based attachment splitting (35 MB Outlook limit → "Part X of N"). */
class EmailSendServiceSplitTest {

    @Test
    void keepsEverythingInOnePartWhenUnderTheCap() {
        List<List<StaatInsightDocument>> parts =
                EmailSendService.splitBySize(List.of(document("A", 10), document("B", 20), document("C", 30)), 100);

        assertThat(parts).hasSize(1);
        assertThat(aceIds(parts.get(0))).containsExactly("A", "B", "C");
    }

    @Test
    void startsANewPartWhenTheCapWouldBeExceeded() {
        List<List<StaatInsightDocument>> parts =
                EmailSendService.splitBySize(List.of(document("A", 40), document("B", 50), document("C", 30)), 100);

        assertThat(parts).hasSize(2);
        assertThat(aceIds(parts.get(0))).containsExactly("A", "B");
        assertThat(aceIds(parts.get(1))).containsExactly("C");
    }

    @Test
    void shipsAnOversizedDocumentAloneInItsOwnPart() {
        List<List<StaatInsightDocument>> parts =
                EmailSendService.splitBySize(List.of(document("A", 10), document("HUGE", 150), document("B", 10)), 100);

        assertThat(parts).hasSize(3);
        assertThat(aceIds(parts.get(0))).containsExactly("A");
        assertThat(aceIds(parts.get(1))).containsExactly("HUGE");
        assertThat(aceIds(parts.get(2))).containsExactly("B");
    }

    private static StaatInsightDocument document(String aceId, int sizeBytes) {
        return new StaatInsightDocument(
                null, aceId, "Client " + aceId, aceId + ".html", "x".repeat(sizeBytes), null, null);
    }

    private static List<String> aceIds(List<StaatInsightDocument> documents) {
        return documents.stream().map(StaatInsightDocument::aceId).toList();
    }
}
