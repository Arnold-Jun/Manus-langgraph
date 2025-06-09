package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

/**
 * LangGraph4j节点基础类
 * 实现NodeAction接口以支持LangGraph4j状态图
 */
@Slf4j
public abstract class BaseNode implements NodeAction<AgentMessageState> {

    protected final ChatModel chatModel;

    public BaseNode(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Map<String, Object> apply(AgentMessageState state) throws Exception {
        try {
            log.info("Processing node: {}", getNodeName());
            return processNode(state);
        } catch (Exception e) {
            log.error("Error in node {}: {}", getNodeName(), e.getMessage(), e);
            return Map.of(
                "error", "节点执行错误: " + e.getMessage(),
                "messages", state.lastMessage().orElse(AgentMessageState.createAiMessage("节点执行错误"))
            );
        }
    }

    /**
     * 具体的节点处理逻辑
     * @param state 当前状态
     * @return 更新后的状态数据
     */
    protected abstract Map<String, Object> processNode(AgentMessageState state) throws Exception;

    /**
     * 获取节点名称
     */
    protected abstract String getNodeName();

    /**
     * 调用ChatModel获取响应
     */
    protected String callChatModel(String systemPrompt, String userInput) {
        try {
            List<org.springframework.ai.chat.messages.Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput)
            );
            
            Prompt prompt = new Prompt(messages);
            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error calling chat model", e);
            return "调用语言模型时发生错误: " + e.getMessage();
        }
    }

    /**
     * 解析下一步动作
     */
    protected String parseNextAction(String response) {
        String lowercaseResponse = response.toLowerCase();
        
        if (lowercaseResponse.contains("搜索") || lowercaseResponse.contains("search")) {
            return "search";
        } else if (lowercaseResponse.contains("分析") || lowercaseResponse.contains("analysis")) {
            return "analysis";
        } else if (lowercaseResponse.contains("总结") || lowercaseResponse.contains("summary")) {
            return "summary";
        } else if (lowercaseResponse.contains("用户输入") || lowercaseResponse.contains("human_input")) {
            return "human_input";
        } else if (lowercaseResponse.contains("完成") || lowercaseResponse.contains("finish")) {
            return "FINISH";
        }
        
        return "FINISH"; // 默认完成
    }
} 