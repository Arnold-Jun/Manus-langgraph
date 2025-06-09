package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 协调器节点 - 负责分析任务并决定下一步行动
 * 基于LangGraph4j的NodeAction实现
 */
@Slf4j
public class CoordinatorNode extends BaseNode {

    private static final String SYSTEM_PROMPT = """
        你是一个智能体协调器，负责分析用户请求并决定下一步的行动。
        
        你可以选择以下行动：
        1. search - 当需要搜索信息时
        2. analysis - 当需要分析数据时
        3. summary - 当需要总结信息时
        4. human_input - 当需要用户进一步输入时
        5. FINISH - 当任务完成时
        
        请分析用户的请求，选择最合适的下一步行动。
        如果任务已经完成，请回复包含"完成"或"FINISH"的响应。
        如果需要执行某个动作，请在回复中明确包含动作名称。
        """;

    public CoordinatorNode(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建上下文信息
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("用户请求: ").append(userInput).append("\n");
        
        // 如果有之前的工具结果，包含进来
        String toolResults = state.toolResults().orElse("");
        if (!toolResults.isEmpty()) {
            contextBuilder.append("之前的执行结果: \n").append(toolResults).append("\n");
        }
        
        // 如果有执行结果，检查是否需要进一步处理
        String currentResult = state.result().orElse("");
        if (!currentResult.isEmpty()) {
            contextBuilder.append("当前结果: ").append(currentResult).append("\n");
            contextBuilder.append("请判断任务是否已经完成，还是需要进一步处理。");
        }
        
        String context = contextBuilder.toString();
        
        // 调用语言模型进行决策
        String response = callChatModel(SYSTEM_PROMPT, context);
        
        // 解析下一步动作
        String nextAction = parseNextAction(response);
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage(response);
        
        // 更新状态并返回
        if ("FINISH".equals(nextAction)) {
            return Map.of(
                "next", "",
                "currentAgent", "coordinator",
                "finished", true,
                "result", currentResult.isEmpty() ? response : currentResult,
                "messages", aiMessage
            );
        } else {
            return Map.of(
                "next", nextAction,
                "currentAgent", "coordinator",
                "messages", aiMessage
            );
        }
    }

    @Override
    protected String getNodeName() {
        return "CoordinatorNode";
    }
} 