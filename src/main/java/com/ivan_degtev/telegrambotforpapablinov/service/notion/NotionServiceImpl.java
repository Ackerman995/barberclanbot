package com.ivan_degtev.telegrambotforpapablinov.service.notion;

import com.ivan_degtev.telegrambotforpapablinov.mapper.NotionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotionServiceImpl {

    private final WebClient notionWebClient;
    private final NotionMapper notionMapper;

    private final static String DATA_BASE_ID = "d88493ddf2d149329babf255d790e578";
    //    private final static String PATH_FOR_SAVE_FILES = "src/main/resources/files";
//    private final static String PATH_FILE_WITH_IDS_WORK_PAGES = "src/main/resources/page_ids.txt";

    private final static String PATH_FOR_SAVE_FILES = "app/files";
    private final static String PATH_FILE_WITH_IDS_WORK_PAGES = "page_ids.txt";


    /**
     * Метод для загрузки всех файлов по имеющейся мапе с их урл локлаьно в программу из notion
     *
     * @return
     */
    public List<String> downloadAllFiles() {
        Map<String, String> fileMap = getFilesFromAllPages();

        Path targetDirectory = Paths.get(PATH_FOR_SAVE_FILES);
        List<String> results = new ArrayList<>();

        try {
            Files.createDirectories(targetDirectory);
        } catch (IOException e) {
            log.error("Ошибка создания директории: {}", e.getMessage());
        }

        fileMap.forEach((fileName, fileUrl) -> {
            Path filePath = targetDirectory.resolve(fileName);

            if (!isFileExists(filePath)) {
                try {
                    downloadFile(fileUrl, filePath);
                    String successMsg = "Файл " + fileName + " успешно загружен.";
                    log.info(successMsg);
                    results.add(successMsg);
                } catch (IOException e) {
                    String errorMsg = "Ошибка загрузки файла " + fileName + ": " + e.getMessage();
                    log.error(errorMsg);
                    results.add(errorMsg);
                }
            } else {
                String infoMsg = "Файл " + fileName + " уже существует, загрузка пропущена.";
                log.info(infoMsg);
                results.add(infoMsg);
            }
        });

        return results;
    }

    /**
     * Меьтод для получения всех данных об объектах на определённой страницы в notion(айди страницы нужно указать в коде или в файле - см константу)
     * Получаем данные - парсим только названия и url файлов
     *
     * @return Map с имя и url всех файлов на конкретной страние notion
     */
    public Map<String, String> getFilesFromAllPages() {
        List<String> pageIds = readPageIdsFromFile(PATH_FILE_WITH_IDS_WORK_PAGES);
        Map<String, String> allFiles = new HashMap<>();

        for (String pageId : pageIds) {
            String nextCursor = null;

            do {
                String uri = "/blocks/{pageId}/children?page_size=100";
                if (nextCursor != null) {
                    uri += "&start_cursor=" + nextCursor;
                }

                String response = notionWebClient.get()
                        .uri(uri, pageId)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                Map<String, String> parsedFiles = notionMapper.extractDataFromFiles(response);

                parsedFiles.forEach((fileName, fileUrl) -> {
                    if (!allFiles.containsKey(fileName)) {
                        allFiles.put(fileName, fileUrl);
                    }
                });

                nextCursor = notionMapper.extractNextCursor(response);

            } while (nextCursor != null);
        }
        return allFiles;
    }

    /**
     * Проверяет, существует ли файл по указанному пути.
     *
     * @param filePath путь к файлу
     * @return true, если файл существует; false, если нет
     */
    private boolean isFileExists(Path filePath) {
        return Files.exists(filePath);
    }

    /**
     * Утилитный метод для загрузки одного файла по URL и сохранения на диск
     */
    private void downloadFile(String fileUrl, Path targetPath) throws IOException {
        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл успешно загружен и сохранен: {}", targetPath);
        } catch (IOException e) {
            log.error("Ошибка загрузки файла {}: {}", fileUrl, e.getMessage());
            throw e;
        }
    }

    /**
     * Утилитный метод по чтению строк файла и маппинга их в лист
     */
    private List<String> readPageIdsFromFile(String fileName) {
        List<String> pageIds = new ArrayList<>();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Файл не найден: " + fileName);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    pageIds.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pageIds;
    }

    /**
     * Легаси
     */
    public List<String> getListWithPageId() {
        String response = notionWebClient.post()
                .uri("/databases/{databaseId}/query", DATA_BASE_ID)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        List<String> idPages = notionMapper.extractIdFromPages(response);
        return idPages;
    }

    /**
     * Легаси, просто получить данные по запросу, для теста
     */
    public Map<String, String> getFilesFromPage(String pageId) {
        String response = notionWebClient.get()
                .uri("/blocks/{pageId}/children", pageId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        Map<String, String> mapWithData = notionMapper.extractDataFromFiles(response);
        log.info("Получил замапленные данные всех файлов имя и ссылку {}", mapWithData.toString());
        return mapWithData;
    }
}
