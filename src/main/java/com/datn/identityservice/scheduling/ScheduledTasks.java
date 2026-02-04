package com.datn.identityservice.scheduling;

import com.datn.identityservice.repository.InvalidatedTokenRepository;
import com.datn.identityservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduledTasks {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(fixedRate = 3600000) // Quét mỗi giờ
    @Transactional
    public void cleanUnverifiedUsers() {
        log.info("--- Bắt đầu quét User trạng thái UNVERIFIED quá 24h ---");

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        long deletedCount = userRepository.deleteByStatusAndCreatedAtBefore("UNVERIFIED", cutoff);

        if (deletedCount > 0) {
            log.info("Đã dọn dẹp {} user chưa xác thực.", deletedCount);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("--- Bắt đầu dọn dẹp Blacklist Token đã hết hạn ---");

        invalidatedTokenRepository.deleteByExpiryAtBefore(Instant.now());

        log.info("Dọn dẹp Blacklist hoàn tất.");
    }
}