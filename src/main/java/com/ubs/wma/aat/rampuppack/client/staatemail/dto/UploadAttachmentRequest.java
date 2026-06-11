package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

/**
 * Body of StaatEmail {@code POST /uploadAttachment} — one call per attachment, ahead of
 * {@code /sendEmail}. {@code contentBase64} is the Base64-encoded file content (here: the UTF-8
 * bytes of a retention-pack HTML document).
 */
public record UploadAttachmentRequest(String fileName, String contentBase64) {}
