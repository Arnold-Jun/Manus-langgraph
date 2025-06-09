package com.zhouruojun.manus.core.nodes;

import com.zhouruojun.manus.model.AgentMessageState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

/**
 * 分析节点 - 负责数据分析和信息处理
 */
@Slf4j
public class AnalysisNode extends BaseNode {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的分析智能体，擅长数据分析和信息处理。
        
        你的主要职责是：
        1. 分析收集到的信息和数据
        2. 识别关键模式、趋势和洞察
        3. 提供深入的分析结论
        4. 为后续决策提供支持
        
        请对提供的信息进行深入分析，提取关键洞察和结论。
        """;

    public AnalysisNode(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    protected Map<String, Object> processNode(AgentMessageState state) throws Exception {
        String userInput = state.userInput().orElse("");
        
        // 构建分析上下文
        StringBuilder analysisContext = new StringBuilder();
        analysisContext.append("分析任务: ").append(userInput).append("\n");
        
        // 包含之前的搜索结果等信息
        String existingToolResults = state.toolResults().orElse("");
        if (!existingToolResults.isEmpty()) {
            analysisContext.append("可用数据: \n").append(existingToolResults).append("\n");
        }
        
        analysisContext.append("请对以上信息进行深入分析。");
        
        // 调用语言模型执行分析
        String analysisResult = callChatModel(SYSTEM_PROMPT, analysisContext.toString());
        
        // 创建AI响应消息
        var aiMessage = AgentMessageState.createAiMessage("分析结果: " + analysisResult);
        
        // 构建工具结果字符串，追加到现有结果
        String updatedToolResults = existingToolResults.isEmpty() ? 
            "analysis: " + analysisResult : 
            existingToolResults + "\nanalysis: " + analysisResult;
        
        // 更新状态并返回
        return Map.of(
            "currentAgent", "analysis",
            "toolResults", updatedToolResults,
            "next", "coordinator",
            "messages", aiMessage
        );
    }

    @Override
    protected String getNodeName() {
        return "AnalysisNode";
    }
} 