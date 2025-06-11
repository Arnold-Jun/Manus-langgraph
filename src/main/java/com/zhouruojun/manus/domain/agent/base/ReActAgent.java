package com.zhouruojun.manus.domain.agent.base;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * ReActAgent是BaseAgent的子类，实现了ReAct（Reasoning and Acting）模式
 * 该模式将每一个推理步骤分为思考(think)和行动(act)两个阶段
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Slf4j
@SuperBuilder
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态并决定下一步操作
     * @return 是否需要执行行动
     */
    public abstract CompletableFuture<Boolean> think();

    /**
     * 执行决定的行动
     * @return 行动的结果
     */
    public abstract CompletableFuture<String> act();

    /**
     * 执行单个步骤：思考和行动
     * 重写BaseAgent的step方法，实现ReAct模式
     */
    @Override
    public CompletableFuture<String> step() {
        return think().thenCompose(shouldAct -> {
            if (!shouldAct) {
                return CompletableFuture.completedFuture("思考完成 - 无需行动");
            }
            return act();
        });
    }
}
