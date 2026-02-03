package com.datn.identityservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.telegram")
@Data
public class TelegramProperties {
    private String botToken;
    private String chatId;
}
