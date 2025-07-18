package com.my.qwe.task;

import java.io.IOException;

public class DutuTask implements ITask{
    @Override
    public void start(TaskContext context, TaskThread thread) {
        CommonActions commonActions = new CommonActions(context,thread);


    }

    @Override
    public String getName() {
        return "读图";
    }
}
