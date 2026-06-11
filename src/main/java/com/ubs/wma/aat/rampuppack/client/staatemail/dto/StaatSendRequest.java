package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

import java.util.List;

/**
 * Body of StaatEmail {@code POST /sendEmail}. {@code smeId} is a unique GUID generated per send
 * (kept in {@code email_log.sme_id} for tracking); {@code parentSmeId} carries the same value.
 * Sender identity ({@code senderGpn}, {@code fromGpn}, addresses) and {@code applicationName}
 * come from configuration ({@code app.staatemail.*}).
 */
public record StaatSendRequest(
        String smeId,
        String parentSmeId,
        String senderGpn,
        String fromGpn,
        String fromAddress,
        String senderAddress,
        List<MailRecipient> recipients,
        String applicationName,
        MailRequestDetails mailDetails) {
}
