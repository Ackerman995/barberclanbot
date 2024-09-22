package com.ivan_degtev.telegrambotforpapablinov.service.impl;

import com.ivan_degtev.telegrambotforpapablinov.config.BotConfig;
import com.ivan_degtev.telegrambotforpapablinov.service.WebhookService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
public class WebhookServiceImpl implements WebhookService {

    private final BotConfig botConfig;
    private final WebClient webClient;

    public WebhookServiceImpl(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.webClient = WebClient.builder().baseUrl("https://api.telegram.org").build();
    }

    public String createWebhook() {
        String url = String.format("/bot%s/setWebhook?url=%s", botConfig.getBotToken(), botConfig.getFullWebhookUrl());

        try {
            String response = webClient.post()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Webhook set response: " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to set webhook";
        }
    }

    public String getAllWebhooks() {
        String url = String.format("/bot%s/getWebhookInfo", botConfig.getBotToken());

        try {
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Webhook info response: " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get webhook info";
        }
    }

    public String deleteWebhook() {
        String url = String.format("/bot%s/deleteWebhook", botConfig.getBotToken());

        try {
            String response = webClient.post()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Webhook delete response: " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to delete webhook";
        }
    }
}

