package com.ivan_degtev.telegrambotforpapablinov.service.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface Assistant {

    @SystemMessage("""
            Вы внутренний ассистент компании Папа Блинов. Твоя задача отвечать на общие вопросы, вопросы связанные с компанией
            и предоставлять данные из внутренней базы данных, когда я запрашивают.
            При  общении всегда запоминай айди чата клиента {{currentChatId}}
            """)
    @UserMessage("""
            Вопрос клиента: {{userMessage}}
            """)
    String chat(
            @MemoryId String memoryId,
            @V("currentChatId") String currentChatId,
            @V("userMessage")String userMessage
    );

}
