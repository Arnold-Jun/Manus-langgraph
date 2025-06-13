package com.zhouruojun.manus.infrastructure.config;

import com.zhouruojun.manus.infrastructure.a2a.A2aRegistry;
import com.zhouruojun.manus.infrastructure.a2a.AgentCapabilities;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "manus.remote-agents")
@Data
public class RemoteAgentConfig {
    private List<RemoteAgent> agents = new ArrayList<>();

    @Data
    public static class RemoteAgent {
        private String id;
        private String name;
        private String endpoint;
        private List<Skill> skills = new ArrayList<>();

        @Data
        public static class Skill {
            private String id;
            private String name;
            private String description;
            private List<String> tags = new ArrayList<>();
        }
    }

    @Bean
    public A2aRegistry a2aRegistry() {
        A2aRegistry registry = new A2aRegistry();
        
        // 注册配置的远程智能体
        for (RemoteAgent agent : agents) {
            AgentCapabilities capabilities = AgentCapabilities.builder()
                    .agentId(agent.getId())
                    .name(agent.getName())
                    .skills(agent.getSkills().stream()
                            .map(skill -> AgentCapabilities.Skill.builder()
                                    .id(skill.getId())
                                    .name(skill.getName())
                                    .description(skill.getDescription())
                                    .tags(skill.getTags())
                                    .build())
                            .toList())
                    .features(AgentCapabilities.Features.builder()
                            .streaming(true)
                            .pushNotifications(true)
                            .stateTracking(true)
                            .asyncInvocation(true)
                            .build())
                    .build();

            registry.registerAgent(agent.getId(), agent.getEndpoint(), capabilities);
        }

        return registry;
    }
} 