package com.ivan_degtev.telegrambotforpapablinov.service;

import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import org.springframework.stereotype.Service;

@Service
public interface OpenAiService {

     /**
      * Обычный запрос к ллм текстом , запоминается айди юзера для истории и айди чата для верной отправки в чат(если он есть)
      */
     void getAnswerFromLlm(String chatId, String fromId, String question, Long replayMessageId);

//     /**
//      *
//      * Метод для поиска в ембендинговой БД и выдачи валдиных файлов по запросу юзера
//      */
//     void searchFilesForRequest(String query);
//
//     /**
//      * Метод для отправки обычных сообщений от LLM в чаты или группы
//      */
//     void sendMessage(String chatId, String answer, Long replayMessageId);
}
