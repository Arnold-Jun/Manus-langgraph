package com.zhouruojun.manus.core.base;

import com.zhouruojun.manus.model.LLM;
import com.zhouruojun.manus.model.Memory;
import com.zhouruojun.manus.model.Message;
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

            // 使用LLM生成自然语言响应，而不是使用预设的句子
            CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> {
                // 创建一个系统提示，指导LLM如何总结结果
                String summaryPromptText =
                        "你是一个智能助手，请根据以下执行步骤生成一个简洁、自然、友好的回复。" +
                                "不要重复所有执行步骤的细节，只需总结主要成果和重要信息。" +
                                "如果工具执行出现了错误，请简要解释问题。" +
                                "如果只是思考但没有实际行动，请表明你理解了用户请求但不需要执行具体操作。" +
                                "如果没有执行任何步骤，请询问用户需要帮助的内容。";

                // 将执行摘要作为上下文
                String executionContextText =
                        "以下是执行步骤的结果：\n\n" + executionSummary.toString() +
                                "\n\n请根据这些信息生成一个对用户友好的回复。";

                // 创建Manus项目的Message对象
                List<Message> tempMessages = new ArrayList<>();
                tempMessages.add(Message.systemMessage(summaryPromptText, null));
                tempMessages.add(Message.userMessage(executionContextText, null));

                // 调用LLM生成响应
                try {
                    return llm.call(tempMessages).get();
                } catch (Exception e) {
                    log.error("使用LLM生成响应时出错: {}", e.getMessage(), e);
                    // 出错时返回一个基本响应
                    if (results.isEmpty()) {
                        return "我没有执行任何步骤，请问您需要我帮您做什么？";
                    } else if (!hasActionTaken.get()) {
                        return "我理解了您的请求，但目前并不需要执行特定操作。请问您有其他问题或需求吗？";
                    } else {
                        return "我已为您完成了请求。执行过程中出现了一些情况，可能需要您的进一步指导。";
                    }
                }
            });

            try {
                // 等待LLM响应并返回
                String generatedResponse = responseFuture.get();

                // 如果需要，可以在末尾附加执行步骤的详细信息
                 generatedResponse += "\n\n详细执行步骤：\n" + executionSteps;

                return generatedResponse;
            } catch (Exception e) {
                log.error("等待LLM响应时出错: {}", e.getMessage(), e);
                // 如果出现异常，返回一个基本响应
                return "我已处理完您的请求，但在生成详细回复时遇到了问题。您可以查看执行结果:\n\n" + executionSteps;
            }
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
}

