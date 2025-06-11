package com.zhouruojun.manus.domain.workflow.node.specialized;

import com.zhouruojun.manus.domain.agent.base.AgentNodeAdapter;
import com.zhouruojun.manus.domain.agent.specialized.AnalysisAgent;
import com.zhouruojun.manus.infrastructure.tools.collection.ToolCollection;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 分析智能体节点
 * 将AnalysisAgent适配为LangGraph4j的Node，支持工具调用
 */
@Slf4j
public class AnalysisAgentNode extends AgentNodeAdapter {

    /**
     * 创建分析智能体节点
     * @param chatModel Spring AI的ChatModel
     * @param toolCollection 工具集合，包含分析相关工具
     * @param agentName 智能体名称
     * @param promptLoader 提示词加载器
     * @param promptPath 提示词文件路径
     */
    public AnalysisAgentNode(ChatModel chatModel, ToolCollection toolCollection, String agentName, PromptLoader promptLoader, String promptPath) {
        super(new AnalysisAgent(chatModel, toolCollection, agentName, promptLoader, promptPath));
        log.info("分析智能体节点初始化完成，智能体名称: {}", agentName);
    }

    /**
     * 使用默认名称创建分析智能体节点
     * @param chatModel Spring AI的ChatModel
     * @param toolCollection 工具集合
     * @param promptLoader 提示词加载器
     * @param promptPath 提示词文件路径
     */
    public AnalysisAgentNode(ChatModel chatModel, ToolCollection toolCollection, PromptLoader promptLoader, String promptPath) {
        this(chatModel, toolCollection, "analysis_agent", promptLoader, promptPath);
    }

    @Override
    public String getNodeName() {
        return "AnalysisAgentNode";
    }

    @Override
    public String getAgentName() {
        return "analysis";
    }
} 