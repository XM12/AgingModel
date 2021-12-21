package com.lenovo.agingmodel.agingtask;

import android.util.Log;

import com.lenovo.agingmodel.utils.FileUtil;

import java.util.concurrent.atomic.AtomicLong;

public class CopyTask implements Runnable {
    private static final String TAG = "CopyTask";

    private String sourcePath;
    private String targetPath;
    private AtomicLong taskCounter;
    private TaskProgressListener listener;

    public CopyTask(String sourcePath, String targetPath, AtomicLong taskCounter) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.taskCounter = taskCounter;
        this.taskCounter.incrementAndGet();
    }

    @Override
    public void run() {
//        Log.d(TAG, "soucePath = " + sourcePath + ", targetPath = " + targetPath);
        FileUtil.getInstance().doCopyLocalFile(sourcePath, targetPath);
        //任务完成，更新任务数量
        long undoneCount = 0;
        if (taskCounter != null) {
            undoneCount = taskCounter.decrementAndGet();
        }
        if (listener != null) {
            listener.taskProgress(undoneCount);
        }
    }

    public interface TaskProgressListener {
        void taskProgress(long undoneCount);
    }

    public void setTaskProgressListener(TaskProgressListener listener) {
        this.listener = listener;
    }
}
