package com.ivan_degtev.telegrambotforpapablinov.service.ai;

import com.ivan_degtev.telegrambotforpapablinov.component.TelegramWebhookConfiguration;
import com.ivan_degtev.telegrambotforpapablinov.dto.TYPE_REQUEST;
import com.ivan_degtev.telegrambotforpapablinov.exception.LlmQuerySyntaxException;
import com.ivan_degtev.telegrambotforpapablinov.mapper.OpenAiMapper;
import com.ivan_degtev.telegrambotforpapablinov.service.impl.RedisServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProcessingRegularRequestsService {

    private final TelegramWebhookConfiguration telegramWebhookConfiguration;
    @Value("${openai.token}")
    private String openAiToken;
    private final WebClient webClient;
    private final OpenAiMapper openAiMapper;

    @Lazy private final OpenAiMemoryControlServiceImpl openAiMemoryControlService;
    private final ProcessingSearchRequestsService processingSearchRequestsService;
    private final RedisServiceImpl redisService;

    private final static String ASSISTANT_ID = "asst_nPqCeUlu5QPRx9OhhrRKmSLB";
    private final static String SYSTEM_MESSAGE_FOR_SEARCH_ID_FILES = """
            Пользователь ищет актуальные файлы из векторного хранилища по своему запросу. Тебе нужно проанализировать его запрос, найти 5 самых подходящих файла 
            и выдать только их внутренние названия и id без изменений! Выдать нужно в формате json, но без изменения названий файлов! Нужно записать в json название
            файла точь-в-точь, как оно называется в твоем внутреннем хранилище! 
            Пример: \"Имя файла во внутреннем хранилище.docx\":\"id_файла_в_хранилище\".
            """;

    private final static Map<Long, String> userThreads = new HashMap<>();
    private final static Map<Long, Integer> summaryMessagesForUser = new HashMap<>();

    public ProcessingRegularRequestsService(
            @Value("${openai.token}") String openAiToken,
            OpenAiMapper openAiMapper,
            WebClient.Builder webClient,
            @Lazy OpenAiMemoryControlServiceImpl openAiMemoryControlService,
            ProcessingSearchRequestsService processingSearchRequestsService,
            TelegramWebhookConfiguration telegramWebhookConfiguration,
            RedisServiceImpl redisService
    ) {
        this.openAiToken = openAiToken;
        this.openAiMapper = openAiMapper;
        this.webClient = webClient
                .baseUrl("https://api.openai.com")
                .build();
        this.openAiMemoryControlService = openAiMemoryControlService;
        this.processingSearchRequestsService = processingSearchRequestsService;
        this.telegramWebhookConfiguration = telegramWebhookConfiguration;
        this.redisService = redisService;
    }

    public void createRequestGetResponse(String chatId, Long fromId, String question, Long replayMessageId) {
        try {
//            String threadId = userThreads.get(fromId);
//            TYPE_REQUEST currentTypeRequest = redisService.getUserRequestType(String.valueOf(fromId));
//
//            if (threadId == null) {
//                String jsonResponseCreateThread = createThread(fromId);
//                threadId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateThread);
//
//                userThreads.put(fromId, threadId);
//                addAndCleanHistoryMessage(fromId);
//            } else {
//                addAndCleanHistoryMessage(fromId);
//                threadId = userThreads.get(fromId);
//            }
            String threadId = redisService.getUserThread(String.valueOf(fromId));
            System.out.println("Retrieved Thread ID from Redis: " + threadId);
            TYPE_REQUEST currentTypeRequest = redisService.getUserRequestType(String.valueOf(fromId));

            if (threadId == null) {
                String jsonResponseCreateThread = createThread(fromId);
                System.out.println("Create Thread Response: " + jsonResponseCreateThread);

                threadId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateThread);
                System.out.println("Extracted Thread ID: " + threadId);

                redisService.setUserThread(String.valueOf(fromId), threadId);
                addAndCleanHistoryMessage(String.valueOf(fromId));
            } else {
                addAndCleanHistoryMessage(String.valueOf(fromId));
            }

            if (threadId == null || threadId.isEmpty()) {
                throw new IllegalArgumentException("Thread ID is null or empty");
            }

            //here we are creating LLM instructions to retrieve only the referenced files names without distorting them aanyhow
            String jsonResponseCreateMessage = createResponseMessage(currentTypeRequest, threadId, question);
            String responseMessageId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateMessage);

            String jsonResponseCreateRun = runThread(threadId);
            String responseRunId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateRun);

            if (checkStatusReceivingResponse(threadId, responseRunId)) {
                String jsonResponseGetMessages = getMessages(threadId);
                String responseIdAnswer = openAiMapper.extractLatestMessageId(jsonResponseGetMessages);

                String jsonResponseGetMessage = getMessage(threadId, responseIdAnswer);
                String responseLlm = openAiMapper.extractDataFromLlmAnswer(jsonResponseGetMessage);

                if (currentTypeRequest.equals(TYPE_REQUEST.SEARCH)) {
                    Map<String, String> filesData = openAiMapper.extractFileIds(responseLlm);
                    processingSearchRequestsService.preparingDataForDownloadingFiles(filesData, chatId, replayMessageId);
                    return;
                }
                log.info("Получил ответ от ллм по сути вопроса замапленный: {}", responseLlm);

                telegramWebhookConfiguration.sendResponseMessage(chatId, responseLlm, replayMessageId);
            }
        } catch (LlmQuerySyntaxException ex) {
            throw new LlmQuerySyntaxException("Ошибка в последовательности запросов к Open AI для работы с ассистентом");
        }
    }

    /**
     * метод проверяет кол-во записей в мапе по id чата, когда оно равно 10  - создаётся новый тред и отсчет начинается заново(для экономии токенов)
     */
    public void addAndCleanHistoryMessage(String fromId) {
        String countStr = redisService.getUserMessageCount(fromId);
        int currentMessageCount = (countStr != null && !countStr.isEmpty()) ? Integer.parseInt(countStr) : 0;
        log.info("Получил в сервис данные о кол-ве сообщений в треде и замапил в инт {}", currentMessageCount);

        if (currentMessageCount == 0) {
//            redisService.incrementUserMessageCount(fromId);
            redisService.incrementUserMessageCount(fromId, String.valueOf(currentMessageCount + 1));
        } else if (currentMessageCount < 10) {
//            redisService.incrementUserMessageCount(fromId);
            redisService.incrementUserMessageCount(fromId, String.valueOf(currentMessageCount + 1));
        } else if (currentMessageCount == 10) {
            String summaryMessages = openAiMemoryControlService.generateManualSummary(redisService.getUserThread(fromId), fromId);

            deleteOldThread(fromId);

            String jsonResponseCreateThread = createNewThreadWithSummary(summaryMessages);
            String threadId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateThread);

            redisService.setUserThread(fromId, threadId);
            redisService.resetUserMessageCount(fromId);
            log.info("Записан новый id треда после переполнения прошлого {}", redisService.getUserThread(fromId));
        }
    }
//    public void addAndCleanHistoryMessage(Long chatId) {
//        if (summaryMessagesForUser.get(chatId) == null) {
//            summaryMessagesForUser.put(chatId, 1);
//        } else if (summaryMessagesForUser.get(chatId) != null && summaryMessagesForUser.get(chatId) < 10) {
//            summaryMessagesForUser.put(chatId, summaryMessagesForUser.get(chatId) + 1);
//        } else if (summaryMessagesForUser.get(chatId) != null && summaryMessagesForUser.get(chatId) == 10) {
//            String summaryMessages = openAiMemoryControlService.generateManualSummary(userThreads.get(chatId));
//
//            deleteOldThread(userThreads.get(chatId));
//
//            String jsonResponseCreateThread = createNewThreadWithSummary(summaryMessages);
//            String threadId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateThread);
//
//            userThreads.put(chatId, threadId);
//            summaryMessagesForUser.put(chatId, 1);
//            log.info("Записан новый id треда после переполнения прошлого {}", userThreads.get(chatId));
//        }
//    }
    /**
     * Метод проверяет внесен ли в общий тред ответ от ллм, когда это происходит - он отдаёт айди этого сообщения
     * @return
     */
    public boolean checkStatusReceivingResponse(String threadId, String responseRunId) {
        Instant startTime = Instant.now();
        String responseRunStatus;

        do {
            String jsonResponseGetRun = getRun(threadId, responseRunId);
            responseRunStatus = openAiMapper.extractRunStatus(jsonResponseGetRun);

            if (responseRunStatus.equals("completed")) {
                return true;
            }

            if (Duration.between(startTime, Instant.now()).getSeconds() > 120) {
                log.error("Timeout reached while waiting for completion status.");
                throw new LlmQuerySyntaxException("Время ожидания превышено, статус не стал 'completed'.");
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for completion status", e);
                return false;
            }
        } while (!responseRunStatus.equals("completed"));

        return false;
    }

    /**
     * Утилитный метод для создания нового треда для экономии токенов и передачи в него резюме прошлого контекста общения, срабатывает после 10 вопросов в текущем треде
     * @param summary
     * @return
     */
    public String createNewThreadWithSummary(String summary) {
        String response = webClient.post()
                .uri("/v1/threads")
                .header("Authorization", "Bearer " + openAiToken)
                .header("Content-Type", "application/json")
                .header("OpenAI-Beta", "assistants=v2")
                .bodyValue(Map.of(
                        "assistant_id", ASSISTANT_ID,
                        "messages", List.of(
                                Map.of("role", "assistant", "content", "Вот что мы обсудили на данный момент: " + summary)
                        )
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return response;
    }

    public String deleteOldThread(String fromId) {
        String threadId = redisService.getUserThread(fromId);

        String response = webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/threads/{threadId}")
                        .build(threadId))
                .header("Authorization", "Bearer " + openAiToken)
                .header("Content-Type", "application/json")
                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        redisService.deleteOldThread(fromId);
        return response;
    }

    /**
     * Утилитный метод для получения всех ассистентов по айди данной компании в open ai
     */
    public String getCompanyAssistants() {
        return webClient.get()
                .uri("/v1/assistants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    /**
     * Утилитный метод для получения всех ассистентов по айди данной компании в open ai
     */
    public String getCompanyFiles() {
        return webClient.get()
                .uri("/v1/files")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
//                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     *     Метод для создания треда для конкретного пользователя
     */
    public String createThread(Long chatId) {
        System.out.println("creating thread from chatid: " + chatId);
        return webClient.post()
                .uri("/v1/threads")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

    }

    /**
     * Метод по созданию сообщения-вопрос к ллм, для помещения его после в тред
     */
    public String createResponseMessage(TYPE_REQUEST currentTypeId, String threadId, String userMessage) {
        Map<String, String> messages = new HashMap<>();

        if (currentTypeId.equals(TYPE_REQUEST.SEARCH)) {
            messages = Map.of("role", "user", "content", SYSTEM_MESSAGE_FOR_SEARCH_ID_FILES + "Запрос пользователя: " + userMessage);
        } else {
            messages = Map.of("role", "user", "content", userMessage);
        }
        System.out.println("Fetching messages for Thread ID: " + threadId);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/threads/{threadId}/messages")
                        .build(threadId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(messages)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Метод для запуска треда, в который добавлено текущее сообщение. Важно. Получить айди этого запуска для дальнейшей работы
     * @param threadId
     * @return
     */
    public String runThread(String threadId) {
        log.info("Running thread with ID: {}", threadId);
        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/threads/{threadId}/runs")
                            .build(threadId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                    .header("OpenAI-Beta", "assistants=v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("assistant_id", ASSISTANT_ID))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error response from server: {}", e.getResponseBodyAsString());
            log.error("Status code: {}, message: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            return null;
        }
    }


    /**
     * Метод для просмотра инфы о запуске. Использвоать только статус - при завершённом - получить далее изменения в треде - то есть ответ
     * @param threadId
     * @param runId - получить после запуска
     * @return
     */
    public String getRun(String threadId, String runId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/threads/{threadId}/runs/{runId}")
                        .build(threadId, runId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Получить все сообщения из треда. Использвоать, когда нужно получить айди последнего сообщения - ответа от ллм
     * @param threadId
     * @return
     */
    public String getMessages(String threadId) {
        System.out.println("Fetching messages for Thread ID: " + threadId);
        try {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/threads/{threadId}/messages")
                        .build(threadId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();}
            catch (WebClientResponseException e) {
            System.err.println("Error fetching messages for Thread ID: " + threadId);
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    public String getMessage(String threadId, String messageId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/threads/{threadId}/messages/{messageId}")
                        .build(threadId, messageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                .header("OpenAI-Beta", "assistants=v2")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String ensureValidThread(String userId, String threadId) {
        try {
            // Проверяем доступность треда
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/threads/{threadId}/messages")
                            .build(threadId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiToken)
                    .header("OpenAI-Beta", "assistants=v2")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return threadId;
        } catch (WebClientResponseException.NotFound e) {
            // Тред недоступен, создаем новый
            System.out.println("Thread not found, creating a new one...");

            String jsonResponseCreateThread = createThread(Long.parseLong(userId));
            System.out.println("Create Thread Response: " + jsonResponseCreateThread);

            String newThreadId = openAiMapper.extractIdAfterCreateResponseMessage(jsonResponseCreateThread);
            System.out.println("Extracted Thread ID: " + newThreadId);
            redisService.setUserThread(userId, newThreadId);
            return newThreadId;
        }
    }

}