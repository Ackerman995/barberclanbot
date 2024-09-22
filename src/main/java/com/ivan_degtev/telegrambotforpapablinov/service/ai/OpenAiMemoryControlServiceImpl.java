package com.ivan_degtev.telegrambotforpapablinov.service.ai;

import com.ivan_degtev.telegrambotforpapablinov.mapper.OpenAiMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiMemoryControlServiceImpl {

    @Value("${openai.token}")
    private String openAiToken;

    private final WebClient webClient;
    private final ProcessingRegularRequestsService processingRegularRequestsService;
    private final OpenAiMapper openAiMapper;

    private final static String assistantId = "asst_TMo9HU85ItAzi87f2fMeSheQ";

    public OpenAiMemoryControlServiceImpl(
            @Value("${openai.token}") String openAiToken,
            OpenAiMapper openAiMapper,
            WebClient.Builder webClient,
            ProcessingRegularRequestsService processingRegularRequestsService
    ) {
        this.openAiToken = openAiToken;
        this.openAiMapper = openAiMapper;
        this.webClient = webClient
                .baseUrl("https://api.openai.com")
                .build();
        this.processingRegularRequestsService = processingRegularRequestsService;
    }


    /**
     * Метод с запросом на апи для создания краткого резюме прошлого диалога для последующего внедрения этого в новый тред
     * @param threadId
     * @return
     */
    public String generateManualSummary(String threadId) {
        StringBuilder threadMessages = getThreadMessages(threadId);

        String summaryResponse = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + openAiToken)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "model", "gpt-4",
                        "messages", List.of(
                                Map.of("role", "user", "content", "Сделай резюме по контексту предыдущего общения с клиентом, " +
                                        "это резюме нужно для AI для нового диалога, но с лучшим пониманием контекста. Прошлый диалог(начиная с последних сообщений): "
                                        + threadMessages.toString())
                        ),
                        "max_tokens", 300
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return summaryResponse;
    }

    /**
     * Утилитный метод для резюмирования истории  - получает все сообщения по апи и отдаёт их в маппер для маппинга в строку с ролями
     * @param threadId
     * @return
     */
    private StringBuilder getThreadMessages(String threadId) {
        String allMessages = processingRegularRequestsService.getMessages(threadId);
        StringBuilder mapWithMessages = openAiMapper.extractRoleAndContentFromMemory(allMessages);
        return mapWithMessages;
    }

}
