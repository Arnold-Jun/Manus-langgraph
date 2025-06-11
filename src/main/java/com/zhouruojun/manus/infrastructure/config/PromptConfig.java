package com.zhouruojun.manus.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 提示词配置类
 * 集中管理所有提示词文件的路径
 */
@Configuration
@ConfigurationProperties(prefix = "manus.prompt")
public class PromptConfig {
    
    /**
     * Node提示词路径配置
     */
    private NodePrompts node = new NodePrompts();
    
    /**
     * Agent提示词路径配置
     */
    private AgentPrompts agent = new AgentPrompts();
    
    public static class NodePrompts {
        private String coordinator = "node/coordinator.txt";
        private String summary = "node/summary.txt";
        
        public String getCoordinator() { return coordinator; }
        public void setCoordinator(String coordinator) { this.coordinator = coordinator; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
    
    public static class AgentPrompts {
        private String search = "agent/search.txt";
        private String analysis = "agent/analysis.txt";
        private String summary = "agent/summary.txt";
        
        public String getSearch() { return search; }
        public void setSearch(String search) { this.search = search; }
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
    
    public NodePrompts getNode() { return node; }
    public void setNode(NodePrompts node) { this.node = node; }
    public AgentPrompts getAgent() { return agent; }
    public void setAgent(AgentPrompts agent) { this.agent = agent; }
} 