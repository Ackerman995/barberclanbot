package com.ivan_degtev.telegrambotforpapablinov.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiMapper {

    private final ObjectMapper objectMapper;

    public String extractIdAfterCreateResponseMessage(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return jsonNode.path("id").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractRunStatus(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return jsonNode.path("status").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractLatestMessageId(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode dataArray = jsonNode.path("data");

            if (dataArray.isArray() && !dataArray.isEmpty()) {
                JsonNode latestMessage = dataArray.get(0);
                return latestMessage.path("id").asText();
            }
        } catch (Exception e) {
            log.error("Error parsing JSON to extract latest message ID", e);
        }
        return null;
    }

    public String extractDataFromLlmAnswer(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode contentArray = jsonNode.path("content");
            if (contentArray.isArray() && contentArray.size() > 0) {
                JsonNode firstContent = contentArray.get(0);
                return firstContent.path("text").path("value").asText();
            }
        } catch (Exception e) {
            log.error("Error extracting data from LLM answer", e);
        }
        return null;
    }

    public StringBuilder extractRoleAndContentFromMemory(String jsonString) {
        StringBuilder conversation = new StringBuilder();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode dataArray = jsonNode.path("data");

            if (dataArray.isArray() && !dataArray.isEmpty()) {
                for (JsonNode element : dataArray) {
                    String role = element.path("role").asText();
                    JsonNode contentArray = element.path("content");

                    if (contentArray.isArray() && !contentArray.isEmpty()) {
                        for (JsonNode contentNode : contentArray) {
                            String contentType = contentNode.path("type").asText();
                            if ("text".equals(contentType)) {
                                String value = contentNode.path("text").path("value").asText();
                                conversation.append(role).append(": ").append(value).append("\n");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing JSON to extract role and content", e);
        }

        return conversation;
    }

    /**
     * Метод выделяет JSON из строки, парсит его и возвращает Map с именами файлов и их ID.
     *
     * @param response Строка, содержащая JSON.
     * @return Map с ключами (именами файлов) и значениями (их ID).
     */
    public Map<String, String> extractFileIds(String response) {
        try {
            log.info("Received response for file ID extraction: {}", response);

            Pattern jsonPattern = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(response);

            if (matcher.find()) {
                String jsonString = matcher.group();
                JsonNode rootNode = objectMapper.readTree(jsonString);

                Map<String, String> fileMap = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fileName = field.getKey();
                    String fileId = field.getValue().asText();
                    fileMap.put(fileName, fileId);
                }
                log.info("Найденные ID файлов: {}", fileMap);
                return fileMap;
            } else {
                log.warn("JSON не найден в строке.");
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Ошибка при парсинге ответа на запрос поиска файлов", e);
            return Collections.emptyMap();
        }
    }

    public List<String> extractFileNamesById(String jsonString, String id) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonString);

        List<String> fileNames = new ArrayList<>();

        // Проверяем, что JSON содержит объект с данными
        if (root.has("data")) {
            for (JsonNode messageNode : root.get("data")) {
                // Сравниваем ID
                if (messageNode.has("id") && messageNode.get("id").asText().equals(id)) {
                    // Проходим по контенту
                    JsonNode contentNode = messageNode.get("content");
                    if (contentNode != null && contentNode.isArray()) {
                        for (JsonNode contentItem : contentNode) {
                            if (contentItem.has("text")) {
                                JsonNode textNode = contentItem.get("text");
                                if (textNode.has("annotations")) {
                                    JsonNode annotationsNode = textNode.get("annotations");
                                    if (annotationsNode.isArray()) {
                                        for (JsonNode annotation : annotationsNode) {
                                            if (annotation.has("text")) {
                                                String annotationText = annotation.get("text").asText();
                                                String fileName = extractFileNameFromAnnotation(annotationText);
                                                if (fileName != null) {
                                                    fileNames.add(fileName);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return fileNames;
    }

    private static String extractFileNameFromAnnotation(String annotationText) {
        // Регулярное выражение для извлечения содержимого внутри квадратных скобок, включая текст с пробелами и расширением файла
        String pattern = "【.*?†(.*?)】";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(annotationText);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public List<String> extractFileIds(String jsonString, String id) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonString);

        List<String> fileNames = new ArrayList<>();

        // Проверяем, что JSON содержит объект с данными
        if (root.has("data")) {
            for (JsonNode messageNode : root.get("data")) {
                // Сравниваем ID
                if (messageNode.has("id") && messageNode.get("id").asText().equals(id)) {
                    // Проходим по контенту
                    JsonNode contentNode = messageNode.get("content");
                    if (contentNode != null && contentNode.isArray()) {
                        for (JsonNode contentItem : contentNode) {
                            if (contentItem.has("text")) {
                                JsonNode textNode = contentItem.get("text");
                                if (textNode.has("annotations")) {
                                    JsonNode annotationsNode = textNode.get("annotations");
                                    if (annotationsNode.isArray()) {
                                        for (JsonNode annotation : annotationsNode) {
                                            if (annotation.has("file_citation") && annotation.get("file_citation").has("file_id")) {
                                                fileNames.add(annotation.get("file_citation").get("file_id").asText());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return fileNames;
    }

    public List<String> extractFileNamesFromJson(List<String> jsonStrings) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> fileNames = new ArrayList<>();

        for (String jsonString : jsonStrings) {
            JsonNode root = objectMapper.readTree(jsonString);

            if (root.has("filename")) {
                fileNames.add(root.get("filename").asText());
            }
        }

        return fileNames;
    }

}
