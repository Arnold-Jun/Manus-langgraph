package com.zhouruojun.manus.core.specialized;

import com.zhouruojun.manus.core.base.ToolCallAgent;
import com.zhouruojun.manus.tools.collection.ToolCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.UUID;

/**
 * 总结智能体
 * 专注于信息汇总、整合信息并生成最终报告
 */
@Slf4j
public class SummaryAgent extends AbstractSpecializedAgent {

    /**
     * 创建一个总结智能体实例
     *
     * @param chatModel 聊天模型
     * @param summaryTools 总结相关工具集合
     * @param name 智能体名称
     */
    public SummaryAgent(ChatModel chatModel, ToolCollection summaryTools, String name) {
        super(createDelegate(chatModel, summaryTools, name));

        // 设置总结专家的系统提示词
        setSystemPrompt(
                "你是一个专业的总结智能体，擅长信息整合和报告生成。\n" +
                "你的主要职责是：\n" +
                "1. 整合来自不同来源的信息和分析结果\n" +
                "2. 提炼关键内容，确保信息完整而简洁\n" +
                "3. 按照逻辑结构组织内容\n" +
                "4. 生成清晰、连贯的最终报告\n\n" +
                "当你收到任务时，请先了解总结目标和受众，然后整合所有材料，最后输出格式良好的报告。\n" +
                "如有必要，可以要求分析智能体提供更深入的见解，或要求搜索智能体补充缺失信息。" +
                "重要提示：\n" +
                "- 搜索完成后必须调用terminate(\"success\", \"" + name + "\")工具结束工作流\n" +
                "- 如果无法完成搜索，则调用terminate(\"failure\", \"" + name + "\")工具\n" +
                "- 确保你的最终回复包含完整的搜索结果，因为这将作为结果返回给协调器\n"
        );

        log.info("总结智能体初始化完成，ID: {}", getConversationId());
    }

    /**
     * 创建委托的ToolCallAgent实例
     */
    private static ToolCallAgent createDelegate(ChatModel chatModel, ToolCollection tools, String name) {
        return ToolCallAgent.builder()
                .name(name)
                .description("专注于信息汇总和报告生成的智能体，擅长整合分析结果和生成清晰报告。")
                .chatModel(chatModel)
                .availableTools(tools)
                .conversationId(UUID.randomUUID().toString())
                .build();
    }
}
