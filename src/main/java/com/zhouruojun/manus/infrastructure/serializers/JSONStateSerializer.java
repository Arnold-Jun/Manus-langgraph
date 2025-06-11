package com.zhouruojun.manus.infrastructure.serializers;

import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.bsc.langgraph4j.serializer.plain_text.jackson.JacksonStateSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class JSONStateSerializer extends JacksonStateSerializer<AgentMessageState> {
    public JSONStateSerializer() {
        super(AgentMessageState::new);
        this.objectMapper.registerModule((new SimpleModule()).addDeserializer(ToolExecutionRequest.class, new ToolExecutionRequestDeserializer()));
    }

    public void write(AgentMessageState object, ObjectOutput out) throws IOException {
        String json = this.objectMapper.writeValueAsString(object);
        out.writeUTF(json);
    }

    public AgentMessageState read(ObjectInput in) throws IOException, ClassNotFoundException {
        String json = in.readUTF();
        return (AgentMessageState)this.objectMapper.readValue(json, AgentMessageState.class);
    }
}