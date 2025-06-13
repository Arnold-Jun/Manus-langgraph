package com.zhouruojun.manus.domain.workflow.node.specialized;

import com.zhouruojun.manus.domain.workflow.node.base.BaseNode;
import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;

/**
 * 总结节点 - 负责信息整合和报告生成
 */
@Slf4j
public class SummaryNode extends BaseNode {

    private final String systemPrompt;

    public SummaryNode(ChatModel chatModel, PromptLoader promptLoader, String promptPath) {
        super(chatModel);
        this.systemPrompt = promptLoader.loadPrompt(promptPath);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");

        // 构建回复生成上下文
        StringBuilder replyContext = new StringBuilder();
        replyContext.append("用户问题: ").append(userInput).append("\n");

        // 如果有历史记录，在这里可以提供历史上下文信息
        if (state.hasHistory()) {
            replyContext.append("\n这是一个基于历史对话的问题。\n");
            replyContext.append("以下是对话历史记录：\n");

            // 获取历史记录
            List<ChatMessage> history = state.sessionHistory();
            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                String roleStr = msg.type().toString();
                String prefix = "USER".equals(roleStr) ? "用户" : "系统";
                replyContext.append(String.format("%d. %s: %s\n", i+1, prefix, msg.toString()));
            }
            replyContext.append("请注意用户可能在询问之前的对话内容、问题或相关信息。\n");
        }

        // 检查是否有工具执行结果
        List<String> toolResults = state.toolResults();
        if (!toolResults.isEmpty()) {
            replyContext.append("可用信息: \n")
                       .append(String.join("\n", toolResults))
                       .append("\n");
            replyContext.append("请基于以上信息为用户生成一个完整、有用的回复。");
        } else {
            if (state.hasHistory()) {
                replyContext.append("请基于对话历史上下文为用户生成一个有帮助的回复。");
                replyContext.append("如果用户询问之前的问题，请尽量根据可能的历史信息进行回答。");
            } else {
                replyContext.append("请直接针对用户的问题生成一个有帮助的回复。");
            }
        }

        // 调用语言模型生成最终回复
        String finalReply = callChatModel(systemPrompt, replyContext.toString());

        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage(finalReply);

        // 返回最终状态 - 设置finished=true来结束工作流
        return Map.of(
                "currentAgent", "summary",
                "result", finalReply,  // 设置最终结果
                "finished", true,      // 标记工作流完成
                "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "SummaryNode";
    }
}