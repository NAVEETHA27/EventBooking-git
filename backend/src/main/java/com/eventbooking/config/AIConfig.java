package com.eventbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eventgpt")
public class AIConfig {
    private String chatModel = "gemini-2.5-flash";
    private int maxHistoryMessages = 10;
    private int maxContextDocuments = 12;
    private VectorDb vectorDb = new VectorDb();

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public int getMaxContextDocuments() {
        return maxContextDocuments;
    }

    public void setMaxContextDocuments(int maxContextDocuments) {
        this.maxContextDocuments = maxContextDocuments;
    }

    public VectorDb getVectorDb() {
        return vectorDb;
    }

    public void setVectorDb(VectorDb vectorDb) {
        this.vectorDb = vectorDb;
    }

    public static class VectorDb {
        private String provider = "mysql";
        private String url = "";
        private String username = "";
        private String password = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
