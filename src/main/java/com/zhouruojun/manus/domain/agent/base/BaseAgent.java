package com.zhouruojun.manus.domain.agent.base;

import com.zhouruojun.manus.domain.model.LLM;
import com.zhouruojun.manus.domain.model.Memory;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Data
@Slf4j
@SuperBuilder
public abstract class BaseAgent implements Agent {
    // 核心属性
    @Default
    private String name = "";  // 移除final关键字，默认为空字符串而非UUID
    @Default
    private String description = "Base Agent";
    protected ChatResponse response;
    @Default
    protected String conversationId = UUID.randomUUID().toString();

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 依赖项
    private LLM llm;
    @Default
    private Memory memory = new Memory();
    @Default
    private AgentState state = AgentState.IDLE;
    private ChatModel chatModel;

    // 执行控制
    @Default
    private int maxSteps = 10;
    @Default
    private int currentStep = 0;
    @Default
    private int duplicateThreshold = 2;

    /**
     * 初始化智能体的基本组件 - 在构建后调用此方法
     */
    public void initialize() {
        // 传递会话ID到Memory
        this.memory.setConversationId(this.conversationId);

        // 使用ChatModel初始化LLM
        if (chatModel != null) {
            this.llm = new LLM(chatModel);
        } else {
            throw new IllegalStateException("ChatModel未提供，无法初始化LLM");
        }

        // 初始化系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            // 在系统提示中添加智能体ID信息，以便AI能够在调用terminate工具时使用
            String enhancedPrompt = systemPrompt +
                "\n\n[META INFO]\n" +
                "Your agent ID: " + this.name + "\n" +
                "When calling the terminate tool, always include your agent ID for proper termination in multi-agent environments.\n" +
                "[END META INFO]";

            SystemMessage systemMessage = new SystemMessage(enhancedPrompt);
            updateMemory(systemMessage);
        }
    }

    /**
     * 状态上下文方法：在特定状态下执行操作
     *
     * @param newState 要切换到的新状态
     * @param action   要执行的操作
     * @return 操作的结果
     */
    protected <T> T withState(AgentState newState, Supplier<T> action) {
        if (newState == null) {
            throw new IllegalArgumentException("无效状态: null");
        }

        AgentState previousState = this.state;
        this.state = newState;
        try {
            return action.get();
        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("在{}状态执行操作时发生错误: {}", newState, e.getMessage(), e);
            throw e;
        } finally {
            // 只有在执行成功且不是终止状态时才恢复前一个状态
            if (this.state != AgentState.ERROR &&
                    this.state != AgentState.FINISHED) {
                this.state = previousState;
            }
        }
    }

    @Override
    public CompletableFuture<String> run(String request) {
        return CompletableFuture.supplyAsync(() -> {
            if (state != AgentState.IDLE && state != AgentState.FINISHED) {
                log.warn("尝试在{}状态下运行智能体", state);
                throw new IllegalStateException("不能从状态运行智能体: " + state);
            }

            // 重置状态
            if (state == AgentState.FINISHED) {
                state = AgentState.IDLE;
                currentStep = 0;
            }

            if (request != null && !request.isEmpty()) {
                UserMessage userMessage = new UserMessage(request);
                updateMemory(userMessage);
            }

            List<String> results = new ArrayList<>();
            // 使用AtomicBoolean代替boolean以便在lambda表达式中修改
            AtomicBoolean hasActionTaken = new AtomicBoolean(false);

            withState(AgentState.RUNNING, () -> {
                while (currentStep < maxSteps && state != AgentState.FINISHED) {
                    currentStep++;
                    log.info("执行步骤 {}/{}", currentStep, maxSteps);

                    String stepResult = step().join();

                    if (isStuck()) {
                        handleStuckState();
                    }

                    // 检查是否执行了实际操作
                    if (!stepResult.contains("无需行动")) {
                        hasActionTaken.set(true);
                    }

                    results.add("步骤 " + currentStep + ": " + stepResult);
                }

                if (currentStep >= maxSteps) {
                    log.info("达到最大步骤数 ({}), 终止执行", maxSteps);
                    setState(AgentState.IDLE);
                    currentStep = 0;
                    results.add("终止: 达到最大步骤数 (" + maxSteps + ")");
                }

                return null;
            });

            // 生成对话形式的回复
            StringBuilder executionSummary = new StringBuilder();

            // 首先收集执行步骤的结果信息
            String executionSteps = String.join("\n", results);
            executionSummary.append(executionSteps);

            if (results.isEmpty()) {
                return "我没有执行任何步骤，请问您需要我帮您做什么？";
            }
            return String.join("\n", results);
        });
    }

    @Override
    public void updateMemory(org.springframework.ai.chat.messages.Message message) {
        memory.addMessage(conversationId, message);
    }

    @Override
    public abstract CompletableFuture<String> step();

    @Override
    public List<org.springframework.ai.chat.messages.Message> getMessages() {
        return memory.getMessages();
    }

    /**
     * 处理智能体陷入重复状态的情况
     */
    protected void handleStuckState() {
        String stuckPrompt = "观察到重复响应。考虑新策略，避免重复已尝试过的无效路径。";
        this.nextStepPrompt = stuckPrompt + "\n" + (this.nextStepPrompt != null ? this.nextStepPrompt : "");
        log.warn("智能体检测到循环状态。添加提示: {}", stuckPrompt);
    }

    /**
     * 检测智能体是否陷入重复状态
     *
     * @return 是否陷入重复
     */
    protected boolean isStuck() {
        List<org.springframework.ai.chat.messages.Message> messages = getMessages();
        if (messages.size() < 2) {
            return false;
        }

        org.springframework.ai.chat.messages.Message lastMessage = messages.get(messages.size() - 1);
        String lastContent = getContentFromSpringMessage(lastMessage);
        if (lastContent == null || lastContent.isEmpty()) {
            return false;
        }

        int duplicateCount = 0;
        for (int i = messages.size() - 2; i >= 0; i--) {
            org.springframework.ai.chat.messages.Message msg = messages.get(i);
            if (msg.getMessageType() == MessageType.ASSISTANT &&
                    lastContent.equals(getContentFromSpringMessage(msg))) {
                duplicateCount++;
                if (duplicateCount >= duplicateThreshold) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 提取org.springframework.ai.chat.messages.Message的内容
     */
    private String getContentFromSpringMessage(org.springframework.ai.chat.messages.Message message) {
        // 适配不同类型的Spring Message
        if (message instanceof org.springframework.ai.chat.messages.AssistantMessage) {
            return ((org.springframework.ai.chat.messages.AssistantMessage) message).getText();
        } else if (message instanceof UserMessage) {
            return ((UserMessage) message).getText();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).getText();
        } else if (message != null) {
            // 对于其他类型的消息，尝试直接获取内容
            return message.getText();
        }
        return null;
    }

    /**
     * 实现Agent接口要求的getName方法
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 实现Agent接口要求的getState方法
     */
    @Override
    public AgentState getState() {
        return this.state;
    }

    /**
     * 设置智能体状态
     */
    public void setState(AgentState state) {
        this.state = state;
    }

    /**
     * 获取内存对象
     */
    public Memory getMemory() {
        return this.memory;
    }

    /**
     * 获取会话ID
     */
    public String getConversationId() {
        return this.conversationId;
    }

    /**
     * 获取LLM对象
     */
    public LLM getLlm() {
        return this.llm;
    }

    /**
     * 获取系统提示
     */
    public String getSystemPrompt() {
        return this.systemPrompt;
    }

    /**
     * 设置系统提示
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * 获取最大步数
     */
    public int getMaxSteps() {
        return this.maxSteps;
    }

    /**
     * 设置最大步数
     */
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 设置描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

}

