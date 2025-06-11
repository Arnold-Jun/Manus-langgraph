package com.zhouruojun.manus.infrastructure.tools.imp.summary;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 文本总结工具
 * 专门为总结智能体提供的工具
 */
@Component
public class TextSummaryTool {

    /**
     * 生成文本摘要
     *
     * @param text 需要总结的文本内容
     * @param summaryLength 摘要长度（短、中、长）
     * @return 文本摘要
     */
    @Tool(description = "对长文本进行智能摘要，提取关键信息。这是总结智能体专用的工具。")
    public static String summarizeText(
            @ToolParam(description = "需要总结的文本内容") String text,
            @ToolParam(description = "摘要长度: 短、中、长") String summaryLength) {
        
        // 根据长度生成不同详细程度的摘要
        String summary;
        switch (summaryLength.toLowerCase()) {
            case "短":
                summary = "简短摘要: 这是一个关于核心主题的概述。";
                break;
            case "中":
                summary = "中等摘要: 文本涵盖了主要观点，包括关键论据和结论。主要发现包括几个重要方面的分析。";
                break;
            case "长":
                summary = "详细摘要: 文本深入讨论了多个维度的内容。首先，介绍了背景信息和问题陈述。其次，详细分析了相关因素和影响。最后，提出了具体的结论和建议。整体而言，这是一个全面而深入的分析。";
                break;
            default:
                summary = "标准摘要: 文本包含了重要信息，经过分析后提取了关键要点。";
        }
        
        return String.format("文本摘要 (原文长度: %d 字符, 摘要长度: %s):\n\n%s\n\n关键词: 模拟关键词1, 模拟关键词2, 模拟关键词3", 
                text.length(), summaryLength, summary);
    }

    /**
     * 提取关键要点
     *
     * @param content 内容文本
     * @param pointsCount 要提取的要点数量
     * @return 关键要点列表
     */
    @Tool(description = "从文本中提取关键要点和重点信息。这是总结智能体专用的工具。")
    public static String extractKeyPoints(
            @ToolParam(description = "需要提取要点的内容") String content,
            @ToolParam(description = "要提取的要点数量") int pointsCount) {
        
        StringBuilder points = new StringBuilder("关键要点提取结果:\n");
        
        for (int i = 1; i <= Math.min(pointsCount, 5); i++) {
            points.append(String.format("%d. 关键要点 %d: 这是从原文中提取的重要信息点\n", i, i));
        }
        
        points.append(String.format("\n总计提取了 %d 个关键要点 (原文长度: %d 字符)", 
                Math.min(pointsCount, 5), content.length()));
        
        return points.toString();
    }
} 