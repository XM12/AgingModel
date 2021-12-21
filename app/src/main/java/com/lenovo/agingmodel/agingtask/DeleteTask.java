package com.lenovo.agingmodel.agingtask;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DeleteTask implements Runnable {
    private static final String TAG = "DeleteTask";
    private final AtomicLong taskCounter;
    private final AtomicBoolean executingTaskTag;
    private final File dir;
    private TaskProgressListener listener;

    public DeleteTask(AtomicLong taskCounter, AtomicBoolean executingTaskTag, File dir) {
        this.taskCounter = taskCounter;
        this.executingTaskTag = executingTaskTag;
        this.dir = dir;
//        Log.i(TAG, "delete task dir path = " + dir.getAbsolutePath());
    }

    @Override
    public void run() {
        deleteFile(dir, 0);
        //更新任务数量
        if (listener != null) {
            listener.taskProgress(taskCounter.decrementAndGet());
        }
    }

    //删除单个文件夹下的文件
    private long deleteFile(File dir, long startIndex) {
        final int MAX_FILES = 10000;
        if (executingTaskTag.get()) {
            File[] files = dir.listFiles(new FileFilter() {
                int index = -1;

                @Override
                public boolean accept(File pathname) {
                    index++;
                    return index % 2 == 0
                            && startIndex <= index
                            && index < startIndex + MAX_FILES;
                }
            });
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (executingTaskTag.get()) {
                        boolean delete = file.delete();
                        if (!delete) {
                            Log.i(TAG, "deleteFile: dir path = " + dir.getAbsolutePath() + ", delete file = " + delete);
                        }
                    } else {
                        return 0;
                    }
                }
                return deleteFile(dir, startIndex + MAX_FILES / 2);
            } else {
                if (isEmptyDir(dir)) {
                    Log.i(TAG, "deleteFile: isEmptyDir true");
                }
                return 0;
            }
        } else {
            return 0;
        }
    }

    //检查文件夹是否存在文件
    private boolean isEmptyDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles(new FileFilter() {
                int index = 0;

                @Override
                public boolean accept(File pathname) {
                    index++;
                    return index == 1;
                }
            });
            return files == null || files.length == 0;
        }
        return false;
    }

    public interface TaskProgressListener {
        void taskProgress(long undoneCount);
    }

    public void setTaskProgressListener(TaskProgressListener listener) {
        this.listener = listener;
    }
}
