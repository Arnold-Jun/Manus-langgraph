package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 搜索节点 - 负责信息搜索和资料收集
 */
@Slf4j
public class SearchNode extends BaseNode {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的搜索智能体，擅长信息检索和资料查找。
        
        你的主要职责是：
        1. 理解用户的搜索需求，提取关键信息点
        2. 模拟搜索过程，提供相关信息
        3. 整理和组织搜索结果，确保信息的相关性和准确性
        4. 按照清晰的结构返回搜索结果
        
        请根据用户的需求进行搜索，并返回结构化的搜索结果。
        """;

    public SearchNode(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建搜索上下文
        StringBuilder searchContext = new StringBuilder();
        searchContext.append("搜索任务: ").append(userInput).append("\n");
        searchContext.append("请执行搜索并提供相关信息。");
        
        // 调用语言模型执行搜索
        String searchResult = callChatModel(SYSTEM_PROMPT, searchContext.toString());
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage("搜索结果: " + searchResult);
        
        // 构建工具结果字符串
        String toolResults = "search: " + searchResult;
        
        // 更新状态并返回
        return Map.of(
            "currentAgent", "search",
            "toolResults", toolResults,
            "next", "coordinator",
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "SearchNode";
    }
} 