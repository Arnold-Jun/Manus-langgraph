package com.zhouruojun.manus.core.specialized;

import com.zhouruojun.manus.core.base.Agent;
import com.zhouruojun.manus.core.base.AgentState;
import com.zhouruojun.manus.core.base.ToolCallAgent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 专门化智能体的抽象基类
 * 使用委托模式实现Agent接口
 * 简化专门智能体的实现
 */
@Slf4j
public abstract class AbstractSpecializedAgent implements Agent {

    /**
     * 委托对象 - 实际的智能体实现
     */
    @Getter
    protected final ToolCallAgent delegate;

    /**
     * 创建专门化智能体
     *
     * @param delegate 委托的ToolCallAgent对象
     */
    protected AbstractSpecializedAgent(ToolCallAgent delegate) {
        this.delegate = delegate;
        log.info("创建专门化智能体：{}, ID: {}", this.getClass().getSimpleName(), delegate.getConversationId());
    }

    @Override
    public CompletableFuture<String> run(String request) {
        return delegate.run(request);
    }

    @Override
    public void updateMemory(Message message) {
        delegate.updateMemory(message);
    }

    @Override
    public CompletableFuture<String> step() {
        return delegate.step();
    }

    @Override
    public List<Message> getMessages() {
        return delegate.getMessages();
    }

    @Override
    public AgentState getState() {
        return delegate.getState();
    }

    @Override
    public void initialize() {
        // 默认实现委托给delegate的initialize方法
        delegate.initialize();
        log.info("初始化专门化智能体：{}", this.getClass().getSimpleName());
    }

    /**
     * 获取智能体名称
     */
    public String getName() {
        return delegate.getName();
    }

    /**
     * 获取智能体描述
     */
    public String getDescription() {
        return delegate.getDescription();
    }

    /**
     * 获取智能体会话ID
     */
    public String getConversationId() {
        return delegate.getConversationId();
    }

    /**
     * 获取系统提示词
     */
    public String getSystemPrompt() {
        return delegate.getSystemPrompt();
    }

    /**
     * 设置系统提示词
     */
    protected void setSystemPrompt(String systemPrompt) {
        delegate.setSystemPrompt(systemPrompt);
    }
}
