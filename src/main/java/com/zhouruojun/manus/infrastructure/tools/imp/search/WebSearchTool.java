package com.zhouruojun.manus.infrastructure.tools.imp.search;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 网络搜索工具
 * 专门为搜索智能体提供的工具
 */
@Component
public class WebSearchTool {

    /**
     * 执行网络搜索
     *
     * @param query 搜索查询关键字
     * @param maxResults 最大返回结果数量
     * @return 搜索结果
     */
    @Tool(description = "执行网络搜索以获取最新、准确的信息。" +
                        "当用户要求搜索、查找、了解任何信息时，这是你的首要工具。" +
                        "适用于：新闻搜索、资料查找、实时信息获取等所有需要网络信息的场景。" )
    public static String webSearch(
            @ToolParam(description = "搜索的关键字或问题，如'热点'、'最新新闻'等") String query,
            @ToolParam(description = "最大返回结果数量，默认为5") int maxResults) {
        
        // 模拟搜索结果
        return String.format("网络搜索结果 (搜索词: %s, 最多 %d 条结果):\n" +
                "1. 搜索结果示例1 - 这是一个模拟的搜索结果\n" +
                "2. 搜索结果示例2 - 另一个相关的信息\n" +
                "3. 搜索结果示例3 - 更多相关内容", 
                query, maxResults);
    }
} 