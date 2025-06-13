package com.zhouruojun.manus.domain.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体状态模型
 * 用于管理智能体的状态和消息历史
 */
public class AgentMessageState extends MessagesState<ChatMessage> {
    private final Map<String, Object> state;

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
        "messages", Channels.appender(ArrayList::new),
        "sessionHistory", Channels.appender(ArrayList::new),
        "currentMessage", Channels.appender(ArrayList::new),
        "nextNode", Channels.appender(ArrayList::new),
        "isFinished", Channels.appender(ArrayList::new),
        "result", Channels.appender(ArrayList::new),
        "toolResults", Channels.appender(ArrayList::new),
        "hasHistory", Channels.appender(ArrayList::new)
    );

    public AgentMessageState(Map<String, Object> initData) {
        super(initData);
        this.state = new ConcurrentHashMap<>(initData);
    }

    public List<ChatMessage> messages() {
        return this.<List<ChatMessage>>value("messages")
                .orElse(new ArrayList<>());
    }

    public List<ChatMessage> sessionHistory() {
        return this.<List<ChatMessage>>value("sessionHistory")
                .orElse(new ArrayList<>());
    }

    public Optional<ChatMessage> lastMessage() {
        List<ChatMessage> messages = messages();
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(messages.get(messages.size() - 1));
    }

    public Optional<String> currentMessage() {
        return this.value("currentMessage");
    }

    public Optional<String> next() {
        return this.value("nextNode");
    }

    public boolean isFinished() {
        return this.<Boolean>value("isFinished").orElse(false);
    }

    public Optional<String> result() {
        return this.value("result");
    }

    /**
     * 获取工具结果列表
     */
    @SuppressWarnings("unchecked")
    public List<String> toolResults() {
        return this.<List<String>>value("toolResults")
                .orElse(new ArrayList<>());
    }

    public void addMessage(ChatMessage message) {
        List<ChatMessage> messages = messages();
        messages.add(message);
        state.put("messages", messages);
    }

    public void addToSessionHistory(ChatMessage message) {
        List<ChatMessage> history = sessionHistory();
        history.add(message);
        state.put("sessionHistory", history);
    }

    public void setCurrentMessage(String message) {
        state.put("currentMessage", message);
    }

    public void setNext(String nextNode) {
        state.put("nextNode", nextNode);
    }

    public void setFinished(boolean finished) {
        state.put("isFinished", finished);
    }

    public void setResult(String result) {
        state.put("result", result);
    }

    public void addToolResult(String result) {
        List<String> results = toolResults();
        results.add(result);
        state.put("toolResults", results);
    }

    public static ChatMessage fromChatMessage(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return UserMessage.from(userMessage.toString());
        } else if (message instanceof AiMessage aiMessage) {
            return AiMessage.from(aiMessage.toString());
        } else if (message instanceof SystemMessage systemMessage) {
            return SystemMessage.from(systemMessage.toString());
        }
        throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
    }

    public static AgentMessageState createMessagesWithHistory(String userInput, List<Message> sessionHistory) {
        Map<String, Object> initData = new HashMap<>();
        List<ChatMessage> messages = new ArrayList<>();
        List<ChatMessage> history = new ArrayList<>();

        if (sessionHistory != null) {
            for (Message msg : sessionHistory) {
                org.springframework.ai.chat.messages.Message springMessage = msg.getSpringMessage();
                if (springMessage instanceof org.springframework.ai.chat.messages.UserMessage userMessage) {
                    history.add(UserMessage.from(userMessage.getText()));
                } else if (springMessage instanceof org.springframework.ai.chat.messages.AssistantMessage assistantMessage) {
                    history.add(AiMessage.from(assistantMessage.getText()));
                } else if (springMessage instanceof org.springframework.ai.chat.messages.SystemMessage systemMessage) {
                    history.add(SystemMessage.from(systemMessage.getText()));
                }
            }
        }

        messages.add(UserMessage.from(userInput));
        
        initData.put("messages", messages);
        initData.put("sessionHistory", history);
        initData.put("currentMessage", userInput);
        initData.put("isFinished", false);
        initData.put("hasHistory", sessionHistory != null && !sessionHistory.isEmpty());
        
        return new AgentMessageState(initData);
    }

    public static AgentMessageState createUserMessage(String userInput) {
        return createMessagesWithHistory(userInput, null);
    }

    /**
     * 创建AI消息
     */
    public static ChatMessage createAiMessage(String content) {
        return AiMessage.from(content);
    }

    /**
     * 检查是否包含历史记录
     */
    public boolean hasHistory() {
        return this.<Boolean>value("hasHistory").orElse(false);
    }

    /**
     * 获取当前用户输入
     */
    public Optional<String> userInput() {
        return this.value("userInput");
    }
}
