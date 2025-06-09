package com.zhouruojun.manus.model;

/**
 * 工作流下一步动作的枚举类
 * 用于规范化工作流中的状态转换
 */
public enum NextAction {
    SEARCH("search"),
    ANALYSIS("analysis"),
    SUMMARY("summary"),
    HUMAN_INPUT("human_input"),
    FINISH("FINISH");

    private final String value;

    NextAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 从字符串获取对应的枚举值
     * @param value 动作的字符串表示
     * @return 对应的枚举值，如果没有匹配则返回FINISH
     */
    public static NextAction fromString(String value) {
        if (value == null) {
            return FINISH;
        }

        String normalized = value.trim().toLowerCase();

        for (NextAction action : NextAction.values()) {
            if (action.getValue().equalsIgnoreCase(normalized)) {
                return action;
            }
        }

        // 兼容中文表示
        if (normalized.contains("搜索")) {
            return SEARCH;
        } else if (normalized.contains("分析")) {
            return ANALYSIS;
        } else if (normalized.contains("总结")) {
            return SUMMARY;
        } else if (normalized.contains("用户输入")) {
            return HUMAN_INPUT;
        } else if (normalized.contains("完成")) {
            return FINISH;
        }

        return FINISH; // 默认返回FINISH
    }
}
