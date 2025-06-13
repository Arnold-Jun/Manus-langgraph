package com.zhouruojun.manus.infrastructure.a2a;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能体能力描述类
 * 用于描述智能体具备的各种能力和特性
 */
@Data
@Builder
public class AgentCapabilities {
    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * 支持的技能列表
     */
    private List<Skill> skills;

    /**
     * 技能描述类
     */
    @Data
    @Builder
    public static class Skill {
        /**
         * 技能ID
         */
        private String id;

        /**
         * 技能名称
         */
        private String name;

        /**
         * 技能描述
         */
        private String description;

        /**
         * 技能参数描述
         */
        private Map<String, String> parameters;

        /**
         * 技能标签
         */
        private List<String> tags;
    }

    /**
     * 特性标记
     */
    private Features features;

    /**
     * 特性描述类
     */
    @Data
    @Builder
    public static class Features {
        /**
         * 是否支持流式输出
         */
        private boolean streaming;

        /**
         * 是否支持推送通知
         */
        private boolean pushNotifications;

        /**
         * 是否支持状态追踪
         */
        private boolean stateTracking;

        /**
         * 是否支持异步调用
         */
        private boolean asyncInvocation;
    }
} 