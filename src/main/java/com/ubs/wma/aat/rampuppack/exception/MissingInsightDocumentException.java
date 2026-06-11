package com.ubs.wma.aat.rampuppack.exception;

import java.util.List;

/**
 * Thrown when a send request references ACE ids that have no STAAT insight document yet
 * (documents are produced externally by DS/datamesh); mapped to HTTP 422 by
 * {@code GlobalExceptionHandler}. Nothing is sent for the request in that case.
 */
public class MissingInsightDocumentException extends RuntimeException {

    private final List<String> missingAceIds;

    public MissingInsightDocumentException(List<String> missingAceIds) {
        super("No STAAT insight document found for ACE id(s): " + String.join(", ", missingAceIds));
        this.missingAceIds = List.copyOf(missingAceIds);
    }

    public List<String> missingAceIds() {
        return missingAceIds;
    }
}
