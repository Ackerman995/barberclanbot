package com.ivan_degtev.telegrambotforpapablinov.service.ai;

import com.ivan_degtev.telegrambotforpapablinov.component.TelegramWebhookConfiguration;
import com.ivan_degtev.telegrambotforpapablinov.mapper.OpenAiMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProcessingSearchRequestsService {

    @Value("${openai.token}")
    private String openAiToken;
    private final WebClient webClient;
    private final OpenAiMapper openAiMapper;
    private final TelegramWebhookConfiguration telegramWebhookConfiguration;

    //    private final static String PATH_FOR_SAVE_FILES = "src/main/resources/files";
    private final static String PATH_FOR_SAVE_FILES = "app/files";

    public ProcessingSearchRequestsService(
            @Value("${openai.token}") String openAiToken,
            OpenAiMapper openAiMapper,
            WebClient.Builder webClient,
            TelegramWebhookConfiguration telegramWebhookConfiguration
    ) {
        this.openAiToken = openAiToken;
        this.openAiMapper = openAiMapper;
        this.webClient = webClient
                .baseUrl("https://api.openai.com")
                .build();
        this.telegramWebhookConfiguration = telegramWebhookConfiguration;
    }

    /**
     * Основной метод для подготовки и поиска файлов
     */
    public void preparingDataForDownloadingFiles(Map<String, String> filesData, String chatId, Long replyToMessageId) {
        for (Map.Entry<String, String> file : filesData.entrySet()) {
            searchFiles(file.getKey(), chatId, replyToMessageId);
        }
    }

    /**
     * Метод поиска файлов по имени
     */
    private void searchFiles(String fileName, String chatId, Long replyToMessageId) {
        Path directoryPath = Paths.get(PATH_FOR_SAVE_FILES);
        List<String> possibleFileNames = prepareNamesWithAllExtensions(fileName);

        try (Stream<Path> filesStream = Files.list(directoryPath)) {
            List<File> filesList = filesStream
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .collect(Collectors.toList());

// Логируем все файлы в директории
// filesList.forEach(file -> log.info("Файл в директории: {}", file.getName()));

// Поиск файлов
            for (String possibleName : possibleFileNames) {
                boolean fileFound = filesList.stream()
                        .anyMatch(file -> file.getName().equalsIgnoreCase(possibleName.trim())); // trim убирает пробелы

                if (fileFound) {
                    log.info("Файл найден: {}", possibleName);
// Отправка файла в Telegram
                    telegramWebhookConfiguration.sendDocument(chatId, new File(directoryPath + File.separator + possibleName), replyToMessageId);
                } else {
                    log.warn("Файл не найден: {}", possibleName);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при поиске файлов: {}", e.getMessage());
        }
    }


    private List<String> prepareNamesWithAllExtensions(String fileName) {
        String baseFileName = fileName.substring(0, fileName.lastIndexOf('.'));
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);

        log.info("Зашёл в метод двойного названию, имя файла: и расширение: {}, {}", fileName, fileExtension);

        List<String> possibleFileNames = new ArrayList<>();
        possibleFileNames.add(fileName);

        if (fileExtension.equalsIgnoreCase("doc")) {
            possibleFileNames.add(baseFileName + ".docx");
        } else if (fileExtension.equalsIgnoreCase("docx")) {
            possibleFileNames.add(baseFileName + ".doc");
        } else if (fileExtension.equalsIgnoreCase("xls")) {
            possibleFileNames.add(baseFileName + ".xlsx");
        } else if (fileExtension.equalsIgnoreCase("xlsx")) {
            possibleFileNames.add(baseFileName + ".xls");
        } else if (fileExtension.equalsIgnoreCase("txt")) {
            possibleFileNames.add(baseFileName + ".txt");
        } else if (fileExtension.equalsIgnoreCase("ppt")) {
            possibleFileNames.add(baseFileName + ".pptx");
        } else if (fileExtension.equalsIgnoreCase("pptx")) {
            possibleFileNames.add(baseFileName + ".ppt");
        } else if (fileExtension.equalsIgnoreCase("pdf")) {
            possibleFileNames.add(baseFileName + ".xls");
        }
            log.info("Итоговая мапа с разными вариантами названий с расширениями: {}",
                    possibleFileNames.stream().collect(Collectors.joining(", ")));
            return possibleFileNames;
        }
    }


