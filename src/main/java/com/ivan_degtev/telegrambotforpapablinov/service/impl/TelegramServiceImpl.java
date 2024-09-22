package com.ivan_degtev.telegrambotforpapablinov.service.impl;

import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import com.ivan_degtev.telegrambotforpapablinov.mapper.WebhookMapper;
import com.ivan_degtev.telegrambotforpapablinov.service.TelegramService;
import com.ivan_degtev.telegrambotforpapablinov.service.TriggersForBotService;
import com.ivan_degtev.telegrambotforpapablinov.service.UpdateIdService;
import com.ivan_degtev.telegrambotforpapablinov.service.ai.OpenAiServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class TelegramServiceImpl implements TelegramService {

    private final WebhookMapper webhookMapper;
    private final OpenAiServiceImpl openAiServiceImpl;
    private final TriggersForBotService triggersForBotService;
    private final UpdateIdService updateIdService;

    @Override
    public void getNewMessagesFromWebhook(String payload) {
        WebhookPayloadDto webhookPayloadDto = webhookMapper.convertStringToWebhookPayload(payload);

        if (webhookPayloadDto == null || webhookPayloadDto.getMessage() == null) {
            log.warn("Не удалось обработать сообщение для следующей отправке в LLM: отсутствует поле message {}", webhookPayloadDto.toString());
            return;
        }

        // Проверка уникальности update_id, чтоб избежать дублирующих данных с веб-хука
        Long updateId = webhookPayloadDto.getUpdateId();
        if (!updateIdService.isUniqueUpdateId(updateId)) {
            log.warn("Дубликат update_id: {}. Сообщение будет проигнорировано.", updateId);
            return;
        }

        String textMessage = webhookPayloadDto.getMessage().getText();
        String chatId = String.valueOf(webhookPayloadDto.getMessage().getChat().getId());
        String fromId = String.valueOf(webhookPayloadDto.getMessage().getFrom().getId());

        if (textMessage != null && !textMessage.isEmpty()) {
            if (triggersForBotService.handleMessage(webhookPayloadDto)) {
                Long replayMessageId = triggersForBotService.getIdMessageForReplay(webhookPayloadDto);

                triggersForBotService.isSearchRequest(fromId, textMessage);

                boolean commandExist = triggersForBotService.searchAndHandleCommand(webhookPayloadDto);

                if (!commandExist) {
                    openAiServiceImpl.getAnswerFromLlm(chatId, fromId, textMessage, replayMessageId);
                    log.info("Команд не было в сообщение - оно ушло в обработку LLM {}", textMessage);
                }
                log.info("Были оправлены сообщения команд в чат");
            }
        } else {
            log.info("Получено сообщение без текста или это системное сообщение. {}", webhookPayloadDto.toString());
        }
    }

}
