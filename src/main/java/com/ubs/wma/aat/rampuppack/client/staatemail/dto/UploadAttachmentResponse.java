package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

/**
 * Response of StaatEmail {@code POST /uploadAttachment}; {@code attachmentReferenceId} is later
 * passed in {@code mailDetails.attachments[].id} of the send request.
 */
public record UploadAttachmentResponse(String fileName, String attachmentReferenceId) {}
