package com.zhouruojun.manus.core;

import com.zhouruojun.manus.model.AgentMessageState;
import com.zhouruojun.manus.core.nodes.*;
import com.zhouruojun.manus.model.Message;
import com.zhouruojun.manus.serializers.AgentSerializers;
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
 * 基于LangGraph4j的工作流引擎
 * 使用真正的StateGraph来构建多智能体协作工作流
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private ChatModel chatModel;
    private MemorySaver checkpointSaver;
    private CompiledGraph<AgentMessageState> compiledGraph;
    private Map<String, AgentMessageState> sessionStates;
    private StateSerializer<AgentMessageState> stateSerializer;

    // 删除旧的构造函数，用这一个替代
    @Autowired
    public WorkflowEngine(ChatModel chatModel,
                          @Autowired(required = false)
                                 StateSerializer<AgentMessageState> serializer) {
        this.chatModel = chatModel;
        this.checkpointSaver = new MemorySaver();
        this.sessionStates = new ConcurrentHashMap<>();

        // 初始化StateSerializer
        this.stateSerializer = (serializer != null) ? serializer : AgentSerializers.STD.object();

        try {
            this.compiledGraph = buildWorkflow();
        } catch (GraphStateException e) {
            log.error("Failed to build workflow", e);
            throw new RuntimeException("Failed to initialize workflow engine", e);
        }
    }


    /**
     * 构建工作流状态图
     */
    private CompiledGraph<AgentMessageState> buildWorkflow() throws GraphStateException {
        // 创建节点实例
        CoordinatorNode coordinatorNode = new CoordinatorNode(chatModel);
        SearchNode searchNode = new SearchNode(chatModel);
        AnalysisNode analysisNode = new AnalysisNode(chatModel);
        SummaryNode summaryNode = new SummaryNode(chatModel);
        HumanInputNode humanInputNode = new HumanInputNode();

        // 路由函数
        EdgeAction<AgentMessageState> coordinatorRouter = (AgentMessageState state) -> {
            String next = state.next().orElse("summary");
            // 确保next值不为空，如果为空或为空字符串，默认为summary
            if (next == null || next.isEmpty()) {
                next = "summary";
            }
            log.info("Coordinator routing to: {}", next);

            // 检查next是否在条件边映射中存在
            if (!Map.of("search", "search", "analysis", "analysis", "summary", "summary",
                       "human_input", "human_input").containsKey(next)) {
                log.warn("无效的路由目标: {}，默认转为summary", next);
                next = "summary";
            }

            return next;
        };

        // 构建状态图 - 使用Map作为状态类型，因为LangGraph4j可能不支持直接的状态类
        StateGraph<AgentMessageState> stateGraph = new StateGraph<>(AgentMessageState.SCHEMA, stateSerializer)
                .addNode("coordinator", node_async(coordinatorNode))
                .addNode("search", node_async(searchNode))
                .addNode("analysis", node_async(analysisNode))
                .addNode("summary", node_async(summaryNode))
                .addNode("human_input", node_async(humanInputNode))

                // 从开始节点到协调器
                .addEdge(START, "coordinator")

                // 协调器的条件边
                .addConditionalEdges("coordinator",
                    edge_async(coordinatorRouter),
                    Map.of(
                        "search", "search",
                        "analysis", "analysis",
                        "summary", "summary",
                        "human_input", "human_input"
                    ))

                // 各专业智能体完成后回到协调器
                .addEdge("search", "coordinator")
                .addEdge("analysis", "coordinator")
                .addEdge("human_input", "coordinator")
                
                // summary节点直接连接到END，作为最终输出
                .addEdge("summary", END)
                ;

        // 编译配置
        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .build();

        return stateGraph.compile(compileConfig);
    }

    /**
     * 执行工作流
     */
    public CompletableFuture<String> executeWorkflow(String userInput, String sessionId) {
        return executeWorkflow(userInput, sessionId, null);
    }

    /**
     * 执行工作流 - 带会话历史的重载方法
     */
    public CompletableFuture<String> executeWorkflow(String userInput, String sessionId, List<Message> sessionHistory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting workflow execution for session: {}", sessionId);

                // 清理可能存在的旧会话状态
                cleanupSession(sessionId);

                // 准备历史消息数据 - 如果有历史记录则添加到初始状态
                Map<String, Object> initialData;
                if (sessionHistory != null && !sessionHistory.isEmpty()) {
                    // 创建初始状态数据 - 包含历史消息
                    initialData = Map.of(
                        "userInput", userInput,
                        "sessionId", sessionId,
                        "messages", AgentMessageState.createMessagesWithHistory(userInput, sessionHistory),
                        "sessionHistory", sessionHistory,  // 添加历史记录作为独立字段
                        "next", "coordinator", // 确保有明确的初始目标节点
                        "currentAgent", "start", // 添加当前代理字段
                        "finished", false, // 明确设置为未完成
                        "toolResults", "", // 清空工具结果
                        "result", "", // 清空之前的结果
                        "hasHistory", true // 标记有历史记录
                    );
                    log.info("Including {} history messages in workflow", sessionHistory.size());
                } else {
                    // 创建初始状态数据 - 无历史消息
                    initialData = Map.of(
                        "userInput", userInput,
                        "sessionId", sessionId,
                        "messages", AgentMessageState.createUserMessage(userInput),
                        "sessionHistory", new java.util.ArrayList<Message>(),  // 空的历史记录列表
                        "next", "coordinator", // 确保有明确的初始目标节点
                        "currentAgent", "start", // 添加当前代理字段
                        "finished", false, // 明确设置为未完成
                        "toolResults", "", // 清空工具结果
                        "result", "", // 清空之前的结果
                        "hasHistory", false // 标记无历史记录
                    );
                }

                // 运行配置 - 使用新的线程ID确保状态隔离
                RunnableConfig config = RunnableConfig.builder()
                        .threadId(sessionId + "_" + System.currentTimeMillis()) // 添加时间戳确保唯一性
                        .build();

                // 执行工作流
                AsyncGenerator<NodeOutput<AgentMessageState>> stream =
                        compiledGraph.stream(initialData, config);

                StringBuilder resultBuilder = new StringBuilder();
                String finalResult = null;
                int iterationCount = 0;
                final int MAX_ITERATIONS = 25; // 设置最大迭代次数
                AgentMessageState lastState = null;

                try {
                    for (NodeOutput<AgentMessageState> output : stream) {
                        AgentMessageState state = output.state();
                        String nodeName = output.node();
                        lastState = state; // 保存最后的状态

                        log.info("Processing node: {} (iteration: {})", nodeName, ++iterationCount);

                        if (state.result().isPresent()) {
                            String result = state.result().get();
                            resultBuilder.append(result).append("\n");
                            finalResult = result;
                        }

                        if (state.isFinished()) {
                            log.info("Workflow finished normally after {} iterations", iterationCount);
                            break;
                        }

                        // 检查是否达到最大迭代次数
                        if (iterationCount >= MAX_ITERATIONS) {
                            log.warn("Reached maximum number of iterations ({}). Terminating workflow.", MAX_ITERATIONS);

                            // 如果有协调器节点，可以尝试请求一个最终总结
                            if (lastState != null) {
                                String forcedSummary = "由于工作流迭代次数过多，系统已自动终止。基于目前已收集的信息，我会尝试为您提供一个回答：\n\n" +
                                                     "您的问题是：" + userInput + "\n\n" +
                                                     "根据已处理的内容，最终结论是：";

                                // 如果有历史消息，可以尝试进行一次最终总结
                                if (!lastState.messages().isEmpty()) {
                                    forcedSummary += "\n\n[系统已强制终止工作流，此为根据已收集信息的初步回答]";
                                    resultBuilder.append(forcedSummary);
                                    finalResult = forcedSummary;
                                }
                            }
                            break;
                        }
                    }
                } catch (IllegalStateException e) {
                    // 捕获迭代次数超限异常
                    if (e.getMessage().contains("Maximum number of iterations")) {
                        log.warn("Caught max iterations exception. Terminating workflow gracefully.", e);
                        String forcedTermination = "系统检测到工作流陷入循环。已强制终止并尝试返回部分结果。\n";

                        if (resultBuilder.length() > 0) {
                            // 已有一些结果，返回这些结果
                            forcedTermination += "\n根据已处理的内容，当前结果是：\n" + resultBuilder.toString();
                        } else {
                            // 没有结果，返回通用消息
                            forcedTermination += "\n很抱歉，系统无法为您的问题提供有效答案。请尝试重新表述您的问题，或者分解为多个简单问题。";
                        }
                        return forcedTermination;
                    } else {
                        throw e; // 重新抛出其他异常
                    }
                }

                return finalResult != null ? finalResult : resultBuilder.toString().trim();

            } catch (Exception e) {
                log.error("Error executing workflow", e);
                return "执行过程中发生错误: " + e.getMessage();
            }
        });
    }

    /**
     * 清理会话状态
     */
    public void cleanupSession(String sessionId) {
        sessionStates.remove(sessionId);
        log.info("Cleaned up session: {}", sessionId);
    }
}
