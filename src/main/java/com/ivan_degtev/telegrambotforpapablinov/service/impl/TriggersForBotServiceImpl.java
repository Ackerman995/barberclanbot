package com.ivan_degtev.telegrambotforpapablinov.service.impl;

import com.ivan_degtev.telegrambotforpapablinov.component.TelegramWebhookConfiguration;
import com.ivan_degtev.telegrambotforpapablinov.dto.TYPE_REQUEST;
import com.ivan_degtev.telegrambotforpapablinov.dto.mapping.WebhookPayloadDto;
import com.ivan_degtev.telegrambotforpapablinov.service.TriggersForBotService;
import com.ivan_degtev.telegrambotforpapablinov.service.ai.ProcessingRegularRequestsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class TriggersForBotServiceImpl implements TriggersForBotService {

    private final TelegramWebhookConfiguration telegramWebhookConfiguration;
    private final RedisServiceImpl redisService;

    private final static String BOT_USERNAME = "papa_blinov_bot";

    private final static String START_MESSAGE = """
            Привет! Это бот компании "Папа Блинов". Наша компания специализируется на изгготовлении и продаже блинов и мы открыты к сотрудничеству.
            Вы можете узнать больше о возможностях бота - нажав на команду информация.
            Не стесняйтесь спрашивать! Хорошего дня!
            """;
    private final static String INFO_MESSAGE = """
            Привет! Это общая информация о работе бота компании "Папа Блинов".  
            Бот создан для ответов на вопросы, связанные с компанией, но также, в случае необходимости может ответить вам на любой другой вопрос, которые позволяют обрабатывать
            LLM-системы, в частности Open Ai.  
            Бот имеет два режима работы  - режим обычных запросом и режим поиска файлов. В режиме обычных запросов - вы задаете вопрос и вам отвечают текстом. 
            В режиме поиска - бот отправляет вам 5 наиболее подходящих под ваше описание файлов из внутренней базы компании "Папа Блинов".   
            Для смены режимов используйте соотвествующие команды бота.   
            Команду достаточно использовать 1 раз и все следующие запросы будут в выбранном режиме, пока вы не смените его, если введёте другую команду.  
            Режим обычных запросов является режимом по умолчанию. 
            При использовании бота в группах, для поиска файлов, вы также можете ввести команду @search в ответ на сообщение бота и он переключится в режим поиска файлов.
            При использовании бота в группах он обрабатывает только сообщения, где указан его никнейм (@papa_blinov_bot) или если вы отвечаете на прошлое сообщение бота.
            Для корректной работы, пожалуйста, задавайте вопросы последовательно, не нужно писать подряд несколько сообщений, это может привести к ошибке. Сформулируйте свой запрос,
            отправьте его и после получения ответа(обычно это не более 30 секунд) - задайте сделающий вопрос. 
            Желаем вам приятного использования!
            """;

    private final static String QUESTION_MESSAGE = """
            Вы переключились в режим обычных вопросов. В этом режиме бот будет отвечать текстом на ваши вопросы, использую внутреннюю документацию компании
            "Папа Блинов". Вы также можете задать любой другой вопрос и бот ответит в рамках возможнолостей LLM-модели Chat-GPT 4. Для переключения в режим поиска файлов
            компании - используйте команду /search
            """;
    private final static String SEARCH_MESSAGE = """
                        Вы переключились в режим выдачи файлов. В этом режиме бот будет только отправлять вам внутренние файлы компании "Папа Блинов". 
                        Бот будет выдывать вам файлы, наиболее близкие по контексту вашего запроса. Пожалуйста, формулируйте ваш запрос более подходяще для поиска. 
                        Чтобы задать любой другой вопрос и получить ответ в текстовом формате, переключитесь  в режим обычных запросов - используйте команду /question
            """;
    private final static String HELP_MESSAGE = """
            Если бот  не работает или ведёт себя как то странно, или если вы просто хотите связаться с отделом разработки - пожалуйста напишите нам в телеграм
            @kirill_zorinov или @ar_terria.
            Желательно сделать скриншоты или записи экрана с фиксацией бага/неправильного поведения и предоставить их.
            Приносим свои извинения за доставленные неудобства, скоро этот баг будет устранен!
            """;

    private final static String CLEAN_YOUR_MEMORY = """
            Память диалога очищена на стороне Open AI. Теперь Искусственный Интеллект не будет помнить о вашем предыдущем разговоре. Можете начать его с начала!
            """;

    private final static String UNKNOWN_MESSAGE = """
            Вы ввели несуществующую команду или слово, содержание в начале знак "/", который телеграм расценивает, как команду. Пожалуйста, введите корректную команду
            или отправьте заново ваше сообщение без символов "/". Спасибо!
            """;
    private final ProcessingRegularRequestsService processingRegularRequestsService;


    @Override
    public boolean searchAndHandleCommand(WebhookPayloadDto webhookPayloadDto) {
        if (webhookPayloadDto.getMessage().getEntities() != null &&
                webhookPayloadDto.getMessage().getEntities().get(0).getType().equals("bot_command")) {
            String textMessage = webhookPayloadDto.getMessage().getText();
            String fromId = String.valueOf(webhookPayloadDto.getMessage().getFrom().getId());
            String chatId = String.valueOf(webhookPayloadDto.getMessage().getChat().getId());
            Long replayMessageId = getIdMessageForReplay(webhookPayloadDto);

            Pattern pattern = Pattern.compile("/\\w+");
            Matcher matcher = pattern.matcher(textMessage);
            while (matcher.find()) {
                String command = matcher.group();
                log.info("Найдена команда: {}", command);

                processCommand(fromId, chatId, replayMessageId, command);
            }
            return true;
        }
        return false;
    }

    /**
     * Метод для обработки команд
     */
    private void processCommand(String fromId, String chatId, Long replayMessageId, String command) {
        switch (command.toLowerCase()) {
            case "/start":
                log.info("Обработка команды /start");
                telegramWebhookConfiguration.sendResponseMessage(chatId, START_MESSAGE, replayMessageId);
                return;
            case "/info":
                log.info("Обработка команды /info");
                telegramWebhookConfiguration.sendResponseMessage(chatId, INFO_MESSAGE, replayMessageId);
                return;
            case "/question":
                log.info("Обработка команды /question");
                redisService.saveUserRequestType(fromId, TYPE_REQUEST.REGULAR);
                telegramWebhookConfiguration.sendResponseMessage(chatId, QUESTION_MESSAGE, replayMessageId);
                return;
            case "/search":
                log.info("Обработка команды /search");
                redisService.saveUserRequestType(fromId, TYPE_REQUEST.SEARCH);
                telegramWebhookConfiguration.sendResponseMessage(chatId, SEARCH_MESSAGE, replayMessageId);
                return;
            case "/clean_your_memory":
                log.info("Обработка команды /clean_your_memory");
                processingRegularRequestsService.deleteOldThread(fromId);
                telegramWebhookConfiguration.sendResponseMessage(chatId, CLEAN_YOUR_MEMORY, replayMessageId);
                return;
            case "/help":
                log.info("Обработка команды /help");
                telegramWebhookConfiguration.sendResponseMessage(chatId, HELP_MESSAGE, replayMessageId);
                return;
            default:
                log.warn("Неизвестная команда: {}", command);
                telegramWebhookConfiguration.sendResponseMessage(chatId, UNKNOWN_MESSAGE, replayMessageId);
        }
    }


    @Override
    public boolean handleMessage(WebhookPayloadDto payload) {
        WebhookPayloadDto.MessageDto message = payload.getMessage();
        String chatType = payload.getMessage().getChat().getType();

        /*
        Ранняя првоерка, если чат приватный - отвечать на все сообщения
         */
        if (chatType.equals("private")) {
            return true;
        } else if (chatType.equals("supergroup")) {
            var replyToMessage = payload.getMessage().getReplyToMessage();

            Long botId = null;


            if (replyToMessage != null) {
                botId = replyToMessage.getFrom() != null ? replyToMessage.getFrom().getId() : null;
            }

            return isBotMentioned(message, BOT_USERNAME) || (botId != null && isReplyToBot(message, botId));
        }
        return false;
    }

    @Override
    public void isSearchRequest(String fromId, String textMessage) {
        if(textMessage != null && textMessage.toLowerCase().contains("@search")) {
            redisService.saveUserRequestType(fromId, TYPE_REQUEST.SEARCH);
        }
    }

    @Override
    public Long getIdMessageForReplay(WebhookPayloadDto payload) {
        if (payload.getMessage().getChat().getType().equals("private")) {
            return null;
        } else if (payload.getMessage().getChat().getType().equals("supergroup")) {
            return payload.getMessage().getMessageId();
        } else {
            return null;
        }
    }

    @Override
    public boolean isBotMentioned(WebhookPayloadDto.MessageDto message, String botUsername) {
        if (message.getText() != null && message.getEntities() != null) {
            for (WebhookPayloadDto.EntityDto entity : message.getEntities()) {
                if ("bot_command".equals(entity.getType()) || "mention".equals(entity.getType())) {
                    String entityText = message.getText().substring(entity.getOffset(), entity.getOffset() + entity.getLength());

                    if (entityText.contains("@" + botUsername)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isReplyToBot(WebhookPayloadDto.MessageDto message, Long botId) {
        WebhookPayloadDto.MessageDto replyToMessage = message.getReplyToMessage();
        if (replyToMessage != null) {
            return replyToMessage.getFrom() != null && replyToMessage.getFrom().getUsername().equals(BOT_USERNAME);
        }
        return false;
    }
}
