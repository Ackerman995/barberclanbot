package com.ivan_degtev.telegrambotforpapablinov.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
