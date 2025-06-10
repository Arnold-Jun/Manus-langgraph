package com.zhouruojun.manus.model;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token管理工具类，用于估算和管理消息的token数量
 */
public class TokenManager {

    /**
     * 估算消息列表的总token数
     * @param messages 消息列表
     * @return 估算的token数
     */
    public static int estimateTokenCount(List<Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            total += estimateTokensForMessage(msg);
        }
        return total;
    }

    /**
     * 估算单条消息的token数
     * 这里使用简单的估算方法，实际应用中可以使用更准确的tokenizer
     * @param message 消息
     * @return 估算的token数
     */
    public static int estimateTokensForMessage(Message message) {
        if (message == null) {
            return 0;
        }

        String content = null;

        // 检查消息类型并提取内容
        if (message instanceof UserMessage) {
            content = ((UserMessage) message).getText();
        } else if (message instanceof SystemMessage) {
            content = ((SystemMessage) message).getText();
        } else if (message instanceof AssistantMessage) {
            content = ((AssistantMessage) message).getText();
        } else {
            content = message.getText();
        }

        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 简单估算：假设平均每个英文单词是1.3个token，每个中文字符是1个token
        int wordCount = countWords(content);
        int chineseCharCount = countChineseChars(content);

        return (int)(wordCount * 1.3) + chineseCharCount + 4; // +4作为消息元数据开销
    }

    /**
     * 计算英文单词数量
     * @param text 文本
     * @return 单词数量
     */
    private static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        String[] words = text.split("\\s+");
        return words.length;
    }

    /**
     * 计算中文字符数量
     * @param text 文本
     * @return 中文字符数量
     */
    private static int countChineseChars(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        Pattern p = Pattern.compile("[\\u4e00-\\u9fa5]");
        Matcher m = p.matcher(text);
        while (m.find()) {
            count++;
        }
        return count;
    }
}
