package com.zhouruojun.manus.domain.workflow.node.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.zhouruojun.manus.domain.model.NextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;

/**
 * LangGraph4j节点基础类
 * 实现NodeAction接口以支持LangGraph4j状态图
 */
public abstract class BaseNode implements NodeAction<AgentMessageState> {

    private static final Logger log = LoggerFactory.getLogger(BaseNode.class);

    protected final ChatModel chatModel;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    public BaseNode(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Map<String, Object> apply(AgentMessageState state) throws Exception {
        try {
            log.info("Processing node: {}", getNodeName());
            return processNode(state);
        } catch (Exception e) {
            log.error("Error in node {}: {}", getNodeName(), e.getMessage(), e);
            return Map.of(
                "error", "节点执行错误: " + e.getMessage(),
                "messages", state.lastMessage().orElse(AgentMessageState.createAiMessage("节点执行错误"))
            );
        }
    }

    /**
     * 具体的节点处理逻辑
     * @param state 当前状态
     * @return 更新后的状态数据
     */
    protected abstract Map<String, Object> processNode(AgentMessageState state) throws Exception;

    /**
     * 获取节点名称
     */
    protected abstract String getNodeName();

    /**
     * 调用ChatModel获取响应
     */
    protected String callChatModel(String systemPrompt, String userInput) {
        try {
            List<org.springframework.ai.chat.messages.Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userInput)
            );
            
            Prompt prompt = new Prompt(messages);
            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error calling chat model", e);
            return "调用语言模型时发生错误: " + e.getMessage();
        }
    }

    /**
     * 解析下一步动作
     */
    protected String parseNextAction(String response) {
        try {
            // 1. 从整个响应里提取第一段 {...} JSON
            Matcher m = JSON_PATTERN.matcher(response.trim());
            if (m.find()) {
                String json = m.group();
                JsonNode root = MAPPER.readTree(json);

                // 2. 读取 action 字段
                if (root.has("action")) {
                    String act = root.get("action").asText();
                    return NextAction.fromString(act).getValue();
                }
            }
        } catch (Exception e) {
            log.warn("JSON 解析失败，转入备用逻辑", e);
        }

        // 3. 备用：如果真的不是标准 JSON，那再按最简激进策略——只看开头 prefix
        String lower = response.stripLeading().toLowerCase();
        if (lower.startsWith("search:") || lower.startsWith("搜索："))       return NextAction.SEARCH.getValue();
        if (lower.startsWith("analysis:") || lower.startsWith("分析："))   return NextAction.ANALYSIS.getValue();
        if (lower.startsWith("summary:") || lower.startsWith("总结："))    return NextAction.SUMMARY.getValue();
        if (lower.startsWith("human_input:") || lower.startsWith("用户输入：")) return NextAction.HUMAN_INPUT.getValue();

        // 4. 最后兜底 - 默认到summary进行最终处理
        return NextAction.SUMMARY.getValue();
    }
}
