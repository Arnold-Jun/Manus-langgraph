package com.zhouruojun.manus.model;

import com.zhouruojun.manus.model.Memory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LLM {
    public final ChatModel chatModel;
    private String conversationId = "default";

    public Prompt prompt;

    public LLM(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 调用LLM进行对话
     * @param messages 消息列表
     * @return 异步返回LLM的响应内容
     */
    public CompletableFuture<String> call(List<com.zhouruojun.manus.model.Message> messages) {
        return CompletableFuture.supplyAsync(() -> {
            // 转换消息为Spring AI格式
            List<Message> aiMessages = convertToAiMessages(messages);

            // 创建提示
            Prompt prompt = new Prompt(aiMessages);

            // 调用模型并获取响应
            ChatResponse response = chatModel.call(prompt);

            this.prompt =prompt;
            // 返回响应内容
            return response.getResult().getOutput().getText();
        });
    }

    public CompletableFuture<ChatResponse> askTool(
            List<Message> messages,
            List<Message> systemMsgs,
            Memory memory,
            ChatOptions chatOptions,
            String conversationId) {

        return CompletableFuture.supplyAsync(() -> {
            // 转换消息为Spring AI格式
            List<Message> aiMessages = messages;
            // 添加系统消息
            if (systemMsgs != null && !systemMsgs.isEmpty()) {
                List<Message> sysAiMsgs = systemMsgs;
                for (Message sysMsg : sysAiMsgs) {
                    aiMessages.add(0, sysMsg); // 系统消息放在最前面
                }
            }

            // 创建带工具的提示
            Prompt prompt = new Prompt(aiMessages, chatOptions);

            memory.addMessages(conversationId, prompt.getInstructions());

            Prompt promptWithMemory = new Prompt(memory.getChatMemory().get(conversationId), chatOptions);

            this.prompt = promptWithMemory;
            // 调用模型并直接返回响应对象
            return chatModel.call(promptWithMemory);
        });
    }

    /**
     * 将消息列表转换为Spring AI消息格式
     * @param messages 消息列表
     * @return Spring AI消息列表
     */
    private List<Message> convertToAiMessages(List<com.zhouruojun.manus.model.Message> messages) {
        List<Message> aiMessages = new ArrayList<>();
        for (com.zhouruojun.manus.model.Message msg : messages) {
            aiMessages.add(convertToAiMessage(msg));
        }
        return aiMessages;
    }

    /**
     * 将单个消息转换为Spring AI消息格式
     * @param message 消息
     * @return Spring AI消息
     */
    private Message convertToAiMessage(com.zhouruojun.manus.model.Message message) {
        return switch (message.getRole()) {
            case USER -> new UserMessage(message.getContent());
            case SYSTEM -> new SystemMessage(message.getContent());
            case ASSISTANT -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };

    }
}
