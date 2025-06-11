# Manus-langgraph

基于LangGraph4j架构的多智能体协作系统。

## 项目概述

Manus-langgraph是一个基于Java Spring Boot和LangGraph4j构建的多智能体协作系统，用于处理复杂的自然语言处理任务。该系统实现了一个智能体工作流，通过协调不同专业智能体的协作，能够高效地处理用户输入的问题，进行信息搜索、数据分析和结果总结。

## 系统架构

### 核心组件

- **工作流引擎 (WorkflowEngine)**: 系统的核心组件，负责构建和执行智能体工作流状态图，协调各智能体的交互过程。
- **协调器节点 (CoordinatorNode)**: 负责分析用户请求并决定下一步行动，路由工作流到适合的智能体节点。
- **智能体节点**: 包括SearchAgentNode、AnalysisAgentNode和SummaryNode，分别负责搜索、分析和总结功能。
- **智能体适配器 (AgentNodeAdapter)**: 将专业智能体适配为LangGraph4j的节点，支持工具调用。
- **工具集合 (ToolCollection)**: 为各专业智能体提供专用工具支持。

### 工作流程

1. 用户输入问题
2. 协调器分析请求并决定路由方向
3. 请求路由到相应的专业智能体处理
4. 结果返回给协调器进行下一步决策
5. 最终由总结智能体生成完整回复

## 技术栈

- **Java 21**: 项目的编程语言和运行环境
- **Spring Boot 3.2.3**: Web应用框架
- **Spring AI**: 用于与OpenAI等大型语言模型集成
- **LangChain4j**: Java版本的LangChain框架
- **LangGraph4j**: 基于LangChain4j的工作流和图构建工具
- **Lombok**: 简化Java代码的工具库

## 智能体介绍

### 协调器 (Coordinator)

协调器是工作流的中心节点，负责：
- 分析用户输入
- 决定下一步行动
- 根据任务性质路由到相应智能体
- 整合各智能体的执行结果

### 搜索智能体 (SearchAgent)

专注于信息检索和资料查找：
- 理解用户的搜索需求
- 使用适合的工具和策略获取信息
- 整理和组织搜索结果
- 按清晰结构返回搜索结果

### 分析智能体 (AnalysisAgent)

专注于数据分析和信息处理：
- 对数据进行深入分析和解读
- 识别数据中的模式、趋势和关联性
- 提取关键见解和重要发现
- 生成基于数据的结论和建议

### 总结智能体 (SummaryAgent)

负责生成最终输出结果：
- 整合来自各智能体的信息
- 生成简明扼要的摘要
- 提供结构化的最终回复

## 配置说明

系统配置在`application.yml`文件中管理，主要包括：
- 应用程序基本配置
- Spring AI接口设置
- OpenAI API设置
- Manus系统参数设置
- 日志级别配置

### 关键配置项

```yaml
manus:
  workspace-root: ${user.dir}
  max-steps: 20
  max-observe: 10000
  duplicate-threshold: 2
```

## 使用方法

### 环境准备

1. 安装JDK 21或更高版本
2. 配置Maven
3. 在application.yml中设置OpenAI API密钥和基本URL

### 构建和运行

```bash
# 构建项目
mvn clean package

# 运行项目
java -jar target/Manus-langgraph-1.0-SNAPSHOT.jar
```

### 交互方式

系统启动后，用户可在终端中输入问题与系统交互：
1. 输入问题或指令
2. 系统进行处理（可能涉及多个智能体的协作）
3. 显示最终结果

## 可扩展性

系统设计具有高度可扩展性：
- 支持添加新的智能体类型
- 可定制工具集合
- 可以通过AgentNodeFactory创建新的智能体节点

要添加新智能体:
1. 创建智能体实现类
2. 创建对应的节点适配器
3. 在AgentNodeFactory中注册新智能体
4. 更新工作流图定义

## 状态管理

系统使用AgentMessageState管理智能体间的状态传递，包括：
- 用户输入
- 消息历史
- 智能体执行结果
- 路由决策信息

## 贡献指南

欢迎通过以下方式贡献代码：
1. Fork仓库
2. 创建特性分支 (git checkout -b feature/your-feature)
3. 提交更改 (git commit -m 'Add some feature')
4. 推送到分支 (git push origin feature/your-feature)
5. 创建Pull Request

## 许可证

[待添加]

## 联系方式

[待添加]
