package com.zhouruojun.manus.service;

import com.zhouruojun.manus.model.Message;
import com.zhouruojun.manus.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 * 负责维护会话状态和对话历史
 */
@Service
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    // 存储每个会话的对话历史
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    /**
     * 获取会话的对话历史
     */
    public List<Message> getSessionHistory(String sessionId) {
        return sessionHistory.computeIfAbsent(sessionId, id -> new ArrayList<>());
    }

    /**
     * 添加用户消息到会话历史
     */
    public void addUserMessage(String sessionId, String content) {
        List<Message> history = getSessionHistory(sessionId);
        Message userMessage = Message.builder()
                .role(Role.USER)
                .content(content)
                .build();
        history.add(userMessage);
        log.debug("Added user message to session {}: {}", sessionId, content);
    }

    /**
     * 添加系统响应到会话历史
     */
    public void addSystemResponse(String sessionId, String content) {
        List<Message> history = getSessionHistory(sessionId);
        Message assistantMessage = Message.builder()
                .role(Role.ASSISTANT)
                .content(content)
                .build();
        history.add(assistantMessage);
        log.debug("Added system response to session {}", sessionId);
    }

    /**
     * 清除会话历史
     */
    public void clearSessionHistory(String sessionId) {
        sessionHistory.remove(sessionId);
        log.info("Cleared history for session {}", sessionId);
    }

    /**
     * 获取会话历史的简要摘要（用于调试）
     */
    public String getSessionSummary(String sessionId) {
        List<Message> history = getSessionHistory(sessionId);
        if (history.isEmpty()) {
            return "空会话";
        }

        return String.format("会话包含 %d 条消息 (用户: %d, 系统: %d)",
                history.size(),
                history.stream().filter(m -> Role.USER.equals(m.getRole())).count(),
                history.stream().filter(m -> Role.ASSISTANT.equals(m.getRole())).count());
    }
}
