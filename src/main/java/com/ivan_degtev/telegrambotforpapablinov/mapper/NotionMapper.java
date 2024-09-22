package com.ivan_degtev.telegrambotforpapablinov.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionMapper {

    private final ObjectMapper objectMapper;



    /**
     * Метод для парсинга данных со страницы в ноушен только имена и урл файлов
     * @param jsonString
     * @return
     */
    public Map<String, String> extractDataFromFiles(String jsonString) {
        try {
            Map<String, String> fileLinks = new HashMap<>();
            JsonNode rootNode = objectMapper.readTree(jsonString);

            JsonNode resultsNode = rootNode.path("results");
            if (resultsNode.isArray()) {
                for (JsonNode block : resultsNode) {
                    if (block.path("type").asText().equals("file")) {
                        String fileName = block.path("file").path("name").asText();
                        String fileUrl = block.path("file").path("file").path("url").asText();
                        fileLinks.put(fileName, fileUrl);
                    }
                }
            }
            return fileLinks;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public String extractNextCursor(String response) {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonNode.path("next_cursor").asText(null);
    }

    /**
     * легаси
     * Метод для парсинга данных с notion, получаем только id конкретной старницы
     * @param jsonString
     * @return
     */
    public List<String> extractIdFromPages(String jsonString) {
        try {
            JsonNode rootNode = new ObjectMapper().readTree(jsonString);
            List<String> pageIds = new ArrayList<>();
            rootNode.path("results").forEach(page -> pageIds.add(page.path("id").asText()));

            return pageIds;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
