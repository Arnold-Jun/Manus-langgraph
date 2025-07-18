package com.zhouruojun.manus;

import com.zhouruojun.manus.domain.workflow.engine.WorkflowEngine;
import com.zhouruojun.manus.application.service.InteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Manus多智能体系统主应用
 * 基于LangGraph4j架构的统一智能体协作平台
 */
@SpringBootApplication
public class ManusApplication {

    private static final Logger log = LoggerFactory.getLogger(ManusApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManusApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // 设置为非Web应用
        app.run(args);
    }

    @Bean
    public CommandLineRunner runManusSystem(
            WorkflowEngine workflowEngine,
            InteractionService interactionService) {
        return args -> {
            log.info("🚀 Manus多智能体系统启动中...");
            log.info("📊 基于LangGraph4j架构");
            log.info("🤖 支持智能体: 协调器、搜索、分析、总结");
            log.info("💬 准备接收用户输入...");
            
            // 启动交互式会话
            interactionService.startInteractiveSession(workflowEngine);
            
            log.info("👋 Manus多智能体系统已关闭");
        };
    }
} 