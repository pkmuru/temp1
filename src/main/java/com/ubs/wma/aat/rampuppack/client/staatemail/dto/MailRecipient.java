package com.ubs.wma.aat.rampuppack.client.staatemail.dto;

/** One recipient of a StaatEmail send; {@code type} is the addressing slot (TO/CC/BCC). */
public record MailRecipient(String emailAddress, String type) {

    public static MailRecipient to(String emailAddress) {
        return new MailRecipient(emailAddress, "TO");
    }
}
