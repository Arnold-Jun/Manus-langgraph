package com.zhouruojun.manus.domain.agent.base;

import com.zhouruojun.manus.domain.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * Agent到NodeAction的适配器基类
 * 允许现有的Agent实现用作LangGraph4j的Node
 */
@Slf4j
public abstract class AgentNodeAdapter implements NodeAction<AgentMessageState> {

    protected final Agent agent;

    /**
     * 构造适配器
     * @param agent 要适配的Agent实例
     */
    protected AgentNodeAdapter(Agent agent) {
        this.agent = agent;
        // 确保Agent已初始化
        if (agent != null) {
            agent.initialize();
        }
    }

    @Override
    public Map<String, Object> apply(AgentMessageState state) throws Exception {
        try {
            log.info("Processing agent node: {}", getNodeName());
            return processAgentNode(state);
        } catch (Exception e) {
            log.error("Error in agent node {}: {}", getNodeName(), e.getMessage(), e);
            return Map.of(
                "error", "智能体节点执行错误: " + e.getMessage(),
                "messages", AgentMessageState.createAiMessage("智能体节点执行错误: " + e.getMessage()),
                "finished", true
            );
        }
    }

    /**
     * 处理智能体节点的核心逻辑
     * @param state 当前状态
     * @return 更新后的状态数据
     */
    protected Map<String, Object> processAgentNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 如果有历史记录，处理历史上下文
        if (state.hasHistory() && !state.getSessionHistory().isEmpty()) {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("【以下是之前的对话历史】\n");
            
            for (var historyMsg : state.getSessionHistory()) {
                String rolePrefix = switch (historyMsg.getRole()) {
                    case USER -> "用户: ";
                    case ASSISTANT -> "助手: ";
                    case SYSTEM -> "系统: ";
                    case TOOL -> "工具: ";
                };

                //Todo：优化，使用更智能的摘要算法
                String content = historyMsg.getContent();
                if (content != null && content.length() > 100) {
                    content = content.substring(0, 97) + "...";
                }
                contextBuilder.append(rolePrefix).append(content).append("\n");
            }
            contextBuilder.append("【历史记录结束】\n\n");
            contextBuilder.append("【当前问题】\n").append(userInput);
            
            userInput = contextBuilder.toString();
        }
        
        // 运行智能体
        String result = agent.run(userInput).get();
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage(result);
        
        // 获取智能体名称，用于状态跟踪
        String agentName = getAgentName();
        
        // 构建工具结果字符串
        String toolResults = agentName + ": " + result;
        
        // 返回状态更新 - 通常完成后回到coordinator决定下一步
        return Map.of(
            "currentAgent", agentName,
            "toolResults", toolResults,
            "result", result,
            "next", "coordinator",  // 完成后返回coordinator
            "messages", aiMessage
        );
    }

    /**
     * 获取节点名称
     */
    public abstract String getNodeName();

    /**
     * 获取智能体名称
     */
    public abstract String getAgentName();

    /**
     * 获取适配的Agent实例
     */
    public Agent getAgent() {
        return agent;
    }
} 