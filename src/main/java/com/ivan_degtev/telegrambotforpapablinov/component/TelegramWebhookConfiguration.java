package com.ivan_degtev.telegrambotforpapablinov.component;

import com.ivan_degtev.telegrambotforpapablinov.config.BotConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TelegramWebhookConfiguration extends TelegramWebhookBot {

    private final BotConfig botConfig;

    public TelegramWebhookConfiguration(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public String getBotPath() {
        return botConfig.getWebhookPath();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            System.out.println("Получено сообщение: " + update.getMessage().getText());
        }
        return null;
    }

    @PostConstruct
    public void registerBotCommands() {
        List<BotCommand> commands = Arrays.asList(
                new BotCommand("/start", "Запуск бота"),
                new BotCommand("/info", "Общая информация о боте и компании"),
                new BotCommand("/question", "Задать обычный вопрос"),
                new BotCommand("/search", "Получить файлы по запросу"),
                new BotCommand("/clean_your_memory", "Очистить память именно своей переписки с Chat GPT"),
        new BotCommand("/help", "Если бот заболел")
        );

        try {
            SetMyCommands setMyCommands = new SetMyCommands();
            setMyCommands.setCommands(commands);

            execute(setMyCommands);
            log.info("Команды успешно зарегистрированы: {}", commands);
        } catch (TelegramApiException e) {
            log.error("Ошибка при регистрации команд: {}", e.getMessage());
        }
    }

    public void sendResponseMessage(String chatId, String text, Long replyToMessageId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        if (replyToMessageId != null) {
            message.setReplyToMessageId(replyToMessageId.intValue());
        }

        try {
            execute(message);
            log.info("Сообщение отправлено: " + text);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    public void sendDocument(String chatId, File file, Long replyToMessageId) {
        SendDocument document = new SendDocument();
        document.setChatId(chatId);
        document.setDocument(new InputFile(file));

        if (replyToMessageId != null) {
            document.setReplyToMessageId(replyToMessageId.intValue());
        }

        try {
            execute(document);
            log.info("Файл отправлен: " + file.getName());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке файла: " + e.getMessage());
        }
    }


    public void sendReplyResponseMessage(String chatId, String text) {
        sendResponseMessage(chatId, text, null);
    }
}
