package com.zhouruojun.manus.domain.workflow.node;

import com.zhouruojun.manus.domain.workflow.node.specialized.AnalysisAgentNode;
import com.zhouruojun.manus.domain.workflow.node.specialized.SearchAgentNode;
import com.zhouruojun.manus.domain.workflow.node.specialized.SummaryAgentNode;
import com.zhouruojun.manus.infrastructure.tools.collection.ToolCollection;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import com.zhouruojun.manus.infrastructure.config.PromptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体节点工厂类
 * 用于创建和管理各种智能体节点实例
 */
@Slf4j
@Component
public class AgentNodeFactory {

    private final ChatModel chatModel;
    private final Map<String, ToolCollection> toolCollections;
    private final PromptLoader promptLoader;
    private final PromptConfig promptConfig;

    /**
     * 构造工厂
     * @param chatModel Spring AI的ChatModel
     * @param promptLoader 提示词加载器
     * @param promptConfig 提示词配置
     */
    public AgentNodeFactory(ChatModel chatModel, PromptLoader promptLoader, PromptConfig promptConfig) {
        this.chatModel = chatModel;
        this.promptLoader = promptLoader;
        this.promptConfig = promptConfig;
        this.toolCollections = new HashMap<>();
        log.info("智能体节点工厂初始化完成（工具集合将按需创建）");
    }

    /**
     * 初始化默认的工具集合 - 现在已移除，改为延迟创建
     */
    @Deprecated
    private void initializeDefaultToolCollections() {
        log.info("工具集合现在改为按需创建");
    }

    /**
     * 创建搜索智能体节点
     * @param agentName 智能体名称
     * @return SearchAgentNode实例
     */
    public SearchAgentNode createSearchAgentNode(String agentName) {
        ToolCollection searchTools = getOrCreateToolCollection("search");
        return new SearchAgentNode(chatModel, searchTools, agentName, promptLoader, promptConfig.getAgent().getSearch());
    }

    /**
     * 创建搜索智能体节点（使用默认名称）
     * @return SearchAgentNode实例
     */
    public SearchAgentNode createSearchAgentNode() {
        return createSearchAgentNode("search_agent");
    }

    /**
     * 创建分析智能体节点
     * @param agentName 智能体名称
     * @return AnalysisAgentNode实例
     */
    public AnalysisAgentNode createAnalysisAgentNode(String agentName) {
        ToolCollection analysisTools = getOrCreateToolCollection("analysis");
        return new AnalysisAgentNode(chatModel, analysisTools, agentName, promptLoader, promptConfig.getAgent().getAnalysis());
    }

    /**
     * 创建分析智能体节点（使用默认名称）
     * @return AnalysisAgentNode实例
     */
    public AnalysisAgentNode createAnalysisAgentNode() {
        return createAnalysisAgentNode("analysis_agent");
    }

    /**
     * 创建总结智能体节点
     * @param agentName 智能体名称
     * @return SummaryAgentNode实例
     */
    public SummaryAgentNode createSummaryAgentNode(String agentName) {
        ToolCollection summaryTools = getOrCreateToolCollection("summary");
        return new SummaryAgentNode(chatModel, summaryTools, agentName, promptLoader, promptConfig.getAgent().getSummary());
    }

    /**
     * 创建总结智能体节点（使用默认名称）
     * @return SummaryAgentNode实例
     */
    public SummaryAgentNode createSummaryAgentNode() {
        return createSummaryAgentNode("summary_agent");
    }

    /**
     * 添加或更新工具集合
     * @param type 工具类型（search、analysis、summary）
     * @param toolCollection 工具集合
     */
    public void addToolCollection(String type, ToolCollection toolCollection) {
        toolCollections.put(type, toolCollection);
        log.info("已添加工具集合，类型: {}", type);
    }

    /**
     * 获取或创建工具集合
     * @param type 工具类型
     * @return ToolCollection实例
     */
    private ToolCollection getOrCreateToolCollection(String type) {
        return toolCollections.computeIfAbsent(type, k -> {
            log.info("正在为类型 {} 创建新的工具集合", type);
            return new ToolCollection(false, true, type);
        });
    }

    /**
     * 获取工具集合
     * @param type 工具类型
     * @return ToolCollection实例，如果不存在则返回null
     */
    public ToolCollection getToolCollection(String type) {
        return toolCollections.get(type);
    }

    /**
     * 创建自定义智能体节点
     * @param type 智能体类型
     * @param agentName 智能体名称
     * @param customTools 自定义工具集合
     * @return 对应的智能体节点
     */
    public Object createCustomAgentNode(String type, String agentName, ToolCollection customTools) {
        switch (type.toLowerCase()) {
            case "search":
                return new SearchAgentNode(chatModel, customTools, agentName, promptLoader, promptConfig.getAgent().getSearch());
            case "analysis":
                return new AnalysisAgentNode(chatModel, customTools, agentName, promptLoader, promptConfig.getAgent().getAnalysis());
            case "summary":
                return new SummaryAgentNode(chatModel, customTools, agentName, promptLoader, promptConfig.getAgent().getSummary());
            default:
                throw new IllegalArgumentException("Unsupported agent type: " + type);
        }
    }
}
