package org.itmo.danil.telegramcloudbot;

import api.longpoll.bots.exceptions.VkApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MasterBot extends TelegramLongPollingBot {

    private ChatsRepository chatsRepository;

    private final Observer observer;

    private final Map<Long, ChatCreationRequest> requestMap = new HashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long APPROVE_EXPECTATION_LIMIT = 5*60*1000;

    public MasterBot(String botToken, Observer observer, ChatsRepository chatsRepository) {
        super(botToken);
        this.observer = observer;
        this.chatsRepository = chatsRepository;
    }

    public void handleCommand(Message message) throws JsonProcessingException, VkApiException {
        if (message.getText().startsWith("/setchat ")) {
            String text = message.getText().substring(9);
            String code = UUID.randomUUID().toString();
            requestMap.put(message.getChatId(), new ChatCreationRequest(code, System.currentTimeMillis(), Long.parseLong(text)));
            BasicMessage codeMessage = new BasicMessage("", "A request to sync this chat to telegram was registered\nApproval code: " + code, Long.parseLong(text));
            try {
                observer.sendMessage(codeMessage, false);
            } catch (VkApiException e) {
                var typeRef = new TypeReference<HashMap<String, Object>>(){};
                var map = objectMapper.readValue(e.getMessage(), typeRef);
                Map<String, Object> error = (Map<String, Object>) map.get("error");
                if (error.get("error_code").equals(917)) {
                    sendMessage(new BasicMessage("", "Bot has no access to this chat", message.getChatId()));
                } else {
                    throw e;
                }
            }
//            observer.setVkChatId(Integer.parseInt(text));
//            observer.setTgChatId(String.valueOf(message.getChatId()));
        } else if (message.getText().startsWith("/approve_chat ")) {
            String text = message.getText().substring(14);
            ChatCreationRequest chatCreationRequest = requestMap.get(message.getChatId());
            if (chatCreationRequest != null) {
                if (System.currentTimeMillis() < chatCreationRequest.timestamp() + APPROVE_EXPECTATION_LIMIT) {
                    if (chatCreationRequest.hash().equals(text)) {
                        requestMap.remove(message.getChatId());
                        chatsRepository.save(message.getChatId(), chatCreationRequest.vkChat());
                        sendMessage(new BasicMessage("", "Chats are successfully synced", message.getChatId()));
                    } else {
                        sendMessage(new BasicMessage("", "Invalid approval code", message.getChatId()));
                    }
                } else {
                    requestMap.remove(message.getChatId());
                    sendMessage(new BasicMessage("", "Time limit exceeded", message.getChatId()));
                }
            } else {
                sendMessage(new BasicMessage( "", "No chats are waiting to be synced", message.getChatId()));
            }
        }
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();

        if (msg.isCommand()) {
            handleCommand(msg);
            return;
        }

        List<Long> vkChats = chatsRepository.findVkChatByTelegram(msg.getChatId());
        for (var chatId : vkChats) {
            BasicMessage message = new BasicMessage(user.getFirstName() + " " + user.getLastName(), msg.getText(), chatId);
            observer.sendMessage(message, false);
        }


//        sendText(user.getId(), msg.getText());
    }

    @Override
    public String getBotUsername() {
        return "vkToTelgramBot";
    }

    public void sendMessage(BasicMessage basicMessage) {
//        if (basicMessage.destination() == null) {
//            System.err.println("Chat not defined");
//            return;
//        }
        SendMessage sm = SendMessage.builder()
                .chatId(basicMessage.destination()) //Who are we sending a message to
                .text(basicMessage.toString()).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

}
