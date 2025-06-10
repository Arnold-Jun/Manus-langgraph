package com.zhouruojun.manus.exception;

/**
 * 表示在LLM处理过程中超过了令牌限制时抛出的异常
 */
public class TokenLimitExceededException extends RuntimeException {

    private final int maxTokens;
    private final int requestedTokens;

    /**
     * 创建一个新的TokenLimitExceededException实例
     *
     * @param message 异常消息
     */
    public TokenLimitExceededException(String message) {
        super(message);
        this.maxTokens = -1;
        this.requestedTokens = -1;
    }

    /**
     * 创建一个新的TokenLimitExceededException实例，包含令牌限制详情
     *
     * @param message 异常消息
     * @param maxTokens 最大允许的令牌数
     * @param requestedTokens 请求的令牌数
     */
    public TokenLimitExceededException(String message, int maxTokens, int requestedTokens) {
        super(message + String.format(" (请求: %d, 限制: %d)", requestedTokens, maxTokens));
        this.maxTokens = maxTokens;
        this.requestedTokens = requestedTokens;
    }

    /**
     * 创建一个新的TokenLimitExceededException实例，包含原因
     *
     * @param message 异常消息
     * @param cause 原因
     */
    public TokenLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.maxTokens = -1;
        this.requestedTokens = -1;
    }

    /**
     * 创建一个新的TokenLimitExceededException实例，包含令牌限制详情和原因
     *
     * @param message 异常消息
     * @param cause 原因
     * @param maxTokens 最大允许的令牌数
     * @param requestedTokens 请求的令牌数
     */
    public TokenLimitExceededException(String message, Throwable cause, int maxTokens, int requestedTokens) {
        super(message + String.format(" (请求: %d, 限制: %d)", requestedTokens, maxTokens), cause);
        this.maxTokens = maxTokens;
        this.requestedTokens = requestedTokens;
    }

    /**
     * 获取最大允许的令牌数
     *
     * @return 最大令牌数，如果未指定则返回-1
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * 获取请求的令牌数
     *
     * @return 请求的令牌数，如果未指定则返回-1
     */
    public int getRequestedTokens() {
        return requestedTokens;
    }

    /**
     * 检查是否有令牌限制详情
     *
     * @return 如果有令牌限制详情则返回true
     */
    public boolean hasTokenDetails() {
        return maxTokens != -1 && requestedTokens != -1;
    }
}
