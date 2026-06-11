package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

/** Response of StaatEmail {@code POST /sendEmail} (delivery is tracked via {@code smeId}). */
public record StaatSendResponse(
        String smeId,
        String status) {
}
