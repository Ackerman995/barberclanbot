package com.ivan_degtev.telegrambotforpapablinov.service;

import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import org.springframework.stereotype.Service;

@Service
public interface TriggersForBotService {


    /**
     * Общий метод для подготовки нужных данных и отпраувки в нужные триггеры для првоерки
     */
    boolean handleMessage(WebhookPayloadDto payload);

    /**
     * Метод для поиска и обработки команд, если они есть
     */
    boolean searchAndHandleCommand(WebhookPayloadDto payload);

    /**
     * Метод для проверки есть ли в тексте комадна для поиска @search
     */
    void isSearchRequest(String fromId, String textMessage);
    /**
     * Утилитный метод нужен для определения типа откуда сообщения для последующей разной обработки(на сообщения из групп бот отвечает, из приватных чатов
     * - просто пишет в чат, без реплаев)
     */
    Long getIdMessageForReplay(WebhookPayloadDto payload);

    /**
    Метод для проверки упоминается ли бот по нику
     */
    boolean isBotMentioned(WebhookPayloadDto.MessageDto message, String botUsername);

    /**
     * Метод для првоерки каждого сообщения в чате - ответ ли это на другие сообщения бота
     */
    boolean isReplyToBot(WebhookPayloadDto.MessageDto message, Long botId);
}
