package com.zhouruojun.manus.infrastructure.serializers;

import com.zhouruojun.manus.infrastructure.serializers.ChatMessageSerializer;
import com.zhouruojun.manus.domain.model.AgentMessageState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.langchain4j.serializer.std.ToolExecutionRequestSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;

public class STDStateSerializer extends ObjectStreamStateSerializer<AgentMessageState> {
    public STDStateSerializer() {
        super(AgentMessageState::new);
        this.mapper().register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer());
        this.mapper().register(ChatMessage.class, new ChatMessageSerializer());
    }
}