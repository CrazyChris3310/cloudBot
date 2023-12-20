package org.example.cloud.services;

import api.longpoll.bots.LongPollBot;
import api.longpoll.bots.exceptions.VkApiException;
import api.longpoll.bots.methods.impl.messages.Send;
import api.longpoll.bots.methods.impl.upload.UploadDoc;
import api.longpoll.bots.model.events.messages.MessageNew;
import api.longpoll.bots.model.objects.additional.Image;
import api.longpoll.bots.model.objects.additional.PhotoSize;
import api.longpoll.bots.model.objects.additional.UploadedFile;
import api.longpoll.bots.model.objects.basic.Message;
import api.longpoll.bots.model.objects.basic.User;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.example.cloud.dto.BasicMessage;
import org.example.cloud.dto.MediaContentType;
import org.example.cloud.entity.ChatMapping;
import org.example.cloud.repository.ChatsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class VkBot extends LongPollBot {

    private final ChatsRepository chatsRepository;

    private final MessageService messageService;

    @Value("${vk.bot.token}")
    private String BOT_TOKEN;

    public VkBot(ChatsRepository chatsRepository, MessageService messageService) {
        this.chatsRepository = chatsRepository;
        this.messageService = messageService;
        this.startPolling();
        System.out.println("Vk bot started");
    }

    @Override
    public String getAccessToken() {
        return BOT_TOKEN;
    }

    @Override
    public void startPolling() {
        new Thread(this::startPoll).start();
    }

    private void startPoll() {
        while (true) {
            try {
                super.startPolling();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    @SqsListener("messages_to_vk.fifo")
    public void sendMessage(BasicMessage message) throws VkApiException, IOException {
        Send sendMessage = vk.messages.send()
                .setPeerId(message.getDestination().intValue());
        String text = message.getText();
        boolean hasText = text != null && !text.isEmpty() && !text.equals("null");

        if (message.getType() == MediaContentType.PHOTO) {
            InputStream inputStream = new URL(message.getUrl()).openStream();
            sendMessage = sendMessage
                    .addPhoto("something.jpg", inputStream)
                    .setMessage(hasText ? message.toString() : message.getFullname());
        } else if (message.getType() == MediaContentType.VIDEO) {
            var url = URI.create(message.getUrl()).toURL();
            var response = vk.docs.getMessagesUploadServer().setType("doc").setPeerId(message.getDestination().intValue()).execute().getResponse();
            var file = new UploadDoc(response.getUploadUrl(), url.getFile(), url.openStream()).execute();
            var savedDoc = vk.docs.save().setFile(file.getFile()).execute().getResponse();
            sendMessage = sendMessage
                    .addDoc(URI.create(message.getUrl()).toURL().getFile(), url.openStream())
                    .setMessage(hasText ? message.toString() : message.getFullname());
//                    .setAttachment(new UploadedFile("video", savedDoc.getDoc().getOwnerId(), savedDoc.getDoc().getId(), null));
        } else if (message.getType() == MediaContentType.AUDIO) {
            var url = URI.create(message.getUrl()).toURL();
            var response = vk.docs.getMessagesUploadServer().setType("audio_message").setPeerId(message.getDestination().intValue()).execute().getResponse();
            var file = new UploadDoc(response.getUploadUrl(), url.getFile(), url.openStream()).execute();
            var savedDoc = vk.docs.save().setFile(file.getFile()).execute().getResponse();
            sendMessage = sendMessage
//                    .addDoc(URI.create(message.getUrl()).toURL().getFile(), url.openStream())
                    .setMessage(hasText ? message.toString() : message.getFullname())
                    .setAttachment(new UploadedFile("audio", savedDoc.getAudioMessage().getOwnerId(), savedDoc.getAudioMessage().getId(), savedDoc.getAudioMessage().getAccessKey()));
        } else if (message.getType() == MediaContentType.DOC) {
            URL url = URI.create(message.getUrl()).toURL();
            sendMessage = sendMessage
                    .addDoc(url.getFile(), url.openStream())
                    .setMessage(hasText ? message.toString() : message.getFullname());
        }

        else if (message.getType() == MediaContentType.TEXT && hasText) {
            sendMessage = sendMessage.setMessage(message.toString());
        }

        sendMessage.execute();
    }

    @Override
    public void onMessageNew(MessageNew messageNew) {
        try {
            Message message = messageNew.getMessage();
            List<BasicMessage> messagesToSend = new ArrayList<>();
            User user = vk.users.get().setUserIds(String.valueOf(message.getFromId())).execute().getResponse().get(0);
            if (message.hasText()) {
                if (message.getText().equalsIgnoreCase("/help")) {
                    String helpMessage = "/help - помощь\n" +
                            "/say_id - узнать id текущего чата\n" +
                            "/list_chats - список синхронизированных чатов\n" +
                            "/unsync tg_chatId - отключать отслеживание чата";
                    vk.messages.send()
                            .setPeerId(message.getPeerId())
                            .setMessage(helpMessage)
                            .execute();
                } else if (message.getText().equalsIgnoreCase("/say_id")) {
                    Integer chatId = message.getPeerId();
                    vk.messages.send()
                            .setPeerId(chatId)
                            .setMessage(String.valueOf(chatId))
                            .execute();
                } else if (message.getText().equalsIgnoreCase("/list_chats")) {
                    List<Long> telegramIds = chatsRepository.findTelegramChatByVkChat(message.getPeerId().longValue());
                    String text = telegramIds.isEmpty() ? "No chats are synced" :
                            telegramIds.stream().map(String::valueOf).collect(Collectors.joining("\n"));
                    vk.messages.send()
                            .setPeerId(message.getPeerId())
                            .setMessage(text)
                            .execute();
                } else if (message.getText().startsWith("/unsync")) {
                    String[] args = message.getText().split(" ");
                    for (int i = 1; i < args.length; ++i) {
                        try {
                            ChatMapping chat = chatsRepository.findByVkChatAndTelegramChat(message.getPeerId().longValue(), Long.parseLong(args[i]));
                            chatsRepository.delete(chat);
                        } catch (Exception ignored) {}
                    }
                } else {
                    BasicMessage basicMessage = new BasicMessage();
                    basicMessage.setType(MediaContentType.TEXT);
                    basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                    basicMessage.setText(message.getText());
                    messagesToSend.add(basicMessage);
                }
            }
            if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                for (var attachment : message.getAttachments()) {
                    if (attachment.getPhoto() != null) {
                        Optional<PhotoSize> photo = attachment.getPhoto().getPhotoSizes().stream()
                                .filter(it -> it.getType().equals("z"))
                                .findAny();
                        if (photo.isEmpty()) {
                            photo = attachment.getPhoto().getPhotoSizes().stream()
                                    .filter(it -> it.getType().equals("y"))
                                    .findAny();
                        }
                        if (photo.isEmpty()) {
                            photo = attachment.getPhoto().getPhotoSizes().stream()
                                    .filter(it -> it.getType().equals("x"))
                                    .findAny();
                        }
                        if (photo.isPresent()) {
                            BasicMessage basicMessage = new BasicMessage();
                            basicMessage.setType(MediaContentType.PHOTO);
                            basicMessage.setUrl(photo.get().getSrc());
                            basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                            messagesToSend.add(basicMessage);
                        }
                    } else if (attachment.getAudioMessage() != null) {
                        String linkMp3 = attachment.getAudioMessage().getLinkMp3();
                        BasicMessage basicMessage = new BasicMessage();
                        basicMessage.setType(MediaContentType.AUDIO);
                        basicMessage.setUrl(linkMp3);
                        basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                        messagesToSend.add(basicMessage);
                    } else if (attachment.getAudio() != null) {
                        String url = attachment.getAudio().getUrl();
                        BasicMessage basicMessage = new BasicMessage();
                        basicMessage.setType(MediaContentType.AUDIO);
                        basicMessage.setUrl(url);
                        basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                        messagesToSend.add(basicMessage);
                    } else if (attachment.getSticker() != null) {
                        Image image = attachment.getSticker().getImages().stream().max(
                                Comparator.comparingInt(a -> a.getHeight() * a.getWidth())).get();
                        BasicMessage basicMessage = new BasicMessage();
                        basicMessage.setType(MediaContentType.PHOTO);
                        basicMessage.setUrl(image.getUrl());
                        basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                        messagesToSend.add(basicMessage);
                    } else if (attachment.getDoc() != null) {
                        String url = attachment.getDoc().getUrl();
                        BasicMessage basicMessage = new BasicMessage();
                        basicMessage.setType(MediaContentType.DOC);
                        basicMessage.setUrl(url);
                        basicMessage.setFullname(user.getFirstName() + " " + user.getLastName());
                        messagesToSend.add(basicMessage);
                    }
                }
            }

            List<Long> chats = chatsRepository.findTelegramChatByVkChat(message.getPeerId().longValue());
            for (var chatId : chats) {
                for (var msg : messagesToSend) {
                    msg.setDestination(chatId);
                    messageService.send(msg);
                }
            }
        } catch (VkApiException e) {
            e.printStackTrace();
        }
    }

}