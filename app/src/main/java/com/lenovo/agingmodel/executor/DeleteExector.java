package com.lenovo.agingmodel.executor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.lenovo.agingmodel.agingtask.DeleteTask;
import com.lenovo.agingmodel.utils.ThreadPoolUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DeleteExector extends BaseExecutor implements DeleteTask.TaskProgressListener {
    private static final String TAG = "DeleteExector";
    private ThreadPoolUtil poolUtil;
    private ThreadPoolExecutor deleteExecutor;
    private AtomicLong taskCounter;
    private List<String> emptyDirs;
    private AtomicBoolean executingTaskTag;

    public DeleteExector(Context context, String fileType) {
        super(context, fileType);
        poolUtil = ThreadPoolUtil.getInstance();
        emptyDirs = new ArrayList<>();
    }

    @Override
    public void deleteTask() {
        super.deleteTask();
        new Thread(new Runnable() {
            @Override
            public void run() {
                isExecutingTask = true;
                executingTaskTag = new AtomicBoolean();
                executingTaskTag.set(isExecutingTask);
                emptyDirs.clear();
                deleteExecutor = poolUtil.getDeleteExecutor();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.startTask();
                        }
                        taskCounter = new AtomicLong();
                        delete(getDir(), 0);
                    }
                }).start();
            }
        }).start();
    }

    //删除任务
    private long delete(File targetFile, long startIndex) {
        if (targetFile == null) {
            if (listener != null) {
                listener.completeTask();
            }
            return 0;
        }
        if (isExecutingTask) {
            final int MAX_DIRS = 1000;
            //防止文件过多导致溢出，每次获取1000个文件进行处理
            File[] dirs = targetFile.listFiles(new FileFilter() {
                int index = -1;

                @Override
                public boolean accept(File pathname) {
                    index++;
                    return startIndex <= index
                            && index < startIndex + MAX_DIRS;
                }
            });
            if (dirs != null && dirs.length > 0) {
//                fileNumber(targetFile);
                for (int i = 0; i < dirs.length; i++) {
                    taskCounter.incrementAndGet();
                    File dir = dirs[i];
                    if (isExecutingTask) {
                        if (isEmptyDir(dir) && !emptyDirs.contains(dir.getAbsolutePath())) {
                            emptyDirs.add(dir.getAbsolutePath());
                            taskCounter.decrementAndGet();
                            continue;
                        }
                        BlockingQueue<Runnable> queue = deleteExecutor.getQueue();
                        if (queue.size() < ThreadPoolUtil.MAX_QUEUE) {
                            //一个文件夹对应一个删除任务
                            DeleteTask deleteTask = new DeleteTask(taskCounter, executingTaskTag, dir);
                            deleteTask.setTaskProgressListener(DeleteExector.this);
                            if (!deleteExecutor.isShutdown()) {
                                deleteExecutor.execute(deleteTask);
                            }
                        } else {
                            i--;
                            SystemClock.sleep(50);
                        }
                    } else {
                        return 0;
                    }
                }
                return delete(targetFile, startIndex + MAX_DIRS);
            } else {
                if (taskCounter.get() == 0 && listener != null) {
                    deleteEmptyDir();
                    if (listener != null) {
                        listener.completeTask();
                    }
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

    private void deleteEmptyDir() {
        if (emptyDirs.size() > 0) {
            for (String dirPath : emptyDirs) {
                File dir = new File(dirPath);
                boolean delete = dir.delete();
                Log.i(TAG, "deleteEmptyDir: dir path = " + dirPath + ", " + delete);
            }
        }
    }

    private void fileNumber(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            Log.e(TAG, "fileNumber: " + file.getName() + ", " + file.listFiles().length);
        }
    }

    @Override
    public void taskProgress(long undoneCount) {
        if (undoneCount == 0) {
            Log.e(TAG, "taskProgress: undoneCount = 0");
            isExecutingTask = false;
            executingTaskTag.set(false);
            deleteEmptyDir();
            if (listener != null) {
                listener.completeTask();
            }
        }
    }

    @Override
    public void destroyTask() {
        isExecutingTask = false;
        executingTaskTag.set(false);
        taskCounter.set(0);
        deleteEmptyDir();
        if (poolUtil != null && !deleteExecutor.isShutdown()) {
            deleteExecutor.shutdownNow();
        }
    }
}
