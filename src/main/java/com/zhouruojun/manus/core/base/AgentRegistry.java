package com.zhouruojun.manus.core.base;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能体注册中心
 * 提供一个全局的地方来注册和获取正在运行的智能体实例
 * 这解决了跨线程访问智能体实例的问题，并为多智能体系统提供基础
 */
@Slf4j
public class AgentRegistry {

    // 使用ConcurrentHashMap存储运行中的智能体，以唯一ID索引
    private static final Map<String, Agent> runningAgents = new ConcurrentHashMap<>();

    // 智能体类型映射，用于按类型查找智能体
    private static final Map<String, Set<String>> agentTypeMap = new ConcurrentHashMap<>();

    // 记录最后一个活跃的智能体
    private static volatile Agent lastActiveAgent = null;

    /**
     * 注册一个智能体实例
     * @param id 用于索引智能体的唯一标识
     * @param agent 要注册的智能体
     */
    public static void registerAgent(String id, Agent agent) {
        if (agent != null && id != null) {
            runningAgents.put(id, agent);
            lastActiveAgent = agent;

            // 如果智能体有类型信息，则添加到类型映射
            if (agent instanceof BaseAgent) {
                String agentType = ((BaseAgent) agent).getDescription();
                if (agentType != null && !agentType.isEmpty()) {
                    agentTypeMap.computeIfAbsent(agentType, k -> ConcurrentHashMap.newKeySet())
                                .add(id);
                }
            }

            log.info("AgentRegistry: 注册智能体, id={}, type={}",
                    id, agent instanceof BaseAgent ? ((BaseAgent) agent).getDescription() : "unknown");
        }
    }

    /**
     * 根据ID获取智能体实例
     * @param id 智能体唯一标识
     * @return 对应的智能体实例，如果不存在则返回null
     */
    public static Agent getAgent(String id) {
        Agent agent = runningAgents.get(id);
        if (agent == null) {
            log.warn("AgentRegistry: 未找到ID为 {} 的智能体", id);
        }
        return agent;
    }

    /**
     * 获取当前活跃的智能体
     * 优先返回ThreadLocal中的智能体，如果没有则返回最后注册的智能体
     * @return 当前智能体实例或null
     */
    public static Agent getCurrentAgent() {
        // 首先尝试从ThreadLocal上下文获取
        ToolCallAgent threadAgent = ToolCallAgentContextHolder.getCurrentAgent();
        if (threadAgent != null) {
            return threadAgent;
        }

        // 如果没有找到，尝试从注册表中获取最后一个活跃的智能体
        if (!runningAgents.isEmpty()) {
            // 查找状态为RUNNING的智能体
            for (Agent agent : runningAgents.values()) {
                if (agent.getState() == AgentState.RUNNING) {
                    log.info("AgentRegistry: 返回找到的运行中智能体");
                    return agent;
                }
            }

            // 如果没有运行中的智能体，返回最后活跃的智能体
            if (lastActiveAgent != null) {
                log.info("AgentRegistry: 返回最后活跃的智能体");
                return lastActiveAgent;
            }

            // 如果还是没有，返回任意一个
            Agent anyAgent = runningAgents.values().iterator().next();
            log.info("AgentRegistry: 返回注册表中的一个智能体");
            return anyAgent;
        }

        log.warn("AgentRegistry: 未找到任何注册的智能体");
        return null;
    }

    /**
     * 注销智能体实例
     * @param id 智能体唯一标识
     */
    public static void unregisterAgent(String id) {
        if (runningAgents.remove(id) != null) {
            log.info("AgentRegistry: 注销ID为 {} 的智能体", id);
        }
    }

    /**
     * 注销所有智能体
     */
    public static void clear() {
        runningAgents.clear();
        lastActiveAgent = null;
        log.info("AgentRegistry: 清空所有注册的智能体");
    }

    /**
     * 获取当前注册的智能体数量
     * @return 智能体数量
     */
    public static int getAgentCount() {
        return runningAgents.size();
    }


    /**
     * 查找处于指定状态的智能体
     *
     * @param state 智能体状态
     * @return 符合状态的智能体列表
     */
    public static List<Agent> getAgentsByState(AgentState state) {
        return runningAgents.values().stream()
                .filter(agent -> agent.getState() == state)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有注册的智能体
     *
     * @return 所有智能体的列表
     */
    public static List<Agent> getAllAgents() {
        return List.copyOf(runningAgents.values());
    }

    /**
     * 获取所有注册的智能体的映射表
     * 返回ID到智能体实例的映射，方便按照ID查找和显示详细信息
     *
     * @return 包含所有智能体的ID到实例映射的Map
     */
    public static Map<String, Agent> getAllAgentsMap() {
        return new ConcurrentHashMap<>(runningAgents);
    }

    /**
     * 获取所有可用智能体类型
     *
     * @return 系统中注册的智能体类型集合
     */
    public static Set<String> getAvailableAgentTypes() {
        return Set.copyOf(agentTypeMap.keySet());
    }

    /**
     * 向智能体发送消息
     * 对于多智能体协作非常有用
     *
     * @param targetAgentId 目标智能体ID
     * @param message 要发送的消息
     * @return 是否成功发送
     */
    public static boolean sendMessageToAgent(String targetAgentId, org.springframework.ai.chat.messages.Message message) {
        Agent targetAgent = getAgent(targetAgentId);
        if (targetAgent != null) {
            try {
                targetAgent.updateMemory(message);
                log.info("AgentRegistry: 成功向智能体 {} 发送消息", targetAgentId);
                return true;
            } catch (Exception e) {
                log.error("AgentRegistry: 向智能体 {} 发送消息失败: {}", targetAgentId, e.getMessage());
            }
        }
        return false;
    }
}
