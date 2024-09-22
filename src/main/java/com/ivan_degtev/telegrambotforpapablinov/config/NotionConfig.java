package com.ivan_degtev.telegrambotforpapablinov.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NotionConfig {

    @Value("${notion.api.token}")
    private String notionToken;

    @Bean
    public WebClient notionWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + notionToken)
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
    }
}
