package com.zhouruojun.manus.domain.agent.base;

/**
 * 用于存储和获取当前线程上下文中的ToolCallAgent实例
 * 这允许工具类在不直接依赖于特定Agent实例的情况下
 * 访问和操作当前正在运行的Agent实例
 */
public class ToolCallAgentContextHolder {
    // 使用ThreadLocal存储当前线程的ToolCallAgent实例
    private static final ThreadLocal<ToolCallAgent> currentAgentHolder = new ThreadLocal<>();

    /**
     * 设置当前线程的ToolCallAgent实例
     * 在ToolCallAgent开始处理请求前调用
     *
     * @param agent 当前活动的ToolCallAgent实例
     */
    public static void setCurrentAgent(ToolCallAgent agent) {
        currentAgentHolder.set(agent);
    }

    /**
     * 获取当前线程的ToolCallAgent实例
     *
     * @return 当前活动的ToolCallAgent实例，如果不存在则返回null
     */
    public static ToolCallAgent getCurrentAgent() {
        return currentAgentHolder.get();
    }

    /**
     * 清除当前线程的ToolCallAgent实例
     * 在ToolCallAgent完成请求处理后调用
     */
    public static void clear() {
        currentAgentHolder.remove();
    }
}
