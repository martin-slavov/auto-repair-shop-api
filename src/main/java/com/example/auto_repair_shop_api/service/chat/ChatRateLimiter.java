package com.example.auto_repair_shop_api.service.chat;

import com.example.auto_repair_shop_api.config.ChatProperties;
import com.example.auto_repair_shop_api.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChatRateLimiter {

    public static final String CLIENT_LIMIT_MESSAGE = "Too many chat requests. Please try again in a minute.";
    public static final String DAILY_LIMIT_MESSAGE =
            "The assistant has reached its daily limit. Please try again tomorrow.";
    private static final int CLEANUP_THRESHOLD = 1000;

    private final ChatProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    private LocalDate currentDay;
    private int dailyCount;

    public ChatRateLimiter(ChatProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized void acquire(String clientIp) {
        Instant now = clock.instant();
        Duration windowLength = properties.getRateLimit().getWindow();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> window.isExpired(now, windowLength));
        }

        Window window = windows.get(clientIp);
        if (window == null || window.isExpired(now, windowLength)) {
            window = new Window(now);
            windows.put(clientIp, window);
        }
        if (window.count >= properties.getRateLimit().getMaxRequests()) {
            throw new RateLimitExceededException(CLIENT_LIMIT_MESSAGE,
                    secondsUntil(now, window.start.plus(windowLength)));
        }

        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        if (!today.equals(currentDay)) {
            currentDay = today;
            dailyCount = 0;
        }
        if (dailyCount >= properties.getDailyLimit()) {
            throw new RateLimitExceededException(DAILY_LIMIT_MESSAGE,
                    secondsUntil(now, today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        window.count++;
        dailyCount++;
    }

    private static long secondsUntil(Instant now, Instant end) {
        long millis = Duration.between(now, end).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private static final class Window {

        private final Instant start;
        private int count;

        private Window(Instant start) {
            this.start = start;
        }

        private boolean isExpired(Instant now, Duration length) {
            return !now.isBefore(start.plus(length));
        }
    }
}
