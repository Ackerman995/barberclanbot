package com.ivan_degtev.telegrambotforpapablinov.service.impl;

import com.ivan_degtev.telegrambotforpapablinov.dto.TYPE_REQUEST;
import com.ivan_degtev.telegrambotforpapablinov.service.UpdateIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements UpdateIdService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UPDATE_ID_PREFIX = "update_id:";
    private static final String TYPE_REQUEST_FROM_ID = "from_id:";

    private static final String ID_ACTUAL_THREAD = "user:threads:";
    private static final String MESSAGES_COUNT_KEY = "user:messagesCount:";

    /**
     * Для фильтрации множественной отправки дублей из нгрока, пропускает только 1 апдейт с уник айди
     * @param updateId
     * @return
     */
    public boolean isUniqueUpdateId(Long updateId) {
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(UPDATE_ID_PREFIX + updateId, "1", Duration.ofHours(24));
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Сохраняет тип запроса для каждою юзера - нужно для понимая идет обычный диалог или запрос на выкачку файлов
     */
    public void saveUserRequestType(String fromId, TYPE_REQUEST typeRequest) {
        redisTemplate.opsForValue().set(TYPE_REQUEST_FROM_ID + fromId, typeRequest, Duration.ofHours(72));
        log.info("Сохранен тип запроса для пользователя {}: {}", fromId, typeRequest);
    }

    /**
     * Возвращает текущий тип диалога для дальнейше работой с ллм
     */
    public TYPE_REQUEST getUserRequestType(String fromId) {
        TYPE_REQUEST typeRequest =  TYPE_REQUEST.valueOf(Objects.requireNonNull(redisTemplate.opsForValue().get(TYPE_REQUEST_FROM_ID + fromId)).toString());

        if (typeRequest != null) {
            log.info("Найден тип запроса для пользователя {}: {}", fromId, typeRequest);
            return typeRequest;
        } else {
            log.info("Тип запроса для пользователя {} не найден в Redis", fromId);
            return TYPE_REQUEST.DEFAULT;
        }
    }

    /**
     * Методы для сохранения данных о тредах для юзера и коол-ве сообщений, нужно для контроля и удаления старых тредов, когда кол-во сообщений в текущем превысит 10
     */
    // Получение треда пользователя
    public String getUserThread(String fromId) {
        return (String) redisTemplate.opsForValue().get(ID_ACTUAL_THREAD + fromId);
    }

    // Сохранение треда пользователя
    public void setUserThread(String fromId, String threadId) {
        redisTemplate.opsForValue().set(ID_ACTUAL_THREAD + fromId, threadId);
    }

    // Получение количества сообщений пользователя
    public String getUserMessageCount(String fromId) {
        String countStr = (String) redisTemplate.opsForValue().get(MESSAGES_COUNT_KEY + fromId);
//        Integer count = countStr != null ? Integer.parseInt(countStr) : 0;
        log.info("Получил данные о кол-ве сообщений у конкретного юзера с id {} в треде из редиса {}", fromId, countStr);
        return countStr;
    }

    // Инкремент количества сообщений
    public void incrementUserMessageCount(String fromId) {
        if (redisTemplate.opsForValue().get(MESSAGES_COUNT_KEY + fromId) == null) {
            redisTemplate.opsForValue().set(MESSAGES_COUNT_KEY + fromId, "0");
        }
        redisTemplate.opsForValue().increment(MESSAGES_COUNT_KEY + fromId, 1);
    }
    public void incrementUserMessageCount(String fromId, String currentCount) {
        redisTemplate.opsForValue().set(MESSAGES_COUNT_KEY + fromId, currentCount);
    }

    // Сброс количества сообщений
    public void resetUserMessageCount(String fromId) {
        redisTemplate.opsForValue().set(MESSAGES_COUNT_KEY + fromId, "1");
    }

    // Удаление старого треда и связанной информации
    public void deleteOldThread(String fromId) {
        redisTemplate.delete(ID_ACTUAL_THREAD + fromId);
        redisTemplate.delete(MESSAGES_COUNT_KEY + fromId);
    }
}
