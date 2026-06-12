package com.ubs.wma.aat.rampuppack.client.staatemail;

import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendRequest;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendResponse;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
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

    /**
     * Standard {@code multipart/form-data} file upload — one call per attachment, ahead of
     * {@code /sendEmail}. The part is named {@code file}; the attachment's file name and content
     * type travel in the part headers (so the {@link Resource} must report a file name).
     */
    @PostExchange(value = "/uploadAttachment", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<UploadAttachmentResponse> uploadAttachment(@RequestPart("file") Resource file);

    @PostExchange("/sendEmail")
    Mono<StaatSendResponse> sendEmail(@RequestBody StaatSendRequest request);
}
