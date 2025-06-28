package com.my.qwe.task;

public interface ITask {
    void execute(TaskContext context) throws Exception;

    void init(TaskContext context) throws Exception;

    /**
     * 执行单步任务，返回true表示任务完成
     */
    boolean executeStep(TaskContext context) throws Exception;

    void cleanup(TaskContext context) throws Exception;
}
