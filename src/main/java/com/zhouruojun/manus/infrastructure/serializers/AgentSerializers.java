package com.zhouruojun.manus.infrastructure.serializers;

import com.zhouruojun.manus.domain.model.AgentMessageState;
import org.bsc.langgraph4j.serializer.StateSerializer;


public enum AgentSerializers {


    STD(new STDStateSerializer()),
    JSON(new JSONStateSerializer());

    private final StateSerializer<AgentMessageState> serializer;

    private AgentSerializers(StateSerializer<AgentMessageState> serializer) {
        this.serializer = serializer;
    }

    public StateSerializer<AgentMessageState> object() {
        return this.serializer;
    }

}
