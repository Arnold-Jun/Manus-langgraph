package com.zhouruojun.manus.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@Slf4j
public class Memory {
    private ChatMemory chatMemory;

    private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;

    // 默认最大token限制
    private int maxTokens = 30000; // 设置安全限制，低于模型的最大限制32768

    // 默认构造函数
    public Memory() {
        this.chatMemory = MessageWindowChatMemory.builder().build();
    }

    // 带参数的构造函数
    public Memory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    // 带token限制的构造函数
    public Memory(int maxTokens) {
        this.chatMemory = MessageWindowChatMemory.builder().build();
        this.maxTokens = maxTokens;
    }

    /**
     * 添加消息并管理token数量
     * @param conversationId 会话ID
     * @param message 消息
     */
    public void addMessage(String conversationId, Message message) {
        chatMemory.add(conversationId, message);

        // 添加消息后检查并管理token数量
        manageTokenLimit(conversationId);
    }

    /**
     * 添加多条消息并管理token数量
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    public void addMessages(String conversationId, List<Message> messages) {
        chatMemory.add(conversationId, messages);

        // 添加消息后检查并管理token数量
        manageTokenLimit(conversationId);
    }

    /**
     * 管理token数量，确保不超过限制
     * @param conversationId 会话ID
     */
    private void manageTokenLimit(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        int estimatedTokens = TokenManager.estimateTokenCount(messages);

        // 如果估算的token数接近限制，进行截断处理
        if (estimatedTokens > maxTokens) {
            log.warn("消息历史token数({})超过限制({}), 开始截断旧消息...", estimatedTokens, maxTokens);
            truncateMessages(conversationId, messages);
        }
    }

    /**
     * 截断消息，优先保留系统消息和最近的消息
     * @param conversationId 会话ID
     * @param messages 当前消息列表
     */
    private void truncateMessages(String conversationId, List<Message> messages) {
        if (messages.size() <= 2) {
            return; // 至少保留最新的两条消息
        }

        // 分离系统消息和其他消息
        List<Message> systemMessages = new ArrayList<>();
        List<Message> otherMessages = new ArrayList<>();

        for (Message msg : messages) {
            if (msg instanceof SystemMessage) {
                systemMessages.add(msg);
            } else {
                otherMessages.add(msg);
            }
        }

        // 计算系统消息的token数
        int systemTokens = TokenManager.estimateTokenCount(systemMessages);

        // 计算需要保留的非系统消息
        List<Message> messagesToKeep = new ArrayList<>(systemMessages);

        // 从最新的消息开始添加，直到接近但不超过限制
        int currentTotal = systemTokens;
        int safetyBuffer = 2000; // 安全边界

        for (int i = otherMessages.size() - 1; i >= 0; i--) {
            Message msg = otherMessages.get(i);
            int msgTokens = TokenManager.estimateTokensForMessage(msg);

            if (currentTotal + msgTokens <= maxTokens - safetyBuffer) {
                messagesToKeep.add(msg);
                currentTotal += msgTokens;
            } else {
                // 继续尝试添加最早的用户消息，可能包含重要上下文
                if (i < 3 && currentTotal + msgTokens <= maxTokens) {
                    messagesToKeep.add(msg);
                    currentTotal += msgTokens;
                }
            }
        }

        log.info("消息截断完成: 从{}条消息中保留{}条, 当前token估算: {}",
                messages.size(), messagesToKeep.size(), currentTotal);

        // 清除现有消息并添加保留的消息
        chatMemory.clear(conversationId);
        chatMemory.add(conversationId, messagesToKeep);
    }

    public List<Message> getMessages() {
        return chatMemory.get(conversationId);
    }

    public void clearMemory() {
        chatMemory.clear(conversationId);
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    /**
     * 设置最大token限制
     * @param maxTokens 最大token数
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * 获取当前会话估算的token数量
     * @return 当前会话token数量
     */
    public int getCurrentTokenCount() {
        return TokenManager.estimateTokenCount(getMessages());
    }
}
