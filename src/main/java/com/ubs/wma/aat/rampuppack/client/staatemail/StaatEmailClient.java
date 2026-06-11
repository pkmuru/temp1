package com.ubs.wma.aat.rampuppack.client.staatemail;

import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendRequest;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendResponse;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentRequest;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

/**
 * Declarative HTTP client for <strong>StaatEmail</strong> — the firm-wide email REST service
 * (Microsoft Entra ID / SPN secured), used for delivery tracking and engagement metrics.
 *
 * <p>Sending is a two-step protocol: upload each attachment ({@link #uploadAttachment}) to obtain
 * an {@code attachmentReferenceId}, then {@link #sendEmail} referencing those ids. Backed by a
 * {@code WebClient} that attaches this service's SPN Bearer token (see {@code StaatEmailConfig});
 * callers do not deal with tokens.
 */
@HttpExchange
public interface StaatEmailClient {

    @PostExchange("/uploadAttachment")
    Mono<UploadAttachmentResponse> uploadAttachment(@RequestBody UploadAttachmentRequest request);

    @PostExchange("/sendEmail")
    Mono<StaatSendResponse> sendEmail(@RequestBody StaatSendRequest request);
}
