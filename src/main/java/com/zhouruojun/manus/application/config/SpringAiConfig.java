package com.zhouruojun.manus.application.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class SpringAiConfig {

    /**
     * 确保在workflow profile下也提供ChatModel Bean
     * 这样WorkflowMultiAgentApplication可以正确注入ChatModel
     */
    @Bean
    @Primary
    @Profile("workflow")
    public ChatModel ensureWorkflowChatModel(ChatModel openAiChatModel) {
        return openAiChatModel;
    }
}
