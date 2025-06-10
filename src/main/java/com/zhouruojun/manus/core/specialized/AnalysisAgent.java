package com.zhouruojun.manus.core.specialized;

import com.zhouruojun.manus.core.base.ToolCallAgent;
import com.zhouruojun.manus.tools.collection.ToolCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.UUID;

/**
 * 分析智能体
 * 专注于数据分析和信息处理
 */
@Slf4j
public class AnalysisAgent extends AbstractSpecializedAgent {

    /**
     * 创建一个分析智能体实例
     *
     * @param chatModel 聊天模型
     * @param analysisTools 分析相关工具集合
     * @param name 智能体名称
     */
    public AnalysisAgent(ChatModel chatModel, ToolCollection analysisTools, String name) {
        super(createDelegate(chatModel, analysisTools, name));

        // 设置分析专家的系统提示词
        setSystemPrompt(
                "你是一个专业的分析智能体，擅长数据分析和信息处理。\n" +
                "你的主要职责是：\n" +
                "1. 对收到的数据进行深入分析和解读\n" +
                "2. 识别数据中的模式、趋势和关联性\n" +
                "3. 提取关键见解和重要发现\n" +
                "4. 生成基于数据的结论和建议\n\n" +
                "当你收到任务时，请先了解分析目标，然后选择合适的分析方法，最后执行分析并提供结果。\n" +
                "如果需要更多数据支撑，可以请求搜索智能体获取；如需生成最终报告，可以将分析结果传递给总结智能体。" +
                "重要提示：\n" +
                "- 搜索完成后必须调用terminate(\"success\", \"" + name + "\")工具结束工作流\n" +
                "- 如果无法完成搜索，则调用terminate(\"failure\", \"" + name + "\")工具\n" +
                "- 确保你的最终回复包含完整的搜索结果，因为这将作为结果返回给协调器\n"
        );

        log.info("分析智能体初始化完成，ID: {}", getConversationId());
    }

    /**
     * 创建委托的ToolCallAgent实例
     */
    private static ToolCallAgent createDelegate(ChatModel chatModel, ToolCollection tools, String name) {
        return ToolCallAgent.builder()
                .name(name)
                .description("专注于数据分析和信息处理的智能体，擅长识别模式和提取见解。")
                .chatModel(chatModel)
                .availableTools(tools)
                .conversationId(UUID.randomUUID().toString())
                .build();
    }
}
