package org.itmo.danil.telegramcloudbot;

import api.longpoll.bots.exceptions.VkApiException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import static org.itmo.danil.telegramcloudbot.TelegramCloudBotApplication.token;

@Component
public class Observer {

    private final ChatsRepository chatsRepository;

    private final MasterBot telegramBot;

    private final VkBot vkBot;

    @Getter
    @Setter
    private volatile Integer vkChatId = null;

    @Getter
    @Setter
    private volatile String tgChatId = null;

    public Observer() throws TelegramApiException, VkApiException {
        chatsRepository = new InMemoryChatsRepository();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBot = new MasterBot(token, this, chatsRepository);
        botsApi.registerBot(telegramBot);

        vkBot = new VkBot(this, chatsRepository);
        vkBot.startPolling();
        System.out.println("Hello, world!");
    }

        public void sendMessage(BasicMessage basicMessage, boolean toTelegram) throws VkApiException {
            if (toTelegram) {
                telegramBot.sendMessage(basicMessage);
            } else {
                vkBot.sendMessage(basicMessage);
            }
        }
}
