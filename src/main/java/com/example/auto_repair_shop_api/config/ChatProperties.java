package com.example.auto_repair_shop_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    private String currency = "EUR";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);
    private int maxTokens = 400;
    private double temperature = 0.2;
    private int dailyLimit = 300;
    private final RateLimit rateLimit = new RateLimit();
    private final Cloudflare cloudflare = new Cloudflare();

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Cloudflare getCloudflare() {
        return cloudflare;
    }

    public static class RateLimit {

        private int maxRequests = 10;
        private Duration window = Duration.ofSeconds(60);

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    public static class Cloudflare {

        private String accountId = "";
        private String apiToken = "";
        private String model = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isConfigured() {
            return hasText(accountId) && hasText(apiToken) && hasText(model);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
