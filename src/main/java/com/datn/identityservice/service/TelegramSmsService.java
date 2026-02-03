package com.datn.identityservice.service;

import com.datn.identityservice.configuration.TelegramProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramSmsService {

    TelegramProperties telegramProperties;
    RestTemplate restTemplate = new RestTemplate();

    public void sendOtp(String phone, String otpCode) {
        String text = "--- [OTP VERIFICATION] ---\n" +
                "Phone: " + phone + "\n" +
                "Code: " + otpCode + "\n" +
                "Expires in: 2 minutes.";

        String url = "https://api.telegram.org/bot{token}/sendMessage?chat_id={chatId}&text={text}";

        Map<String, String> params = new HashMap<>();
        params.put("token", telegramProperties.getBotToken());
        params.put("chatId", telegramProperties.getChatId());
        params.put("text", text);

        try {
            log.info("Đang gửi OTP qua Telegram Bot...");
            restTemplate.getForObject(url, String.class, params);
            log.info("OTP đã gửi thành công đến Telegram của bạn!");
        } catch (Exception e) {
            log.error("Lỗi khi gọi API Telegram: {}", e.getMessage());
        }
    }
}