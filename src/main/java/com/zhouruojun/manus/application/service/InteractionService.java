package com.zhouruojun.manus.application.service;

import com.zhouruojun.manus.domain.workflow.engine.WorkflowEngine;
import com.zhouruojun.manus.domain.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * 用户交互服务
 * 处理用户输入和工作流交互逻辑
 */
@Service
public class InteractionService {

    private static final Logger log = LoggerFactory.getLogger(InteractionService.class);

    @Autowired
    private SessionManager sessionManager;

    /**
     * 启动交互式会话
     */
    public void startInteractiveSession(WorkflowEngine workflowEngine) {
        log.info("🎯 多智能体系统已准备就绪，可以开始交互");
        log.info("📝 输入 'exit' 退出程序");
        log.info("📝 输入 'help' 查看帮助信息");
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            // 显示欢迎信息
            showWelcomeMessage();
            
            // 为整个交互会话创建一个持久的sessionId
            String sessionId = UUID.randomUUID().toString();
            log.info("创建新的会话：{}", sessionId);

            // 交互循环
            while (true) {
                System.out.print("\n💬 请输入您的问题或任务: ");
                String userInput = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(userInput)) {
                    log.info("🔚 正在退出多智能体系统...");
                    break;
                }
                
                if ("help".equalsIgnoreCase(userInput)) {
                    showHelpMessage();
                    continue;
                }
                
                if ("clear".equalsIgnoreCase(userInput)) {
                    sessionManager.clearSessionHistory(sessionId);
                    System.out.println("✅ 已清除当前会话的历史记录");
                    continue;
                }

                if (userInput.isEmpty()) {
                    System.out.println("❌ 请输入有效内容");
                    continue;
                }

                // 添加用户消息到会话历史
                sessionManager.addUserMessage(sessionId, userInput);

                // 处理用户输入，使用相同的sessionId保持对话连贯性
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
        log.debug("当前会话状态: {}", sessionManager.getSessionSummary(sessionId));

        try {
            System.out.println("\n⏳ 正在分析和处理您的请求...");
            
            // 获取当前会话的历史记录
            List<Message> sessionHistory = sessionManager.getSessionHistory(sessionId);

            // 添加用户输入到历史记录 - 注意要在执行工作流前添加
            sessionManager.addUserMessage(sessionId, userInput);

            // 执行工作流 - 传递会话历史
            String result = workflowEngine.executeWorkflow(userInput, sessionId, sessionHistory).get();

            // 添加系统响应到会话历史
            sessionManager.addSystemResponse(sessionId, result);

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
        System.out.println("🧠 已启用记忆功能，可记住对话历史");
        System.out.println("💬 输入'clear'可清除当前会话历史");
        System.out.println("═".repeat(80));
    }

    /**
     * 显示帮助信息
     */
    private void showHelpMessage() {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("📚 帮助信息：");
        System.out.println("• 输入任何问题或任务，系统会智能处理并回答");
        System.out.println("• 'exit' - 退出程序");
        System.out.println("• 'help' - 显示此帮助信息");
        System.out.println("• 'clear' - 清除当前会话的历史记录");
        System.out.println("─".repeat(60));
    }
}
