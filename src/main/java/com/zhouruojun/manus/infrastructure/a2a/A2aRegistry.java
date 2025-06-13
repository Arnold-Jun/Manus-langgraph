package com.zhouruojun.manus.infrastructure.a2a;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体注册中心
 * 管理远程智能体的注册信息和服务发现
 */
@Slf4j
@Component
public class A2aRegistry {
    /**
     * 存储智能体ID到其服务端点的映射
     */
    private final Map<String, String> agentEndpoints = new ConcurrentHashMap<>();

    /**
     * 存储智能体ID到其能力描述的映射
     */
    private final Map<String, AgentCapabilities> agentCapabilities = new ConcurrentHashMap<>();

    /**
     * 注册远程智能体
     * @param agentId 智能体ID
     * @param endpoint 服务端点URL
     * @param capabilities 能力描述
     */
    public void registerAgent(String agentId, String endpoint, AgentCapabilities capabilities) {
        agentEndpoints.put(agentId, endpoint);
        agentCapabilities.put(agentId, capabilities);
        log.info("注册远程智能体: {}, 端点: {}", agentId, endpoint);
    }

    /**
     * 注销远程智能体
     * @param agentId 智能体ID
     */
    public void unregisterAgent(String agentId) {
        agentEndpoints.remove(agentId);
        agentCapabilities.remove(agentId);
        log.info("注销远程智能体: {}", agentId);
    }

    /**
     * 获取智能体的服务端点
     * @param agentId 智能体ID
     * @return 服务端点URL
     */
    public Mono<String> getAgentEndpoint(String agentId) {
        String endpoint = agentEndpoints.get(agentId);
        if (endpoint == null) {
            return Mono.error(new AgentNotFoundException("找不到智能体: " + agentId));
        }
        return Mono.just(endpoint);
    }

    /**
     * 获取智能体的能力描述
     * @param agentId 智能体ID
     * @return 能力描述
     */
    public Mono<AgentCapabilities> getAgentCapabilities(String agentId) {
        AgentCapabilities capabilities = agentCapabilities.get(agentId);
        if (capabilities == null) {
            return Mono.error(new AgentNotFoundException("找不到智能体: " + agentId));
        }
        return Mono.just(capabilities);
    }

    /**
     * 检查智能体是否已注册
     * @param agentId 智能体ID
     * @return 是否已注册
     */
    public boolean isAgentRegistered(String agentId) {
        return agentEndpoints.containsKey(agentId);
    }

    /**
     * 获取所有注册的智能体ID
     * @return 智能体ID集合
     */
    public Iterable<String> getAllAgentIds() {
        return agentEndpoints.keySet();
    }
} 