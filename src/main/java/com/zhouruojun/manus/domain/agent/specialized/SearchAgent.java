package com.zhouruojun.manus.domain.agent.specialized;

import com.zhouruojun.manus.domain.agent.base.ToolCallAgent;
import com.zhouruojun.manus.infrastructure.tools.collection.ToolCollection;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 搜索智能体 - 专注于信息检索和查询
 */
@Slf4j
public class SearchAgent extends AbstractSpecializedAgent {

    /**
     * 创建一个搜索智能体实例
     *
     * @param chatModel 聊天模型
     * @param searchTools 搜索相关工具集合
     * @param name 智能体名称
     * @param promptLoader 提示词加载器
     * @param promptPath 提示词文件路径
     */
    public SearchAgent(ChatModel chatModel, ToolCollection searchTools, String name, PromptLoader promptLoader, String promptPath) {
        super(createDelegate(chatModel, searchTools, name));

        // 设置系统提示词
        setSystemPrompt(promptLoader.loadPromptWithReplacements(
            promptPath,
            Map.of("agent_id", name)
        ));

        log.info("搜索智能体初始化完成，ID: {}", getConversationId());
    }

    /**
     * 创建委托的ToolCallAgent实例
     */
    private static ToolCallAgent createDelegate(ChatModel chatModel, ToolCollection tools, String name) {
        return ToolCallAgent.builder()
                .name(name)
                .description("专注于信息检索和查询的智能体，擅长使用各种搜索工具获取信息。")
                .chatModel(chatModel)
                .availableTools(tools)
                .conversationId(name)
                .build();
    }
}