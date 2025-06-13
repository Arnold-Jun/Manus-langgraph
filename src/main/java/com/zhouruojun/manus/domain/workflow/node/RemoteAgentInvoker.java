package com.zhouruojun.manus.domain.workflow.node;

import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.zhouruojun.manus.infrastructure.a2a.A2aClient;
import com.zhouruojun.manus.infrastructure.a2a.AgentCapabilities;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * 远程智能体调用器
 * 用于在工作流中调用远程智能体服务
 */
@Slf4j
public class RemoteAgentInvoker implements NodeAction<AgentMessageState> {
    private final A2aClient a2aClient;
    private final String targetAgentId;
    private final String skillId;

    public RemoteAgentInvoker(A2aClient a2aClient, String targetAgentId, String skillId) {
        this.a2aClient = a2aClient;
        this.targetAgentId = targetAgentId;
        this.skillId = skillId;
    }

    @Override
    public Map<String, Object> apply(AgentMessageState state) {
        try {
            // 首先检查目标智能体是否可用
            AgentMessageState resultState = a2aClient.checkAgentAvailability(targetAgentId)
                    .flatMap(available -> {
                        if (!available) {
                            return Mono.error(new RuntimeException("远程智能体不可用: " + targetAgentId));
                        }

                        // 获取智能体能力描述
                        return a2aClient.getAgentCapabilities(targetAgentId)
                                .flatMap(capabilities -> {
                                    // 检查是否支持请求的技能
                                    if (!hasSkill(capabilities, skillId)) {
                                        return Mono.error(new RuntimeException(
                                                String.format("智能体 %s 不支持技能 %s", targetAgentId, skillId)));
                                    }

                                    // 准备调用参数
                                    Optional<String> currentMessage = state.currentMessage();
                                    List<ChatMessage> sessionHistory = state.sessionHistory();

                                    Map<String, Object> params = new HashMap<>();
                                    params.put("userInput", currentMessage.orElse(""));
                                    params.put("history", sessionHistory);
                                    params.put("context", sessionHistory);

                                    // 调用远程智能体的技能
                                    return a2aClient.invokeSkill(targetAgentId, skillId, params)
                                            .map(result -> {
                                                // 创建新的状态数据
                                                Map<String, Object> newState = new HashMap<>();
                                                
                                                // 复制现有消息
                                                List<ChatMessage> messages = state.messages();
                                                messages.add(AiMessage.from(result));
                                                newState.put("messages", messages);

                                                // 复制会话历史
                                                List<ChatMessage> history = state.sessionHistory();
                                                history.add(AiMessage.from(result));
                                                newState.put("sessionHistory", history);

                                                // 更新状态
                                                newState.put("currentAgent", targetAgentId);
                                                newState.put("result", result);
                                                newState.put("nextNode", "coordinator");
                                                newState.put("isFinished", false);

                                                return new AgentMessageState(newState);
                                            });
                                });
                    })
                    .doOnSuccess(result -> log.info("成功调用远程智能体 {}", targetAgentId))
                    .doOnError(error -> log.error("调用远程智能体 {} 失败: {}", targetAgentId, error.getMessage()))
                    .block(); // 阻塞等待结果

            if (resultState == null) {
                throw new RuntimeException("远程智能体调用失败：无响应");
            }

            // 返回更新后的状态
            Map<String, Object> result = new HashMap<>();
            result.put("currentAgent", targetAgentId);
            result.put("result", resultState.result().orElse(""));
            result.put("nextNode", "coordinator");
            result.put("messages", resultState.messages());
            result.put("sessionHistory", resultState.sessionHistory());
            result.put("isFinished", false);
            return result;

        } catch (Exception e) {
            log.error("远程智能体调用失败", e);
            Map<String, Object> errorState = new HashMap<>();
            List<ChatMessage> messages = state.messages();
            messages.add(AiMessage.from("远程智能体调用失败: " + e.getMessage()));
            errorState.put("messages", messages);
            errorState.put("error", "远程智能体调用失败: " + e.getMessage());
            errorState.put("nextNode", "coordinator");
            errorState.put("isFinished", true);
            return errorState;
        }
    }

    private boolean hasSkill(AgentCapabilities capabilities, String skillId) {
        return capabilities.getSkills().stream()
                .anyMatch(skill -> skill.getId().equals(skillId));
    }
} 