package org.itmo.danil.telegramcloudbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class InMemoryChatsRepository implements ChatsRepository {

    private final HashMap<Long, List<Long>> vkToTelegram = new HashMap<>();

    private final HashMap<Long, List<Long>> telegramToVk = new HashMap<>();

    public List<Long> findVkChatByTelegram(Long chatId) {
        return Optional.ofNullable(telegramToVk.get(chatId)).orElse(List.of());
    }

    public List<Long> findTelegramChatByVk(Long chatId) {
        return Optional.ofNullable(vkToTelegram.get(chatId)).orElse(List.of());
    }

    public void save(Long telegramChat, Long vkChat) {
        List<Long> list = vkToTelegram.computeIfAbsent(vkChat, k -> new ArrayList<>());
        list.add(telegramChat);

        list = telegramToVk.computeIfAbsent(telegramChat, k -> new ArrayList<>());
        list.add(vkChat);
    }
}
