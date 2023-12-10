package org.itmo.danil.telegramcloudbot;

public record ChatCreationRequest(String hash, Long timestamp, Long vkChat) {
}
