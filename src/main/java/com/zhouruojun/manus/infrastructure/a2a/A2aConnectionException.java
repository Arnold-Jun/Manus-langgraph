package com.zhouruojun.manus.infrastructure.a2a;

/**
 * 远程智能体连接异常
 */
public class A2aConnectionException extends RuntimeException {
    public A2aConnectionException(String message) {
        super(message);
    }

    public A2aConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
} 