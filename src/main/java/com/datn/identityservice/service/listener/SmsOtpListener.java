package com.datn.identityservice.service.listener;

import com.datn.identityservice.dto.event.SmsOtpEvent;
import com.datn.identityservice.service.TelegramSmsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SmsOtpListener {

    TelegramSmsService telegramSmsService;

    @EventListener //@TransactionalEventListener sang @EventListener
    @Async
    public void handleSmsOtpEvent(SmsOtpEvent event) {
        log.info(">>>> ĐÃ NHẬN ĐƯỢC SỰ KIỆN: Đang chuẩn bị gọi TelegramService cho số {}", event.getPhone());
        try {
            telegramSmsService.sendOtp(event.getPhone(), event.getOtpCode());
        } catch (Exception e) {
            log.error("Lỗi thực thi gửi Telegram: {}", e.getMessage());
        }
    }
}