package com.my.qwe.task;

public interface ITask {
    /**
     * 启动任务执行逻辑（由线程调用）
     * @param context 任务上下文，包含配置信息等
     * @param thread 当前任务线程，用于支持暂停与停止控制
     */
    void start(TaskContext context, TaskThread thread);

    /**
     * 获取任务名称（用于 UI 展示）
     */
    String getName();
}
