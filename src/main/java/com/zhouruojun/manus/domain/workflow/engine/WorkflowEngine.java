package com.zhouruojun.manus.domain.workflow.engine;

import com.zhouruojun.manus.domain.workflow.node.AgentNodeFactory;
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

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

    private ChatModel chatModel;
    private MemorySaver checkpointSaver;
    private CompiledGraph<AgentMessageState> compiledGraph;
    private Map<String, AgentMessageState> sessionStates;
    private StateSerializer<AgentMessageState> stateSerializer;
    private AgentNodeFactory agentNodeFactory;
    private PromptLoader promptLoader;
    private PromptConfig promptConfig;

    @Autowired
    public WorkflowEngine(ChatModel chatModel,
                          AgentNodeFactory agentNodeFactory,
                          PromptLoader promptLoader,
                          PromptConfig promptConfig,
                          @Autowired(required = false)
                               StateSerializer<AgentMessageState> serializer) {
        this.chatModel = chatModel;
        this.agentNodeFactory = agentNodeFactory;
        this.promptLoader = promptLoader;
        this.promptConfig = promptConfig;
        this.checkpointSaver = new MemorySaver();
        this.sessionStates = new ConcurrentHashMap<>();

        // 初始化StateSerializer
        this.stateSerializer = (serializer != null) ? serializer : AgentSerializers.STD.object();

        try {
            this.compiledGraph = buildAgentWorkflow();
        } catch (GraphStateException e) {
            log.error("工作流构建失败", e);
            throw new RuntimeException("智能体工作流引擎初始化失败", e);
        }
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
        
        // 保留传统的HumanInputNode
        HumanInputNode humanInputNode = new HumanInputNode();

        // 路由函数
        EdgeAction<AgentMessageState> coordinatorRouter = (AgentMessageState state) -> {
            String next = state.next().orElse("summary_agent");
            // 确保next值不为空，如果为空或为空字符串，默认为summary_agent
            if (next == null || next.isEmpty()) {
                next = "summary_agent";
            }
            log.info("Coordinator routing to: {}", next);

            // 映射路由目标到智能体节点
            Map<String, String> routeMapping = Map.of(
                "search", "search_agent",
                "analysis", "analysis_agent",
                "summary", "summary_agent",
                "human_input", "human_input"
            );

            String targetNode = routeMapping.getOrDefault(next, "summary_agent");
            
            // 检查映射后的节点是否有效
            if (!Map.of("search_agent", "search_agent", 
                       "analysis_agent", "analysis_agent", 
                       "summary_agent", "summary_agent",
                       "human_input", "human_input").containsKey(targetNode)) {
                log.warn("无效的路由目标: {}，默认转为summary_agent", targetNode);
                targetNode = "summary_agent";
            }

            log.info("Final routing target: {}", targetNode);
            return targetNode;
        };

        // 构建状态图
        StateGraph<AgentMessageState> stateGraph = new StateGraph<>(AgentMessageState.SCHEMA, stateSerializer)
                .addNode("coordinator", node_async(coordinatorNode))
                .addNode("search_agent", node_async(searchAgentNode))
                .addNode("analysis_agent", node_async(analysisAgentNode))
                .addNode("summary_agent", node_async(summaryNode))
                .addNode("human_input", node_async(humanInputNode))

                // 从开始节点到协调器
                .addEdge(START, "coordinator")

                // 协调器的条件边
                .addConditionalEdges("coordinator",
                    edge_async(coordinatorRouter),
                    Map.of(
                        "search_agent", "search_agent",
                        "analysis_agent", "analysis_agent",
                        "summary_agent", "summary_agent",
                        "human_input", "human_input"
                    ))

                // 各专业智能体完成后回到协调器
                .addEdge("search_agent", "coordinator")
                .addEdge("analysis_agent", "coordinator")
                .addEdge("human_input", "coordinator")
                
                // summary_agent节点直接连接到END，作为最终输出
                .addEdge("summary_agent", END)
                ;

        // 编译配置
        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .build();

        return stateGraph.compile(compileConfig);
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
                        if (state.toolResults().isPresent()) {
                            String toolResults = state.toolResults().get();
                            log.info("节点 {} 的工具执行结果: {}", nodeName, toolResults);
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