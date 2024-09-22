package com.ivan_degtev.telegrambotforpapablinov.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.Webhook;

@Service
public interface WebhookService {

    String createWebhook();
    String getAllWebhooks();
    String deleteWebhook();
}
