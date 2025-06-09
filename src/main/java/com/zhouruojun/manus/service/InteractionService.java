package com.zhouruojun.manus.service;

import com.zhouruojun.manus.core.WorkflowEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Scanner;
import java.util.UUID;

/**
 * 用户交互服务
 * 处理用户输入和工作流交互逻辑
 */
@Service
@Slf4j
public class InteractionService {

    /**
     * 启动交互式会话
     */
    public void startInteractiveSession(WorkflowEngine workflowEngine) {
        log.info("🎯 多智能体系统已准备就绪，可以开始交互");
        log.info("📝 输入 'exit' 退出程序");
        log.info("📝 输入 'help' 查看帮助信息");
        
        Scanner scanner = new Scanner(System.in);
        String sessionId = UUID.randomUUID().toString();
        
        try {
            // 显示欢迎信息
            showWelcomeMessage();
            
            // 交互循环
            while (true) {
                System.out.print("\n💬 请输入您的问题或任务: ");
                String userInput = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(userInput)) {
                    log.info("🔚 正在退出多智能体系统...");
                    workflowEngine.cleanupSession(sessionId);
                    break;
                }
                
                if ("help".equalsIgnoreCase(userInput)) {
                    showHelpMessage();
                    continue;
                }
                
                if (userInput.isEmpty()) {
                    System.out.println("❌ 请输入有效内容");
                    continue;
                }

                processUserInput(workflowEngine, userInput, sessionId);
            }
        } finally {
            scanner.close();
            log.info("👋 多智能体系统已关闭");
        }
    }

    /**
     * 处理用户输入
     */
    private void processUserInput(WorkflowEngine workflowEngine, String userInput, String sessionId) {
        log.info("🔄 正在处理您的请求: {}", userInput);
        
        try {
            System.out.println("\n⏳ 正在分析和处理您的请求...");
            
            // 执行工作流
            String result = workflowEngine.executeWorkflow(userInput, sessionId).get();
            
            // 显示结果
            System.out.println("\n🤖 多智能体系统响应：");
            System.out.println("═".repeat(60));
            System.out.println(result);
            System.out.println("═".repeat(60));
            
        } catch (Exception e) {
            log.error("❌ 处理请求时出错: {}", e.getMessage(), e);
            System.out.println("\n❌ 发生错误: " + e.getMessage());
        }
    }

    /**
     * 显示欢迎信息
     */
    private void showWelcomeMessage() {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("🎉 欢迎使用 Manus 多智能体系统");
        System.out.println("📊 采用LangGraph4j状态图架构");
        System.out.println("🤖 内置智能体: 协调器 → 搜索 → 分析 → 总结");
        System.out.println("💡 支持复杂任务自动分解和智能体协作");
        System.out.println("═".repeat(80));
    }

    /**
     * 显示帮助信息
     */
    private void showHelpMessage() {
        System.out.println("\n📚 帮助信息:");
        System.out.println("─".repeat(50));
        System.out.println("🔍 搜索示例: \"请搜索人工智能的最新发展\"");
        System.out.println("📊 分析示例: \"分析一下当前科技行业的趋势\"");
        System.out.println("📄 总结示例: \"总结一下机器学习的主要应用\"");
        System.out.println("🔬 研究示例: \"研究一下量子计算的现状和前景\"");
        System.out.println("─".repeat(50));
        System.out.println("⚙️  系统会自动选择合适的智能体来处理您的请求");
        System.out.println("🔄 支持多轮对话，可以在结果基础上继续提问");
        System.out.println("📝 输入 'exit' 退出系统");
        System.out.println("─".repeat(50));
    }
} 