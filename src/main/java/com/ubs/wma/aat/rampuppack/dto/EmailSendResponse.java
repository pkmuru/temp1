package com.ubs.wma.aat.rampuppack.dto;

import java.util.List;

/**
 * Result of a live send: one entry per email part (sends split when attachments exceed the
 * per-email size cap). {@code FAILED} parts are retried automatically by the scheduler.
 */
public record EmailSendResponse(List<EmailPartResult> parts) {}
