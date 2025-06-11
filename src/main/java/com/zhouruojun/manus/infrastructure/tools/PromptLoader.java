package com.zhouruojun.manus.infrastructure.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 提示词加载工具类
 */
@Component
public class PromptLoader {
    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);
    private static final Map<String, String> promptCache = new HashMap<>();

    /**
     * 从resources/prompts目录下加载提示词文件
     * @param fileName 文件名（不包含路径）
     * @return 提示词内容
     */
    public String loadPrompt(String fileName) {
        try {
            // 如果缓存中存在，直接返回
            if (promptCache.containsKey(fileName)) {
                return promptCache.get(fileName);
            }

            // 从classpath加载文件
            ClassPathResource resource = new ClassPathResource("prompts/" + fileName);
            String content = Files.readString(Path.of(resource.getURI()), StandardCharsets.UTF_8);
            
            // 缓存结果
            promptCache.put(fileName, content);
            
            return content;
        } catch (IOException e) {
            log.error("Failed to load prompt file: {}", fileName, e);
            throw new RuntimeException("Failed to load prompt file: " + fileName, e);
        }
    }

    /**
     * 从resources/prompts目录下加载提示词文件，并替换占位符
     * @param fileName 文件名（不包含路径）
     * @param replacements 替换的键值对
     * @return 替换后的提示词内容
     */
    public String loadPromptWithReplacements(String fileName, Map<String, String> replacements) {
        String content = loadPrompt(fileName);
        
        // 替换所有占位符
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            content = content.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return content;
    }
} 