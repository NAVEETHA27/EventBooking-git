package com.eventbooking.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to configure primary Spring AI ChatModel using Google Gemini only.
 */
@Configuration
public class SpringAIConfig {

    @Bean
    @Primary
    @ConditionalOnBean(GoogleGenAiChatModel.class)
    public ChatModel primaryChatModel(GoogleGenAiChatModel googleChatModel) {
        return googleChatModel;
    }
}
