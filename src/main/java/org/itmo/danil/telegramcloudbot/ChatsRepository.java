package org.itmo.danil.telegramcloudbot;

import java.util.List;

public interface ChatsRepository {

    List<Long> findVkChatByTelegram(Long chatId);

    List<Long> findTelegramChatByVk(Long chatId);
    void save(Long telegramChat, Long vkChat);

}
