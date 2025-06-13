package com.zhouruojun.manus.infrastructure.a2a;

import reactor.core.publisher.Mono;

/**
 * Agent-to-Agent通信客户端接口
 * 定义了智能体之间通信的基本方法
 */
public interface A2aClient {
    /**
     * 发送消息给远程智能体
     * @param targetAgentId 目标智能体ID
     * @param message 消息内容
     * @return 响应结果
     */
    Mono<String> sendMessage(String targetAgentId, String message);

    /**
     * 调用远程智能体的特定技能
     * @param targetAgentId 目标智能体ID
     * @param skillId 技能ID
     * @param params 调用参数
     * @return 技能执行结果
     */
    Mono<String> invokeSkill(String targetAgentId, String skillId, Object params);

    /**
     * 检查远程智能体是否可用
     * @param agentId 智能体ID
     * @return 是否可用
     */
    Mono<Boolean> checkAgentAvailability(String agentId);

    /**
     * 获取远程智能体的能力描述
     * @param agentId 智能体ID
     * @return 能力描述
     */
    Mono<AgentCapabilities> getAgentCapabilities(String agentId);
} 