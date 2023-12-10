package org.itmo.danil.telegramcloudbot;

import api.longpoll.bots.exceptions.VkApiException;
import com.vk.api.sdk.actions.LongPoll;
import com.vk.api.sdk.actions.Messages;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Message;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

@SpringBootApplication
public class TelegramCloudBotApplication {

    public static final String token = "5118104182:AAG4CDQRaefmgGar8WQtRPOB_D6ndvjUQOQ";

    public static final Integer VK_APP_ID = 51772799;
    public static final String SECRET_KEY = "WucRMx49nD6OWdK6mLWg";
    public static final String REDIRECT_URL = "";

    public static final Long VK_GROUP_ID = 159674190L;

    public static final String ACCESS_TOKEN = "vk1.a.9IOSjsHMbt-0SUD_hQIacplHpbMckVSgcf-0MJJG8SihZX3_kLkM5WNk_vXX2QLSDzHs7vA7Ptd4PUrIvqW19iTQCYbUNiBOjApjSl4nLR4QqNm4JDpSaiIyHl_dZOWP-M2EK53qetOX1tq-0WOe9EJV1CxhAM63eGbQsNMin7o7x412kkFvIV8yySt6uJo0ih7cTnYBjHzQADZXttbpQA";

    public static final String TOKEN2 = "vk1.a.XX-xg3yNg0fN96vuzy7-s1V4TZSCYGkLz5KfjPqrMu_V__X-nB2r2xjuqSYq9So48p7cEekhGZ16lcNoUsLGg4wvUv2XKR-ZYe5UPz0OQ45fYehww1ftilV25kzWkB41ZDAMVTjSxpqbiAHdLyOSv0YrS3nRWRmER0LEGcqea-KQv2wGL_h3Xgx_yu8QV5Zuq5TsS1hb7BJz6dbA4EQLlg";

    public static void main(String[] args) throws TelegramApiException, VkApiException, ClientException, ApiException {
//        SpringApplication.run(TelegramCloudBotApplication.class, args);

        Observer observer = new Observer();

    }

//    @Bean
//    public MasterBot telegramBot() throws TelegramApiException {
//        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
//        MasterBot bot = new MasterBot(token);
//        botsApi.registerBot(bot);
//        return bot;
//    }
//
//    @Bean
//    public VkBot vkBot() throws VkApiException {
//        VkBot vkBot = new VkBot();
//        vkBot.startPolling();
//        return vkBot;
//    }

}
