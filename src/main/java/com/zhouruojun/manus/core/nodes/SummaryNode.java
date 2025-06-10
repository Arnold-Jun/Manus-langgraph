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
        你是一个专业的回复生成智能体，负责为用户生成最终的回复。
        
        你的主要职责是：
        1. 分析用户的原始请求和所有可用信息
        2. 如果有工具执行结果，进行整合和总结
        3. 如果没有复杂信息，直接针对用户请求给出回复
        4. 生成清晰、自然、有用的最终回复
        
        重要要求：
        - 直接回复用户，不要使用JSON格式
        - 回复要自然、友好、有帮助
        - 如果有多个信息源，要进行合理整合
        - 突出关键信息和结论
        """;

    public SummaryNode(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建回复生成上下文
        StringBuilder replyContext = new StringBuilder();
        replyContext.append("用户问题: ").append(userInput).append("\n");
        
        // 检查是否有工具执行结果
        String existingToolResults = state.toolResults().orElse("");
        if (!existingToolResults.isEmpty()) {
            replyContext.append("可用信息: \n").append(existingToolResults).append("\n");
            replyContext.append("请基于以上信息为用户生成一个完整、有用的回复。");
        } else {
            replyContext.append("请直接针对用户的问题生成一个有帮助的回复。");
        }
        
        // 调用语言模型生成最终回复
        String finalReply = callChatModel(SYSTEM_PROMPT, replyContext.toString());
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage(finalReply);
        
        // 返回最终状态 - 设置finished=true来结束工作流
        return Map.of(
            "currentAgent", "summary",
            "result", finalReply,  // 设置最终结果
            "finished", true,      // 标记工作流完成
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "SummaryNode";
    }
} 