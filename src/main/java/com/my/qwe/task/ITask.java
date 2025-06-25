package com.my.qwe.task;

public interface ITask {
    void execute(TaskContext context) throws Exception;

    default void stop() {
        // 可选：被强制停止时回调
    }

    default void pause() {
        // 可选：暂停任务
    }

    default void resume() {
        // 可选：恢复任务
    }
}
