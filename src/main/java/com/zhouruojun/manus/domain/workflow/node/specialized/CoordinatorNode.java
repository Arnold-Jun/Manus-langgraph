package com.zhouruojun.manus.domain.workflow.node.specialized;

import com.zhouruojun.manus.domain.workflow.node.base.BaseNode;
import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 协调器节点 - 负责分析任务并决定下一步行动
 * 基于LangGraph4j的NodeAction实现
 */
@Slf4j
public class CoordinatorNode extends BaseNode {

    private final String systemPrompt;

    public CoordinatorNode(ChatModel chatModel, PromptLoader promptLoader, String promptPath) {
        super(chatModel);
        this.systemPrompt = promptLoader.loadPrompt(promptPath);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建上下文信息
        StringBuilder contextBuilder = new StringBuilder();
        
        // 添加用户请求信息
        contextBuilder.append("用户请求: ").append(userInput).append("\n");
        
        // 如果有历史记录，添加提示信息
        if (state.hasHistory()) {
            contextBuilder.append("注意: 此问题基于之前的对话历史，请结合历史上下文进行分析。\n");
        }
        
        // 如果有之前的工具结果，包含进来
        String toolResults = state.toolResults().orElse("");
        if (!toolResults.isEmpty()) {
            contextBuilder.append("之前的执行结果: \n").append(toolResults).append("\n");
        }
        
        // 如果有执行结果，检查是否需要进一步处理
        String currentResult = state.result().orElse("");
        if (!currentResult.isEmpty()) {
            contextBuilder.append("当前结果: ").append(currentResult).append("\n");
            contextBuilder.append("请判断任务是否已经完成，还是需要进一步处理。");
        }
        
        String context = contextBuilder.toString();
        
        // 调用语言模型进行决策
        String response = callChatModel(systemPrompt, context);
        // 解析下一步动作
        String nextAction = parseNextAction(response);
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage("协调器: " + response);
        
        // 返回状态更新
        return Map.of(
            "currentAgent", "coordinator",
            "next", nextAction,
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "CoordinatorNode";
    }
} 