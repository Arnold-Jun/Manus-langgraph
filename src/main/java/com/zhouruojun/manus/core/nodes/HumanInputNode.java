package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 人工输入节点 - 处理需要用户进一步输入的情况
 */
public class HumanInputNode implements NodeAction<AgentMessageState> {

    private static final Logger log = LoggerFactory.getLogger(HumanInputNode.class);

    @Override
    public Map<String, Object> apply(AgentMessageState state) throws Exception {
        try {
            log.info("Processing human input node");
            
            // 提示用户需要进一步输入
            String promptMessage = "需要您的进一步输入。请提供更多信息或澄清您的需求。";
            var aiMessage = AgentMessageState.createAiMessage(promptMessage);
            
            // 更新状态并返回
            return Map.of(
                "currentAgent", "human_input",
                "next", "coordinator",
                "messages", aiMessage
            );
            
        } catch (Exception e) {
            log.error("Error in human input node: {}", e.getMessage(), e);
            return Map.of(
                "error", "人工输入节点执行错误: " + e.getMessage(),
                "messages", AgentMessageState.createAiMessage("人工输入节点执行错误")
            );
        }
    }
} 