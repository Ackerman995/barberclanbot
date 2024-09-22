package com.ivan_degtev.telegrambotforpapablinov.controller;

import com.ivan_degtev.telegrambotforpapablinov.service.WebhookService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/webhook")
public class WebhookUtilController {

    private final WebhookService webhookService;

    public WebhookUtilController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/create")
    public String setWebhook() {
        return webhookService.createWebhook();
    }

    @GetMapping("/info")
    public String getWebhookInfo() {
        return webhookService.getAllWebhooks();
    }

    @DeleteMapping("/delete")
    public String deleteWebhook() {
        return webhookService.deleteWebhook();
    }
}
