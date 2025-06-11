// Agent.java
package com.zhouruojun.manus.domain.agent.base;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Agent {
    /**
     * 初始化智能体
     * 在智能体创建后调用此方法进行必要的初始化操作
     */
    void initialize();

    CompletableFuture<String> run(String request);

    void updateMemory(Message message);

    CompletableFuture<String> step();

    List<Message> getMessages();

    AgentState getState();

    /**
     * 获取智能体名称
     * @return 智能体名称
     */
    String getName();
}
