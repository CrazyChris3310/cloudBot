package org.itmo.danil.telegramcloudbot;

public record BasicMessage(String fullname, String text, Long destination) {

    @Override
    public String toString() {
        if (fullname != null && !fullname.isEmpty()) {
            return fullname + ": " + text;
        } else {
            return text;
        }
    }
}
