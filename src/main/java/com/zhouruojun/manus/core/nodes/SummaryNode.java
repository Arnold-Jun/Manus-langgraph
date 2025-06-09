package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 总结节点 - 负责信息整合和报告生成
 */
@Slf4j
public class SummaryNode extends BaseNode {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的总结智能体，擅长信息整合和报告生成。
        
        你的主要职责是：
        1. 整合所有可用的信息和分析结果
        2. 生成清晰、结构化的总结报告
        3. 突出关键发现和重要结论
        4. 提供可行的建议或下一步行动
        
        请将所有信息整合成一个全面、准确的总结报告。
        """;

    public SummaryNode(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建总结上下文
        StringBuilder summaryContext = new StringBuilder();
        summaryContext.append("总结任务: ").append(userInput).append("\n");
        
        // 包含所有工具结果
        String existingToolResults = state.toolResults().orElse("");
        if (!existingToolResults.isEmpty()) {
            summaryContext.append("需要总结的信息: \n").append(existingToolResults).append("\n");
        }
        
        summaryContext.append("请生成一个完整的总结报告。");
        
        // 调用语言模型生成总结
        String summaryResult = callChatModel(SYSTEM_PROMPT, summaryContext.toString());
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage("总结报告: " + summaryResult);
        
        // 构建工具结果字符串，追加到现有结果
        String updatedToolResults = existingToolResults.isEmpty() ? 
            "summary: " + summaryResult : 
            existingToolResults + "\nsummary: " + summaryResult;
        
        // 更新状态并返回
        return Map.of(
            "currentAgent", "summary",
            "toolResults", updatedToolResults,
            "result", summaryResult,  // 设置最终结果
            "next", "coordinator",
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "SummaryNode";
    }
} 