package com.ivan_degtev.telegrambotforpapablinov.controller;

import com.ivan_degtev.telegrambotforpapablinov.service.TelegramService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@AllArgsConstructor
public class WebhookController {

    private final TelegramService telegramService;
    /**
     * На эту ручку будут приходить все входщие и исходящие сообщения с вотсапа, переброшенные сюда через чатпуш -> ngrok
     * Нужно в дальнейшей логике корректно фильтровать и не обрабатывать через LLM исходящие сообшения
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) {
        telegramService.getNewMessagesFromWebhook(payload);
        return ResponseEntity.ok("Сообщение через веб-хук успешно получено!");
    }
}
