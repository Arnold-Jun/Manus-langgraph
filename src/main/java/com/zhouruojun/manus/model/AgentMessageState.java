package com.zhouruojun.manus.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

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

    /**
     * 创建包含历史记录的消息列表
     * @param currentUserInput 当前用户输入
     * @param historyMessages 历史消息列表
     * @return 转换后的ChatMessage对象
     */
    public static ChatMessage createMessagesWithHistory(String currentUserInput, List<Message> historyMessages) {
        // 对于LangGraph4j，我们需要返回一个ChatMessage对象
        // 但是我们可以在消息内容中包含历史记录的摘要
        StringBuilder contentWithHistory = new StringBuilder();

        // 添加历史摘要
        if (historyMessages != null && !historyMessages.isEmpty()) {
            contentWithHistory.append("【以下是之前的对话历史摘要】\n");
            for (Message msg : historyMessages) {
                // 使用 getter 方法获取 role 和 content
                Role role = msg.getRole();
                String roleStr = (role != null) ? role.getValue() : "unknown";
                String rolePrefix = "user".equals(roleStr) ? "用户: " : "系统: ";

                // 获取内容并检查长度
                String content = msg.getContent();
                if (content != null && content.length() > 100) {
                    content = content.substring(0, 97) + "...";
                }
                contentWithHistory.append(rolePrefix).append(content).append("\n");
            }
            contentWithHistory.append("【历史摘要结束】\n\n");
        }

        // 添加当前用户输入
        contentWithHistory.append("【当前问题】\n").append(currentUserInput);

        // 创建用户消息
        return UserMessage.from(contentWithHistory.toString());
    }

    /**
     * 检查是否包含历史记录
     */
    public boolean hasHistory() {
        return this.<Boolean>value("hasHistory").orElse(false);
    }

    /**
     * 获取会话历史记录
     */
    @SuppressWarnings("unchecked")
    public List<Message> getSessionHistory() {
        return this.<List<Message>>value("sessionHistory").orElse(new ArrayList<>());
    }
}
