// AppConfig.java
package com.zhouruojun.manus.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "manus")
public class AppConfig {
    
    /**
     * 工作空间根目录
     */
    private String workspaceRoot = System.getProperty("user.dir");
    
    /**
     * 最大步骤数
     */
    private int maxSteps = 20;
    
    /**
     * 最大观察次数
     */
    private int maxObserve = 10000;
    
    /**
     * 重复阈值
     */
    private int duplicateThreshold = 2;
}