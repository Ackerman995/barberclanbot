package com.ivan_degtev.telegrambotforpapablinov.service;

import org.springframework.stereotype.Service;

@Service
public interface TelegramService {

    /**
     * Начальное получение всех данных из телеграмма по вебхуку
     * @param payload
     */
    void getNewMessagesFromWebhook(String payload);
}
