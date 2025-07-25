package com.zhouruojun.manus.domain.agent.base;

import com.zhouruojun.manus.infrastructure.exception.TokenLimitExceededException;
import com.zhouruojun.manus.domain.model.Message;
import com.zhouruojun.manus.infrastructure.tools.collection.ToolCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ToolCallAgent类用于处理工具/函数调用，提供增强的抽象能力
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Slf4j
@SuperBuilder
public class ToolCallAgent extends ReActAgent {

    private static final String TOOL_CALL_REQUIRED = "需要工具调用但未提供";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 可用工具集合
    @Default
    private ToolCollection availableTools = new ToolCollection();
    @Default
    private String toolChoices = "auto"; // auto, required, none
    @Default
    private List<String> specialToolNames = new ArrayList<>();
    private ChatOptions chatOption;

    // 工具调用相关
    @Default
    private List<ToolCall> toolCalls = new ArrayList<>();
    private Prompt promptWithMemory;
    private String currentBase64Image = null;

    // 执行控制
    private Integer maxObserve = null;

    /**
     * 创建ToolCallAgent的构建器
     * @return ToolCallAgentBuilder实例
     */
    public static ToolCallAgentBuilder<?, ?> builder() {
        return new ToolCallAgentBuilderImpl();
    }

    /**
     * 初始化方法，在创建实例后调用
     */
    @Override
    public void initialize() {
        super.initialize();

        // 设置默认名称和描述
        if (getName() == null) {
            setDescription("toolcall");
        }

        if (getDescription() == null) {
            setDescription("可以执行工具调用的智能体");
        }

        // 设置最大步骤数
        if (getMaxSteps() == 10) { // 如果是父类默认值
            setMaxSteps(30);
        }

        //注册到全局中心
        AgentRegistry.registerAgent(getConversationId(), this);

        // 设置聊天选项
        if (availableTools != null) {
            this.chatOption = availableTools.toChatOptions();
        }
    }

    /**
     * 思考方法：处理当前状态并使用工具决定下一步操作
     * @return 是否需要执行行动
     */
    @Override
    public CompletableFuture<Boolean> think() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🤔 {}开始思考阶段，可用工具数量: {}", getName(), availableTools.getToolCallbacks().length);
                
                // 调用LLM与工具选项 - 只进行一次推理，不执行工具
                ChatResponse response;
                try {
                    response = getLlm().askTool(
                        getMemory().getMessages(),
                        (getSystemPrompt() != null && !getSystemPrompt().trim().isEmpty()) ?
                            Arrays.asList(Message.systemMessage(getSystemPrompt(), null).getSpringMessage()) : null,
                            getMemory(),
                            this.chatOption,
                            getConversationId()).join();
                    this.response = response;

                } catch (Exception e) {
                    // 检查是否是令牌限制错误
                    if (e.getCause() instanceof TokenLimitExceededException) {
                        TokenLimitExceededException tokenError = (TokenLimitExceededException) e.getCause();
                        log.error("🚨 令牌限制错误: {}", tokenError.getMessage());
                        setState(AgentState.FINISHED);
                        return false;
                    }
                    throw e;
                }

                if (response == null) {
                    throw new RuntimeException("从LLM收到的响应为空");
                }

                // 记录LLM的响应
                String llmResponse = response.getResult().getOutput().getText();
                log.info("💭 {}的LLM响应: {}", getName(), llmResponse);
                
                boolean hasToolCalls = response.hasToolCalls();

                getMemory().getChatMemory().add(getConversationId(), response.getResult().getOutput());

                return hasToolCalls;
            } catch (Exception e) {
                log.error("❌ {}的思考过程中发生错误: {}", getName(), e.getMessage(), e);
                setState(AgentState.FINISHED);
                return false;
            }
        });
    }

    /**
     * 行动方法：执行工具调用并处理结果
     * @return 行动的结果
     */
    @Override
    public CompletableFuture<String> act() {
        return CompletableFuture.supplyAsync(() -> {
            // 设置当前线程的智能体上下文
            ToolCallAgentContextHolder.setCurrentAgent(this);
            
            try {
                List<String> results = new ArrayList<>();

                // 添加模型最初的响应文本到结果中
                String initialResponse = this.response.getResult().getOutput().getText();
                results.add(initialResponse);

                if (!this.response.hasToolCalls()) {
                    log.warn("⚠️ {}没有检测到工具调用，但应该调用 webSearch 工具", getName());
                }

                while (this.response.hasToolCalls()) {
                    log.info("🔧 {}是否有工具调用: {}", getName(), this.response.hasToolCalls());
                    log.info("🔧 {}检测到工具调用，数量: {}", getName(), this.response.getResult().getOutput().getToolCalls().size());
                    // 记录工具调用详情
                    this.response.getResult().getOutput().getToolCalls().forEach(toolCall -> {
                        log.info("🛠️ 将要调用工具: {} 参数: {}", toolCall.name(), toolCall.arguments());
                    });
                    
                    log.info("🚀 {}开始执行工具调用", getName());

                    ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
                    // 执行工具调用
                    log.info("🔧 {}正在执行工具...", getName());
                    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(getLlm().prompt,
                            this.response);
                    log.info("✅ {}工具执行完成", getName());
                    
                    // 检查是否是terminate工具调用
                    boolean isTerminate = this.response.getResult().getOutput().getToolCalls().stream()
                            .anyMatch(toolCall -> toolCall.name().equals("terminate"));
                            
                    getMemory().getChatMemory().add(getConversationId(), toolExecutionResult.conversationHistory()
                            .get(toolExecutionResult.conversationHistory().size() - 1));
                            
                    // 如果是terminate工具调用，直接跳出循环
                    if (isTerminate) {
                        String result = toolExecutionResult.conversationHistory()
                                .get(toolExecutionResult.conversationHistory().size() - 1).getText();
                        results.add(result);
                        break;
                    }
                    
                    promptWithMemory = new Prompt(getMemory().getChatMemory().get(getConversationId()), this.chatOption);

                    // 使用对话提示让模型生成一个自然的对话响应
                    String conversationalPrompt = "基于以上的工具调用结果，请以对话的方式向用户解释结果，确保回答清晰友好。";
                    getMemory().addMessage(getConversationId(), Message.userMessage(conversationalPrompt, null).getSpringMessage());

                    // 调用模型获取响应
                    this.response = getLlm().chatModel.call(new Prompt(getMemory().getChatMemory().get(getConversationId()), this.chatOption));
                    String result = this.response.getResult().getOutput().getText();
                    getMemory().addMessage(getConversationId(), this.response.getResult().getOutput());
                    results.add(result);
                }


                // 确保返回的内容采用对话形式，而不仅仅是原始结果
                if (results.isEmpty()) {
                    return "我没有找到相关信息。";
                } else if (results.size() == 1 && !results.get(0).contains("工具调用") && !initialResponse.equals(results.get(0))) {
                    // 如果只有一个结果且不是工具调用，直接返回
                    return results.get(0);
                } else {
                    // 将收集到的结果组合成一个自然的对话响应
                    StringBuilder conversation = new StringBuilder();
                    for (String result : results) {
                        if (result.trim().length() > 0) {
                            conversation.append(result).append("\n\n");
                        }
                    }
                    return conversation.toString().trim();
                }
            } finally {
                // 清理线程上下文
                ToolCallAgentContextHolder.clear();
            }
        });
    }

    /**
     * 运行智能体
     * @param request 请求
     * @return 运行结果
     */
    @Override
    public CompletableFuture<String> run(String request) {
        try {
            // 设置当前线程的智能体上下文
            ToolCallAgentContextHolder.setCurrentAgent(this);
            
            try {
                return super.run(request);
            } finally {
                // 清理线程上下文
                ToolCallAgentContextHolder.clear();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 工具调用的数据结构
     */
    @Getter
    @Setter
    public static class ToolCall {
        private String id;
        private String functionName;
        private Object arguments;

        public ToolCall(String id, String functionName, Object arguments) {
            this.id = id;
            this.functionName = functionName;
            this.arguments = arguments;
        }
    }
}

