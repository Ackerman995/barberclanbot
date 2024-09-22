package com.ivan_degtev.telegrambotforpapablinov.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class WebhookMapper {

    private final ObjectMapper objectMapper;

    public WebhookPayloadDto convertStringToWebhookPayload(String stringPayload) {
        try {
            WebhookPayloadDto request = objectMapper.readValue(stringPayload, WebhookPayloadDto.class);
            return request;
        } catch (Exception e) {
            new RuntimeException("Failed to deserialize incoming request", e);
        }
        return null;
    }
}
