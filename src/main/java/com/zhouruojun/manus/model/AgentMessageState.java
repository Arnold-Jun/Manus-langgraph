package com.zhouruojun.manus.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;
import java.util.Optional;

/**
 * 基于LangGraph4j的智能体状态模型
 * 继承MessagesState以支持LangGraph4j的状态图功能
 */
public class AgentMessageState extends MessagesState<ChatMessage> {

    public AgentMessageState(Map<String, Object> initData) {
        super(initData);
    }

    // 状态字段访问方法
    
    /**
     * 获取下一个要执行的动作
     */
    public Optional<String> next() {
        return this.value("next");
    }
    
    /**
     * 获取当前用户输入
     */
    public Optional<String> userInput() {
        return this.value("userInput");
    }
    
    /**
     * 获取当前执行的智能体
     */
    public Optional<String> currentAgent() {
        return this.value("currentAgent");
    }
    
    /**
     * 获取任务执行结果
     */
    public Optional<String> result() {
        return this.value("result");
    }
    
    /**
     * 获取会话ID
     */
    public Optional<String> sessionId() {
        return this.value("sessionId");
    }
    
    /**
     * 检查是否完成
     */
    public boolean isFinished() {
        return this.<Boolean>value("finished").orElse(false);
    }
    
    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        Optional<String> error = this.value("error");
        return error.isPresent() && !error.get().trim().isEmpty();
    }
    
    /**
     * 获取错误信息
     */
    public Optional<String> error() {
        return this.value("error");
    }
    
    /**
     * 获取工具结果
     */
    public Optional<String> toolResults() {
        return this.value("toolResults");
    }

    /**
     * 创建用户消息
     */
    public static ChatMessage createUserMessage(String content) {
        return UserMessage.from(content);
    }

    /**
     * 创建AI消息
     */
    public static ChatMessage createAiMessage(String content) {
        return AiMessage.from(content);
    }

    /**
     * 创建系统消息
     */
    public static ChatMessage createSystemMessage(String content) {
        return SystemMessage.from(content);
    }
} 