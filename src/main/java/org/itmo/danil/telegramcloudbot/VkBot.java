package org.itmo.danil.telegramcloudbot;

import api.longpoll.bots.LongPollBot;
import api.longpoll.bots.exceptions.VkApiException;
import api.longpoll.bots.model.events.messages.MessageNew;
import api.longpoll.bots.model.objects.basic.Message;
import api.longpoll.bots.model.objects.basic.User;
import lombok.RequiredArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.itmo.danil.telegramcloudbot.TelegramCloudBotApplication.TOKEN2;

@RequiredArgsConstructor
public class VkBot extends LongPollBot {

    private final Observer observer;

    private final ChatsRepository chatsRepository;

    public static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Override
    public String getAccessToken() {
        return TOKEN2;
    }

    @Override
    public void startPolling() throws VkApiException {
        new Thread(this::startPoll).start();
    }

    private void startPoll() {
        try {
            super.startPolling();
        } catch (VkApiException e) {
            throw new RuntimeException(e);
        }
    }

//    public Integer resolveNewChat(Long chatId) {
//        vk.messages.getConversations()
//                .execute().getResponse()
//                .get
//    }

    public void sendMessage(BasicMessage message) throws VkApiException {
//        if (observer.getVkChatId() == null) {
//            System.err.println("Chat not defined");
//            return;
//        }

        vk.messages.send()
                .setPeerId(message.destination().intValue())
                .setMessage(message.toString())
                .execute();
    }

    @Override
    public void onMessageNew(MessageNew messageNew) {
        try {
            Message message = messageNew.getMessage();
            if (message.hasText()) {
                if (message.getText().equalsIgnoreCase("/ДАЙ ID")) {
                    Integer chatId = message.getPeerId();
                    vk.messages.send()
                            .setPeerId(chatId)
                            .setMessage(String.valueOf(chatId))
                            .execute();
                    return;
                }
                User user = vk.users.get().setUserIds(String.valueOf(message.getFromId())).execute().getResponse().get(0);
                List<Long> chats = chatsRepository.findTelegramChatByVk(message.getPeerId().longValue());
                for (var chatId : chats) {
                    BasicMessage basicMessage = new BasicMessage(user.getFirstName() + " " + user.getLastName(), message.getText(), chatId);
                    observer.sendMessage(basicMessage, true);
                }
            }
        } catch (VkApiException e) {
            e.printStackTrace();
        }
    }
}