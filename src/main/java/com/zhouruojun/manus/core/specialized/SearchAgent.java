package com.zhouruojun.manus.core.specialized;

import com.zhouruojun.manus.core.base.ToolCallAgent;
import com.zhouruojun.manus.tools.collection.ToolCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.UUID;

/**
 * 搜索智能体
 * 专注于信息检索和搜索相关服务
 */
@Slf4j
public class SearchAgent extends AbstractSpecializedAgent {

    /**
     * 创建一个搜索智能体实例
     *
     * @param chatModel 聊天模型
     * @param searchTools 搜索相关工具集合
     * @param name 智能体名称
     */
    public SearchAgent(ChatModel chatModel, ToolCollection searchTools, String name) {
        super(createDelegate(chatModel, searchTools, name));

        // 设置搜索专家的系统提示词
        setSystemPrompt(
                "你是一个专业的搜索智能体，擅长信息检索和资料查找。\n" +
                "你的主要职责是：\n" +
                "1. 理解用户的搜索需求，提取关键信息点\n" +
                "2. 使用最合适的工具和搜索策略获取信息\n" +
                "3. 整理和组织搜索结果，确保信息的相关性和准确性\n" +
                "4. 按照清晰的结构返回搜索结果\n\n" +
                "工作流程：\n" +
                "1. 分析任务需求，确定需要获取的信息类型\n" +
                "2. 选择并使用适当的工具执行搜索\n" +
                "3. 整合和处理搜索结果\n" +
                "4. 形成完整、结构化的回复\n" +
                "5. 当你完成任务后，必须调用terminate工具结束当前工作流，返回结果给协调器\n\n" +
                "重要提示：\n" +
                "- 搜索完成后必须调用terminate(\"success\", \"" + name + "\")工具结束工作流\n" +
                "- 如果无法完成搜索，则调用terminate(\"failure\", \"" + name + "\")工具\n" +
                "- 确保你的最终回复包含完整的搜索结果，因为这将作为结果返回给协调器\n"
        );

        log.info("搜索智能体初始化完成，ID: {}", getConversationId());
    }

    /**
     * 创建委托的ToolCallAgent实例
     */
    private static ToolCallAgent createDelegate(ChatModel chatModel, ToolCollection tools, String name) {
        return ToolCallAgent.builder()
                .name(name)
                .description("专注于网上搜索信息的智能体，擅长信息检索和资料查找。")
                .chatModel(chatModel)
                .availableTools(tools)
                .conversationId(UUID.randomUUID().toString())
                .build();
    }
}
