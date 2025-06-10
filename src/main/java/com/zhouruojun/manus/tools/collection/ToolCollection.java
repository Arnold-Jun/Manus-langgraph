package com.zhouruojun.manus.tools.collection;

import lombok.Getter;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * 工具集合类，用于扫描并注册工具
 */
@Component
public class ToolCollection {
    private static final Logger log = Logger.getLogger(ToolCollection.class.getName());

    /**
     * 工具回调列表
     */
    @Getter
    private final List<ToolCallback> toolCallbacks = new ArrayList<>();

    /**
     * 是否启用内部工具执行
     */
    private final boolean internalToolExecutionEnabled;

    /**
     * 是否自动扫描并注册工具
     */
    private final boolean autoScanTools;

    /**
     * 默认构造函数
     */
    public ToolCollection() {
        this(false, true);
    }

    /**
     * 带内部工具执行选项的构造函数
     * @param internalToolExecutionEnabled 是否启用内部工具执行
     */
    public ToolCollection(boolean internalToolExecutionEnabled) {
        this(internalToolExecutionEnabled, true);
    }

    /**
     * 带内部工具执行选项和自动扫描控制的构造函数
     * @param internalToolExecutionEnabled 是否启用内部工具执行
     * @param autoScanTools 是否自动扫描并注册工具
     */
    public ToolCollection(boolean internalToolExecutionEnabled, boolean autoScanTools) {
        this.internalToolExecutionEnabled = internalToolExecutionEnabled;
        this.autoScanTools = autoScanTools;

        // 只有当autoScanTools为true时才扫描并注册工具
        if (autoScanTools) {
            scanAndRegisterTools();
        }
    }

    /**
     * 带初始工具回调的构造函数
     * @param callbacks 初始工具回调列表
     */
    public ToolCollection(ToolCallback... callbacks) {
        this(false, true, callbacks);
    }

    /**
     * 带内部工具执行选项和初始工具回调的构造函数
     * @param internalToolExecutionEnabled 是否启用内部工具执行
     * @param callbacks 初始工具回调列表
     */
    public ToolCollection(boolean internalToolExecutionEnabled, ToolCallback... callbacks) {
        this(internalToolExecutionEnabled, true, callbacks);
    }

    /**
     * 完整的构造函数，包含所有可配置选项
     * @param internalToolExecutionEnabled 是否启用内部工具执行
     * @param autoScanTools 是否自动扫描并注册工具
     * @param callbacks 初始工具回调列表
     */
    public ToolCollection(boolean internalToolExecutionEnabled, boolean autoScanTools, ToolCallback... callbacks) {
        this.internalToolExecutionEnabled = internalToolExecutionEnabled;
        this.autoScanTools = autoScanTools;

        // 添加初始工具回调
        if (callbacks != null) {
            this.toolCallbacks.addAll(Arrays.asList(callbacks));
        }

        // 只有当autoScanTools为true时才扫描并注册工具
        if (autoScanTools) {
            scanAndRegisterTools();
        }
    }

    /**
     * 扫描并注册工具
     */
    private void scanAndRegisterTools() {
        try {
            // 指定要扫描的包路径
            String packageName = "com.Manus.tool.impl";

            // 获取类加载器
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // 获取class文件所在目录
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            int toolCount = 0;

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                // 遍历目录
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    // 获取目录中的所有.class文件
                    File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".class"));

                    if (files != null) {
                        for (File file : files) {
                            // 转换文件名为类名
                            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);

                            try {
                                // 加载类
                                Class<?> clazz = Class.forName(className);

                                // 检查是否实现了Function接口且不是接口或抽象类
                                if (Function.class.isAssignableFrom(clazz) &&
                                    !clazz.isInterface() &&
                                    !Modifier.isAbstract(clazz.getModifiers())) {

                                    // 注册工具
                                    if (registerFunctionTool(clazz)) {
                                        toolCount++;
                                    }
                                }
                                else {
                                    // 直接累加registerMethodTool返回的方法工具数量
                                    toolCount += registerMethodTool(clazz);
                                }
                            } catch (Exception e) {
                                log.warning("加载类 " + className + " 时出错: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            log.info("成功注册了 " + toolCount + " 个工具");

        } catch (Exception e) {
            log.warning("扫描工具时出错: " + e.getMessage());
        }
    }

    /**
     * 函数类注册单个工具类
     * @param toolClass 工具类
     * @return 是否成功注册
     */
    private boolean registerFunctionTool(Class<?> toolClass) {
        try {
            // 创建工具实例
            Object toolInstance = toolClass.getDeclaredConstructor().newInstance();

            if (!(toolInstance instanceof Function)) {
                return false;
            }

            // 获取工具名称
            String toolName = getToolName(toolClass);

            // 获取工具描述
            String toolDescription = getToolDescription(toolClass);

            // 查找输入类型
            Class<?> inputType = findInputType(toolClass);
            if (inputType == null) {
                log.warning("无法确定工具 " + toolClass.getName() + " 的输入类型");
                return false;
            }

            // 创建工具回调
            ToolCallback callback = FunctionToolCallback.builder(
                            toolName,
                            (Function<?, ?>) toolInstance)
                    .description(toolDescription)
                    .inputType(inputType)
                    .build();

            // 添加到工具列表
            toolCallbacks.add(callback);

            log.info("已注册工具: " + toolName + " (" + toolDescription + ")");
            return true;

        } catch (Exception e) {
            log.warning("注册工具 " + toolClass.getName() + " 时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 函数类方法注册单个工具类
     * @param toolClass 工具类
     * @return 是否成功注册
     */
    private int registerMethodTool(Class<?> toolClass) {
        try {
            // 创建工具类实例
            Object toolInstance = toolClass.getDeclaredConstructor().newInstance();

            // 获取该类的所有公共方法
            Method[] methods = toolClass.getDeclaredMethods();
            int registeredAnyMethod = 0;

            for (Method method : methods) {
                // 检查方法是否应该被注册为工具
                // 这里可以根据需要添加条件，例如注解检查或命名约定
                if (Modifier.isPublic(method.getModifiers()) &&
                        Modifier.isStatic(method.getModifiers())) {

                    // 获取工具名称（使用方法名）

                    String toolName = method.getName();

                    // 获取工具描述（可以从注释或其他地方获取）
                    Tool toolAnno = AnnotationUtils.findAnnotation(method, Tool.class);
                    String toolDescription = toolAnno.description();

                    // 创建工具定义
                    ToolDefinition toolDefinition =
                            ToolDefinition.builder()
                                    .name(toolName)
                                    .description(toolDescription)
                                    .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                                    .build();

                    // 创建方法工具回调
                    MethodToolCallback callback =
                            MethodToolCallback.builder()
                                    .toolDefinition(toolDefinition)
                                    .toolMethod(method)
                                    .build();

                    // 添加到工具列表
                    toolCallbacks.add(callback);
                    registeredAnyMethod += 1;

                    log.info("已注册方法工具: " + toolName + " (" + toolDescription + ")");
                }
            }

            return registeredAnyMethod;

        } catch (Exception e) {
            log.warning("注册方法工具类 " + toolClass.getName() + " 时出错: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取工具名称
     * @param toolClass 工具类
     * @return 工具名称
     */
    private String getToolName(Class<?> toolClass) {
        try {
            // 尝试查找NAME字段
            java.lang.reflect.Field nameField = toolClass.getDeclaredField("NAME");
            nameField.setAccessible(true);
            Object name = nameField.get(null);
            if (name instanceof String) {
                return (String) name;
            }
        } catch (Exception ignored) {
            // 忽略异常
        }

        // 使用类名转换为驼峰命名
        String className = toolClass.getSimpleName();
        if (className.endsWith("Tool")) {
            className = className.substring(0, className.length() - 4);
        }

        // 首字母小写
        if (className.length() > 0) {
            return Character.toLowerCase(className.charAt(0)) + className.substring(1);
        }

        return className;
    }

    /**
     * 获取工具描述
     * @param toolClass 工具类
     * @return 工具描述
     */
    private String getToolDescription(Class<?> toolClass) {
        try {
            // 尝试查找DESCRIPTION字段
            java.lang.reflect.Field descField = toolClass.getDeclaredField("DESCRIPTION");
            descField.setAccessible(true);
            Object desc = descField.get(null);
            if (desc instanceof String) {
                return (String) desc;
            }
        } catch (Exception ignored) {
            // 忽略异常
        }

        return "Tool for " + toolClass.getSimpleName();
    }

    /**
     * 查找工具输入类型
     * @param toolClass 工具类
     * @return 输入类型
     */
    private Class<?> findInputType(Class<?> toolClass) {
        // 1. 首先从内部类中查找Request记录类
        for (Class<?> innerClass : toolClass.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("Request") ||
                    innerClass.getSimpleName().endsWith("Request")) {
                return innerClass;
            }
        }

        // 2. 尝试获取Function接口的泛型参数
        try {
            for (java.lang.reflect.Type genericInterface : toolClass.getGenericInterfaces()) {
                if (genericInterface instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericInterface;
                    if (paramType.getRawType().equals(Function.class)) {
                        java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            return (Class<?>) typeArgs[0];
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略异常
        }

        return null;
    }

    /**
     * 手动添加工具回调
     * @param callback 工具回调
     * @return 当前工具集合实例，用于链式调用
     */
    public ToolCollection addToolCallback(ToolCallback callback) {
        if (callback != null) {
            toolCallbacks.add(callback);
        }
        return this;
    }

    /**
     * 手动添加工具
     * @param name 工具名称
     * @param description 工具描述
     * @param tool 工具函数
     * @param inputType 输入类型
     * @return 当前工具集合实例，用于链式调用
     */
    public <I, O> ToolCollection addTool(String name, String description, Function<I, O> tool, Class<I> inputType) {
        ToolCallback callback = FunctionToolCallback.builder(name, tool)
                .description(description)
                .inputType(inputType)
                .build();

        toolCallbacks.add(callback);
        return this;
    }

    /**
     * 获取所有工具回调的数组
     * @return 工具回调数组
     */
    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks.toArray(new ToolCallback[0]);
    }

    /**
     * 创建包含所有工具的ChatOptions
     * @return 包含工具的ChatOptions
     */
    public ChatOptions toChatOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(getToolCallbacks())
                .internalToolExecutionEnabled(internalToolExecutionEnabled)
                .build();
    }
}

