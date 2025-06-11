package com.zhouruojun.manus.infrastructure.tools.imp.common;

import com.zhouruojun.manus.domain.agent.base.Agent;
import com.zhouruojun.manus.domain.agent.base.AgentRegistry;
import com.zhouruojun.manus.domain.agent.base.AgentState;
import com.zhouruojun.manus.domain.agent.base.BaseAgent;
import com.zhouruojun.manus.domain.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 终止工具调用代理的工具
 * 根据接收的状态，在当前的工具调用代理的内存中添加不同的assistant消息
 * 并将代理的状态设置为FINISHED
 */
@Component
@Slf4j
public class TerminateTool {

    /**
     * 终止当前交互
     *
     * @param status 结束状态（success, failure, 或其他）
     * @param agentId 要终止的智能体ID（可选）
     * @return 操作结果信息
     */
    @Tool(description = "【仅在完成所有工作后调用】终止当前工作流程。\n" +
                        "重要限制：\n" +
                        "- 搜索智能体：必须先调用 webSearch 工具获取结果后才能调用此工具\n" +
                        "- 分析智能体：必须先完成数据分析后才能调用此工具\n" +
                        "- 禁止在未执行专门工具的情况下直接调用此工具\n" +
                        "- 此工具仅用于结束已完成的任务，不能用于跳过工作步骤")
    public static String terminate(
            @ToolParam(description = "The finish status of the interaction. It can be success or failure") String status,
            @ToolParam(description = "The ID of the agent to terminate (optional). If not provided, the current agent will be used") String agentId) {
        String assistantMessage;

        if ("success".equalsIgnoreCase(status)) {
            assistantMessage = "任务已成功完成。感谢您的使用！";
        } else if ("failure".equalsIgnoreCase(status)) {
            assistantMessage = "很抱歉，我无法继续完成这项任务。如果您有其他需要帮助的地方，请重新提问。";
        } else {
            assistantMessage = "交互已结束。如果您有其他问题，请随时提问。";
        }

        // 首先尝试使用提供的agentId获取智能体
        Agent agent = null;
        if (agentId != null && !agentId.isEmpty()) {
            agent = AgentRegistry.getAgent(agentId);
            if (agent != null) {
                log.info("TerminateTool: 使用提供的agentId={}找到智能体", agentId);
            }
        }

        // 如果没有提供agentId或未找到对应智能体，尝试从全局注册中心获取当前运行的智能体
        if (agent == null) {
            agent = AgentRegistry.getCurrentAgent();
            log.info("TerminateTool: 使用AgentRegistry.getCurrentAgent()获取智能体");
        }

        if (agent == null) {
            log.warn("TerminateTool: 无法终止交互: 未找到当前运行的Agent实例");
            return "无法终止交互: 未找到当前运行的Agent实例";
        }

        log.info("TerminateTool: 尝试终止交互, 状态: {}, 当前代理类型: {}, 代理ID: {}",
                status, agent.getClass().getSimpleName(), agentId != null ? agentId : "未指定");

        try {
            // 处理添加消息和设置状态的逻辑
            boolean success = setAgentFinished(agent, assistantMessage);

            if (success) {
                // 强制等待确保状态更新
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return "已终止当前交互: " + status;
            } else {
                return "部分完成终止交互操作: " + status + " (可能无法完全终止)";
            }
        } catch (Exception e) {
            log.error("TerminateTool: 终止交互时发生错误", e);
            return "终止交互时发生错误: " + e.getMessage();
        }
    }

    /**
     * 将智能体状态设置为FINISHED并添加终止消息
     * 这个方法将所有状态设置和消息添加逻辑合并在一起
     *
     * @param agent 智能体实例
     * @param message 终止消息
     * @return 是否成功设置
     */
    private static boolean setAgentFinished(Agent agent, String message) {
        // 使用instanceOf检测BaseAgent类型，这是最常见的实现
        if (agent instanceof BaseAgent) {
            BaseAgent baseAgent = (BaseAgent) agent;
            try {
                // 添加消息
                baseAgent.getMemory().addMessage(baseAgent.getConversationId(),
                        Message.assistantMessage(message, null).getSpringMessage());

                // 设置状态
                baseAgent.setState(AgentState.FINISHED);

                log.info("TerminateTool: 成功终止BaseAgent");
                return true;
            } catch (Exception e) {
                log.error("TerminateTool: 终止BaseAgent时出错", e);
                return false;
            }
        } else {
            // 对于其他类型的Agent，我们无法直接操作，但可以尝试中断当前线程
            log.warn("TerminateTool: 非BaseAgent类型，尝试中断当前线程");
            Thread.currentThread().interrupt();
            return false;
        }
    }
} 