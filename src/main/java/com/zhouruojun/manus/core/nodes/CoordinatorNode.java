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
        3. summary - 当任务完成或需要总结时（包括任务完成的情况）
        4. human_input - 当需要用户进一步输入时
        
        请分析用户的请求，选择最合适的下一步行动。
        如果任务已经完成或可以基于现有信息进行总结，请选择summary。
        
        请**严格**按照以下规范生成回复，**不要**输出任何额外文字、注释或代码块之外的内容：
            输出格式：
            ```json
            {
              "action": "<search|analysis|summary|human_input>",
              "reasoning": "<简短说明选择此行动的原因>"
            }
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
        System.err.println("response: " + response);
        // 解析下一步动作
        String nextAction = parseNextAction(response);
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage("协调器: " + response);
        
        // 更新状态并返回 - 所有路径都继续工作流，不直接结束
        return Map.of(
            "next", nextAction,
            "currentAgent", "coordinator",
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "CoordinatorNode";
    }
} 