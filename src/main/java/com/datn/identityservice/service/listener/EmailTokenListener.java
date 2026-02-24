package com.datn.identityservice.service.listener;

import com.datn.identityservice.configuration.AppUrlProperties;
import com.datn.identityservice.dto.event.RegistrationCompleteEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailTokenListener {

    JavaMailSender mailSender;
    AppUrlProperties appUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistrationEvent(RegistrationCompleteEvent event) {
        log.info("Transaction đã commit. Bắt đầu gửi email tới: {}", event.getEmail());

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(event.getEmail());
            mailMessage.setSubject("Xác thực tài khoản của bạn");

//            String verifyUrl = String.format("%s/identity/auth/verify?token=%s",
//                    appUrl.getBase(), event.getToken());

            String verifyUrl = String.format("%s/identity/auth/verify-email/%s",
                    appUrl.getBase(), event.getToken());

            mailMessage.setText("Chào bạn, vui lòng click vào link sau để kích hoạt tài khoản: " + verifyUrl);

            mailSender.send(mailMessage);
            log.info("Đã gửi email thành công!");
        } catch (Exception e) {
            log.error("Gửi email thất bại sau khi commit: {}", e.getMessage());
        }
    }
}
