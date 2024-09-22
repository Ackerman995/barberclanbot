package com.ivan_degtev.telegrambotforpapablinov.dto.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WebhookPayloadDto {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private MessageDto message;

    @JsonProperty("my_chat_member")
    private ChatMemberUpdateDto myChatMember; // Обновление информации о пользователе чата

    @JsonProperty("migrate_from_chat_id")
    private Long migrateFromChatId; // Поле для миграции чата

    @JsonProperty("sender_chat")
    private ChatDto senderChat; // Для обработки системных сообщений

    @Data
    public static class MessageDto {

        @JsonProperty("message_id")
        private Long messageId;

        @JsonProperty("from")
        private UserDto from;

        @JsonProperty("chat")
        private ChatDto chat;

        private Long date;
        private String text;

        @JsonProperty("entities")
        private List<EntityDto> entities;

        @JsonProperty("link_preview_options")
        private LinkPreviewOptions linkPreviewOptions;

        //Эти два поля используются для обработки сообщений в ответ на другие сообщения
        @JsonProperty("message_thread_id")
        private Long messageThreadId; // ID треда сообщения

        @JsonProperty("reply_to_message")
        private MessageDto replyToMessage; // Сообщение, на которое идет ответ


        @JsonProperty("document")
        private Document document;

        @JsonProperty("new_chat_participant")
        private UserDto newChatParticipant; // Новый участник чата

        @JsonProperty("new_chat_member")
        private UserDto newChatMember; // Новый участник чата (альтернативное название)

        @JsonProperty("left_chat_participant")
        private UserDto leftChatParticipant; // Вышедший участник чата

        @JsonProperty("left_chat_member")
        private UserDto leftChatMember; // Вышедший участник чата (альтернативное название)

        @JsonProperty("new_chat_members")
        private List<UserDto> newChatMembers; // Список новых участников чата
    }

    @Data
    public static class LinkPreviewOptions {
        @JsonProperty("url")
        private String url;
    }
    /**
     * Класс для хранения данных об отрпавленном(в нашей логике только ботом) файлах.
     */
    @Data
    public static class Document {
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("mime_type")
        private String mimeType;
        @JsonProperty("file_id")
        private String fileId;
        @JsonProperty("file_unique_id")
        private String fileUniqueId;
        @JsonProperty("file_size")
        private String fileSize;
    }

    @Data
    public static class ChatMemberUpdateDto {

        private ChatDto chat;

        @JsonProperty("from")
        private UserDto from; // Пользователь, который произвел изменения

        private Long date;

        @JsonProperty("old_chat_member")
        private ChatMemberDto oldChatMember; // Старое состояние пользователя чата

        @JsonProperty("new_chat_member")
        private ChatMemberDto newChatMember; // Новое состояние пользователя чата
    }

    /*
    Класс нужен для добавления участников группы, как админинов и разжалования
     */
    @Data
    public static class ChatMemberDto {

        private UserDto user;

        private String status;

        @JsonProperty("is_premium")
        private String isPremium;

        // Права администратора
        @JsonProperty("can_be_edited")
        private Boolean canBeEdited;

        @JsonProperty("can_manage_chat")
        private Boolean canManageChat;

        @JsonProperty("can_change_info")
        private Boolean canChangeInfo;

        @JsonProperty("can_delete_messages")
        private Boolean canDeleteMessages;

        @JsonProperty("can_invite_users")
        private Boolean canInviteUsers;

        @JsonProperty("can_restrict_members")
        private Boolean canRestrictMembers;

        @JsonProperty("can_pin_messages")
        private Boolean canPinMessages;

        @JsonProperty("can_manage_topics")
        private Boolean canManageTopics;

        @JsonProperty("can_promote_members")
        private Boolean canPromoteMembers;

        @JsonProperty("can_manage_video_chats")
        private Boolean canManageVideoChats;

        @JsonProperty("can_post_stories")
        private Boolean canPostStories;

        @JsonProperty("can_edit_stories")
        private Boolean canEditStories;

        @JsonProperty("can_delete_stories")
        private Boolean canDeleteStories;

        @JsonProperty("is_anonymous")
        private Boolean isAnonymous;

        @JsonProperty("can_manage_voice_chats")
        private Boolean canManageVoiceChats;

        @JsonProperty("custom_title")
        private String customTitle; // Пользовательский заголовок для администратора
    }

    @Data
    public static class UserDto {
        private Long id;

        @JsonProperty("is_bot")
        private boolean isBot;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String username;

        @JsonProperty("language_code")
        private String languageCode;

        @JsonProperty("is_premium")
        private String isPremium;
    }

    @Data
    public static class ChatDto {
        private Long id;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String username;
        private String type;

        private String title;

        @JsonProperty("is_premium")
        private String isPremium;
    }

    @Data
    public static class EntityDto {
        private int offset;
        private int length;
        private String type;
    }
}

