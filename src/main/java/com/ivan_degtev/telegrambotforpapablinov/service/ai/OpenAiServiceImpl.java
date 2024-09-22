package com.ivan_degtev.telegrambotforpapablinov.service.ai;

import com.ivan_degtev.telegrambotforpapablinov.component.TelegramWebhookConfiguration;
import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import com.ivan_degtev.telegrambotforpapablinov.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiServiceImpl implements OpenAiService {

    private final Assistant assistant;
    private final TelegramWebhookConfiguration telegramWebhookConfiguration;
    private final ProcessingRegularRequestsService processingRegularRequestsService;
    private final ProcessingSearchRequestsService processingSearchRequestsService;

    @Override
    public void getAnswerFromLlm(String chatId, String fromId, String question, Long replayMessageId) {
        processingRegularRequestsService.createRequestGetResponse(chatId, Long.valueOf(fromId), question, replayMessageId);
//        log.info("Ответ от ллм {}", llmAnswer);

//        sendMessage(chatId, llmAnswer, replayMessageId);
    }

//    @Override
//    public void searchFilesForRequest(String query) {
////        String llmAnswer = processingRegularRequestsService.searchFileIds(query);
////        log.info("Ответ от ллм по поиску файлов {}", llmAnswer);
//    }

//    @Override
//    public void sendMessage(String chatId, String llmAnswer, Long replayMessageId) {
//
//        telegramWebhookConfiguration.sendResponseMessage(chatId, llmAnswer, replayMessageId);
//
//    }
}
