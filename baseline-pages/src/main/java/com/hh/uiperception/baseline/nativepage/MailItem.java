package com.hh.uiperception.baseline.nativepage;

final class MailItem {
    final String sender;
    final String subject;
    final String preview;
    final String time;

    MailItem(String sender, String subject, String preview, String time) {
        this.sender = sender;
        this.subject = subject;
        this.preview = preview;
        this.time = time;
    }
}
