package com.ivan_degtev.telegrambotforpapablinov.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class BotConfig {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    @Value("${ngrok.url}")
    private String ngrokBaseUrl;

    public String getFullWebhookUrl() {
        return ngrokBaseUrl + webhookPath;
    }
}
