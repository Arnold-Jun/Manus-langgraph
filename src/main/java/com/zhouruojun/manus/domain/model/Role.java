package com.zhouruojun.manus.domain.model;

public enum Role {
    USER("user"),
    SYSTEM("system"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}