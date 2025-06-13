package com.zhouruojun.manus.domain.workflow.engine;

import com.zhouruojun.manus.domain.workflow.node.AgentNodeFactory;
import com.zhouruojun.manus.domain.workflow.node.RemoteAgentInvoker;
import com.zhouruojun.manus.domain.workflow.node.specialized.*;
import com.zhouruojun.manus.domain.model.AgentMessageState;
import com.zhouruojun.manus.domain.model.Message;
import com.zhouruojun.manus.infrastructure.serializers.AgentSerializers;
import com.zhouruojun.manus.infrastructure.tools.PromptLoader;
import com.zhouruojun.manus.infrastructure.config.PromptConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.async.AsyncGenerator;
import com.zhouruojun.manus.infrastructure.a2a.A2aClient;
import com.zhouruojun.manus.infrastructure.a2a.A2aRegistry;
import com.zhouruojun.manus.infrastructure.a2a.AgentCapabilities;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.function.Function;
import java.util.Optional;
import java.util.ArrayList;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 基于LangGraph4j的智能体工作流引擎
 * 使用专门的智能体节点，支持工具调用
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final ChatModel chatModel;
    private final PromptLoader promptLoader;
    private final AgentNodeFactory agentNodeFactory;
    private final A2aClient a2aClient;
    private final A2aRegistry a2aRegistry;
    private final PromptConfig promptConfig;
    private MemorySaver checkpointSaver;
    private CompiledGraph<AgentMessageState> compiledGraph;
    private Map<String, AgentMessageState> sessionStates;
    private StateSerializer<AgentMessageState> stateSerializer;

    public WorkflowEngine(
            ChatModel chatModel,
            PromptLoader promptLoader,
            AgentNodeFactory agentNodeFactory,
            A2aClient a2aClient,
            A2aRegistry a2aRegistry,
            PromptConfig promptConfig) {
        this.chatModel = chatModel;
        this.promptLoader = promptLoader;
        this.agentNodeFactory = agentNodeFactory;
        this.a2aClient = a2aClient;
        this.a2aRegistry = a2aRegistry;
        this.promptConfig = promptConfig;
        this.checkpointSaver = new MemorySaver();
        this.sessionStates = new ConcurrentHashMap<>();

        // 初始化StateSerializer
        this.stateSerializer = AgentSerializers.STD.object();

        try {
            this.compiledGraph = buildAgentWorkflow();
        } catch (GraphStateException e) {
            log.error("工作流构建失败", e);
            throw new RuntimeException("智能体工作流引擎初始化失败", e);
        }
        log.info("工作流引擎初始化完成");
    }

    /**
     * 构建智能体工作流状态图
     */
    private CompiledGraph<AgentMessageState> buildAgentWorkflow() throws GraphStateException {
        // 创建节点实例 - 使用智能体节点和传统节点的混合
        CoordinatorNode coordinatorNode = new CoordinatorNode(chatModel, promptLoader, promptConfig.getNode().getCoordinator());
        
        // 使用AgentNodeFactory创建支持工具调用的智能体节点
        SearchAgentNode searchAgentNode = agentNodeFactory.createSearchAgentNode();
        AnalysisAgentNode analysisAgentNode = agentNodeFactory.createAnalysisAgentNode();
        SummaryNode summaryNode = new SummaryNode(chatModel, promptLoader, promptConfig.getNode().getSummary());
        
        // 创建远程智能体调用器
        Map<String, RemoteAgentInvoker> remoteAgents = new HashMap<>();
        List<String> remoteAgentIds = new ArrayList<>();
        a2aRegistry.getAllAgentIds().forEach(remoteAgentIds::add);
        
        if (remoteAgentIds.isEmpty()) {
            log.info("未发现任何远程智能体配置");
        } else {
            log.info("发现 {} 个远程智能体配置，开始初始化...", remoteAgentIds.size());
            for (String remoteAgentId : remoteAgentIds) {
                try {
                    // 首先检查智能体是否可用
                    a2aClient.checkAgentAvailability(remoteAgentId)
                            .doOnError(error -> log.warn("远程智能体 {} 当前不可用: {}", 
                                    remoteAgentId, error.getMessage()))
                            .onErrorResume(error -> Mono.just(false))
                            .flatMap(available -> {
                                if (!available) {
                                    log.info("远程智能体 {} 当前不可用，将在需要时重试连接", remoteAgentId);
                                    return Mono.empty();
                                }
                                return a2aClient.getAgentCapabilities(remoteAgentId);
                            })
                            .doOnError(error -> log.warn("获取智能体 {} 能力描述失败: {}", 
                                    remoteAgentId, error.getMessage()))
                            .onErrorResume(error -> Mono.empty())
                            .subscribe(capabilities -> {
                                if (capabilities != null && capabilities.getSkills() != null) {
                                    for (AgentCapabilities.Skill skill : capabilities.getSkills()) {
                                        String nodeName = remoteAgentId + "_" + skill.getId();
                                        remoteAgents.put(nodeName,
                                                new RemoteAgentInvoker(a2aClient, remoteAgentId, skill.getId()));
                                        log.info("已添加远程智能体节点: {} (技能: {})", remoteAgentId, skill.getId());
                                    }
                                }
                            });
                } catch (Exception e) {
                    log.warn("初始化远程智能体 {} 时发生错误，将在后续使用时重试: {}", 
                            remoteAgentId, e.getMessage());
                }
            }
        }
        
        // 保留传统的HumanInputNode
        HumanInputNode humanInputNode = new HumanInputNode();

        // 路由函数
        EdgeAction<AgentMessageState> routerAction = state -> {
            if (state.isFinished()) {
                return END;
            }

            String nextNode = "coordinator";
            Optional<String> next = state.next();
            if (next.isPresent()) {
                nextNode = next.get();
            }

            // 检查是否是远程智能体调用
            if (remoteAgents.containsKey(nextNode)) {
                return nextNode;
            }

            return switch (nextNode) {
                case "search" -> "search_agent";
                case "analysis" -> "analysis_agent";
                case "summary" -> "summary";
                case "human_input" -> "human_input";
                default -> "coordinator";
            };
        };

        // 构建工作流图
        StateGraph<AgentMessageState> graph = new StateGraph<>(AgentMessageState.SCHEMA, stateSerializer);

        // 添加本地节点
        graph.addNode("coordinator", node_async(coordinatorNode));
        graph.addNode("search_agent", node_async(searchAgentNode));
        graph.addNode("analysis_agent", node_async(analysisAgentNode));
        graph.addNode("summary", node_async(summaryNode));
        graph.addNode("human_input", node_async(humanInputNode));

        // 添加远程节点
        for (Map.Entry<String, RemoteAgentInvoker> entry : remoteAgents.entrySet()) {
            graph.addNode(entry.getKey(), node_async(entry.getValue()));
        }

        // 添加路由边
        graph.addEdge(START, "coordinator");
        graph.addConditionalEdges("coordinator", edge_async(routerAction), Map.of(
            "search_agent", "search_agent",
            "analysis_agent", "analysis_agent",
            "summary", "summary",
            "human_input", "human_input",
            END, END
        ));

        // 添加其他节点到coordinator的边
        graph.addEdge("search_agent", "coordinator");
        graph.addEdge("analysis_agent", "coordinator");
        graph.addEdge("summary", "coordinator");
        graph.addEdge("human_input", "coordinator");

        // 添加远程节点的边
        for (String nodeName : remoteAgents.keySet()) {
            graph.addEdge(nodeName, "coordinator");
        }

        // 编译图
        return graph.compile();
    }

    /**
     * 执行智能体工作流
     */
    public CompletableFuture<String> executeWorkflow(String userInput, String sessionId) {
        return executeWorkflow(userInput, sessionId, null);
    }

    /**
     * 执行智能体工作流 - 带会话历史的重载方法
     */
    public CompletableFuture<String> executeWorkflow(String userInput, String sessionId, List<Message> sessionHistory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始执行智能体工作流，会话ID: {}", sessionId);

                // 清理可能存在的旧会话状态
                cleanupSession(sessionId);

                // 准备历史消息数据 - 如果有历史记录则添加到初始状态
                Map<String, Object> initialData;
                if (sessionHistory != null && !sessionHistory.isEmpty()) {
                    initialData = Map.of(
                        "userInput", userInput,
                        "sessionId", sessionId,
                        "messages", AgentMessageState.createMessagesWithHistory(userInput, sessionHistory),
                        "sessionHistory", sessionHistory,
                        "next", "coordinator",
                        "currentAgent", "start",
                        "finished", false,
                        "toolResults", "",
                        "result", "",
                        "hasHistory", true
                    );
                    log.info("工作流中包含 {} 条历史消息", sessionHistory.size());
                } else {
                    initialData = Map.of(
                        "userInput", userInput,
                        "sessionId", sessionId,
                        "messages", AgentMessageState.createUserMessage(userInput),
                        "sessionHistory", new java.util.ArrayList<Message>(),
                        "next", "coordinator",
                        "currentAgent", "start",
                        "finished", false,
                        "toolResults", "",
                        "result", "",
                        "hasHistory", false
                    );
                }

                // 运行配置
                RunnableConfig config = RunnableConfig.builder()
                        .threadId(sessionId + "_agent_" + System.currentTimeMillis())
                        .build();

                // 执行工作流
                AsyncGenerator<NodeOutput<AgentMessageState>> stream =
                        compiledGraph.stream(initialData, config);

                StringBuilder resultBuilder = new StringBuilder();
                String finalResult = null;
                int iterationCount = 0;
                final int MAX_ITERATIONS = 25;
                AgentMessageState lastState = null;

                try {
                    for (NodeOutput<AgentMessageState> output : stream) {
                        AgentMessageState state = output.state();
                        String nodeName = output.node();
                        lastState = state;

                        log.info("正在处理智能体节点: {} (迭代次数: {})", nodeName, ++iterationCount);

                        // 记录智能体节点的执行结果
                        if (state.result().isPresent()) {
                            String result = state.result().get();
                            resultBuilder.append(result).append("\n");
                            finalResult = result;
                        }

                        // 记录工具调用结果
                        List<String> toolResults = state.toolResults();
                        if (!toolResults.isEmpty()) {
                            for (String toolResult : toolResults) {
                                log.info("节点 {} 的工具执行结果: {}", nodeName, toolResult);
                            }
                        }

                        if (state.isFinished()) {
                            log.info("智能体工作流正常完成，共执行 {} 次迭代", iterationCount);
                            break;
                        }

                        if (iterationCount >= MAX_ITERATIONS) {
                            log.warn("已达到最大迭代次数 ({})，终止工作流执行", MAX_ITERATIONS);
                            break;
                        }
                    }
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("Maximum number of iterations")) {
                        log.warn("工作流执行中检测到循环，正在优雅终止", e);
                        String forcedTermination = "智能体工作流检测到循环。已强制终止并尝试返回部分结果。\n";

                        if (resultBuilder.length() > 0) {
                            forcedTermination += "\n根据智能体处理的内容，当前结果是：\n" + resultBuilder.toString();
                        } else {
                            forcedTermination += "\n很抱歉，智能体系统无法为您的问题提供有效答案。请尝试重新表述您的问题。";
                        }
                        return forcedTermination;
                    } else {
                        throw e;
                    }
                }

                return finalResult != null ? finalResult : resultBuilder.toString().trim();

            } catch (Exception e) {
                log.error("工作流执行过程中发生错误", e);
                return "智能体工作流执行过程中发生错误: " + e.getMessage();
            }
        });
    }

    /**
     * 清理会话状态
     */
    public void cleanupSession(String sessionId) {
        sessionStates.remove(sessionId);
        log.info("已清理智能体工作流会话: {}", sessionId);
    }

    /**
     * 获取AgentNodeFactory实例，允许外部访问
     */
    public AgentNodeFactory getAgentNodeFactory() {
        return agentNodeFactory;
    }
} 