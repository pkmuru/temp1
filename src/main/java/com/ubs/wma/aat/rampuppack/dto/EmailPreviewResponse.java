package com.ubs.wma.aat.rampuppack.dto;

import java.util.List;

/**
 * UI-friendly preview of a send request: the same pipeline as a real send (template resolution,
 * insight documents, merge, size-based splitting) but nothing is delivered to StaatEmail and
 * nothing is logged — the merged HTML goes back to the caller instead.
 */
public record EmailPreviewResponse(String recipientEmail, List<EmailPreviewPart> parts) {}
