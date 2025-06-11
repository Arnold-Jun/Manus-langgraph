package com.zhouruojun.manus.domain.workflow.node.specialized;

import com.zhouruojun.manus.domain.agent.base.AgentNodeAdapter;
import com.zhouruojun.manus.domain.agent.specialized.SummaryAgent;
import com.zhouruojun.manus.infrastructure.tools.collection.ToolCollection;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 总结智能体节点
 * 将SummaryAgent适配为LangGraph4j的Node，支持工具调用
 */
@Slf4j
public class SummaryAgentNode extends AgentNodeAdapter {

    /**
     * 创建总结智能体节点
     * @param chatModel Spring AI的ChatModel
     * @param toolCollection 工具集合，包含总结相关工具
     * @param agentName 智能体名称
     * @param promptLoader 提示词加载器
     * @param promptPath 提示词文件路径
     */
    public SummaryAgentNode(ChatModel chatModel, ToolCollection toolCollection, String agentName, PromptLoader promptLoader, String promptPath) {
        super(new SummaryAgent(chatModel, toolCollection, agentName, promptLoader, promptPath));
        log.info("总结智能体节点初始化完成，智能体名称: {}", agentName);
    }

    /**
     * 使用默认名称创建总结智能体节点
     * @param chatModel Spring AI的ChatModel
     * @param toolCollection 工具集合
     * @param promptLoader 提示词加载器
     * @param promptPath 提示词文件路径
     */
    public SummaryAgentNode(ChatModel chatModel, ToolCollection toolCollection, PromptLoader promptLoader, String promptPath) {
        this(chatModel, toolCollection, "summary_agent", promptLoader, promptPath);
    }

    @Override
    public String getNodeName() {
        return "SummaryAgentNode";
    }

    @Override
    public String getAgentName() {
        return "summary";
    }
} 