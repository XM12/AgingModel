package com.lenovo.agingmodel.executor;

public interface TaskListener {

    void startTask();

    void taskInProgress(int progress);

    void completeTask();
}
