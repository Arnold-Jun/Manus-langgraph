package com.zhouruojun.manus.domain.model;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Message wrapper class that integrates with Spring AI's message types
 * while maintaining compatibility with the existing Manus project structure.
 */
@Getter
@Setter
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Role role;
    private String content;
    private String base64Image;
    private String toolCallId;
    private transient org.springframework.ai.chat.messages.Message springMessage;  // 标记为transient，不参与序列化

    public Message(Role role, String content, String base64Image, String toolCallId) {
        Assert.notNull(role, "Role cannot be null");
        Assert.hasText(content, "Content cannot be empty");

        this.role = role;
        this.content = content;
        this.base64Image = base64Image;
        this.toolCallId = toolCallId;
        this.springMessage = convertToSpringMessage();
    }

    /**
     * Converts this message to a Spring AI message based on the role
     */
    private org.springframework.ai.chat.messages.Message convertToSpringMessage() {
        Map<String, Object> metadata = new HashMap<>();
        if (base64Image != null && !base64Image.isEmpty()) {
            metadata.put("base64Image", base64Image);
        }

        List<Media> mediaList = new ArrayList<>();
        // 将base64Image转换为Media对象
        if (base64Image != null && !base64Image.isEmpty()) {
            // 创建基于base64的Media对象
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            Media imageMedia = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg")) // 假设是JPEG格式，可根据实际情况调整
                .data(imageBytes)
                .build();
            mediaList.add(imageMedia);
        }

        switch(role) {
            case USER:
                UserMessage.Builder userBuilder = UserMessage.builder()
                    .text(content)
                    .metadata(metadata);
                if (!mediaList.isEmpty()) {
                    userBuilder.media(mediaList);
                }
                return userBuilder.build();
            case SYSTEM:
                return SystemMessage.builder()
                    .text(content)
                    .metadata(metadata)
                    .build();
            case ASSISTANT:
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
                // Add tool calls if needed
                return new AssistantMessage(content, metadata, toolCalls, mediaList);
            case TOOL:
                Assert.hasText(toolCallId, "Tool call ID must be provided for tool messages");
                List<ToolResponseMessage.ToolResponse> responses = List.of(
                    new ToolResponseMessage.ToolResponse(toolCallId, "tool", content)
                );
                return new ToolResponseMessage(responses, metadata);
            default:
                throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }

    /**
     * Creates a Message instance from a Spring AI message
     */
    public static Message fromSpringMessage(org.springframework.ai.chat.messages.Message springMessage) {
        String content = springMessage.getText();
        Map<String, Object> metadata = springMessage.getMetadata();
        String base64Image = (String) metadata.getOrDefault("base64Image", "");
        String toolCallId = "";

        Role role;
        if (springMessage instanceof UserMessage) {
            role = Role.USER;
        }
        else if (springMessage instanceof SystemMessage) {
            role = Role.SYSTEM;
        }
        else if (springMessage instanceof AssistantMessage) {
            role = Role.ASSISTANT;
        }
        else if (springMessage instanceof ToolResponseMessage toolResponseMessage) {
            role = Role.TOOL;
            if (!toolResponseMessage.getResponses().isEmpty()) {
                toolCallId = toolResponseMessage.getResponses().get(0).id();
                content = toolResponseMessage.getResponses().get(0).responseData();
            }
        }
        else {
            throw new IllegalArgumentException("Unsupported message type: " + springMessage.getClass().getName());
        }

        return Message.builder()
                .role(role)
                .content(content)
                .base64Image(base64Image)
                .toolCallId(toolCallId)
                .build();
    }

    // Static factory methods
    public static Message userMessage(String content, String base64Image) {
        return Message.builder()
                .role(Role.USER)
                .content(content)
                .base64Image(base64Image)
                .build();
    }

    public static Message systemMessage(String content, String base64Image) {
        return Message.builder()
                .role(Role.SYSTEM)
                .content(content)
                .base64Image(base64Image)
                .build();
    }

    public static Message assistantMessage(String content, String base64Image) {
        return Message.builder()
                .role(Role.ASSISTANT)
                .content(content)
                .base64Image(base64Image)
                .build();
    }

    public static Message toolMessage(String content, String toolCallId, String base64Image) {
        return Message.builder()
                .role(Role.TOOL)
                .content(content)
                .toolCallId(toolCallId)
                .base64Image(base64Image)
                .build();
    }

    // Builder pattern
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    public static class MessageBuilder {
        private Role role;
        private String content = "";
        private String base64Image = "";
        private String toolCallId = "";

        public MessageBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public MessageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public MessageBuilder base64Image(String base64Image) {
            this.base64Image = base64Image != null ? base64Image : "";
            return this;
        }

        public MessageBuilder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId != null ? toolCallId : "";
            return this;
        }

        public Message build() {
            return new Message(role, content, base64Image, toolCallId);
        }
    }

    /**
     * Gets the Spring AI message
     */
    public org.springframework.ai.chat.messages.Message getSpringMessage() {
        if (this.springMessage == null) {
            this.springMessage = convertToSpringMessage();
        }
        return this.springMessage;
    }

    /**
     * Explicit getter for role field (in case Lombok doesn't work)
     */
    public Role getRole() {
        return this.role;
    }

    /**
     * Explicit getter for content field (in case Lombok doesn't work)
     */
    public String getContent() {
        return this.content;
    }
}
