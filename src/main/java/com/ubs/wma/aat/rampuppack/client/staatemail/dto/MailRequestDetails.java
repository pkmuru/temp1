package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

import java.util.List;

/** Mail payload of a StaatEmail send: subject, reply-to, HTML content and attachment references. */
public record MailRequestDetails(String subject, String replyTo, String content, List<AttachmentRef> attachments) {}
