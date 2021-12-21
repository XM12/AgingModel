package com.lenovo.agingmodel.executor;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.lenovo.agingmodel.agingtask.CopyTask;
import com.lenovo.agingmodel.utils.StorageSpaceManager;
import com.lenovo.agingmodel.utils.ThreadPoolUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Andorid 文件夹下有最大文件数量限制
 * <p>
 * 解决方案：每个文件夹复制大约10GB文件，就另新建文件夹进行复制
 */
public class KbExecutor extends BaseExecutor implements CopyTask.TaskProgressListener {
    private static final String TAG = "KbExecutor";
    private ThreadPoolUtil poolUtil;
    private Timer timer;
    private final long TIME_UPDATE_SPACE = 1000;
    private TimerTask timerTask;
    //任务开始时空闲空间
    private long freeSize = 0;
    //任务进行中空闲空间
    private long currentFreeSize = 0;


    public KbExecutor(Context context, String fileType) {
        super(context, fileType);
        poolUtil = ThreadPoolUtil.getInstance();
    }

    private void updateStorageSpace() {
        freeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(context)[1];
        currentFreeSize = freeSize;
        if (timer != null) {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                currentFreeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(context)[1];
                if (listener != null) {
                    //更新任务进度
                    if (undoneTaskCount < 0) {
                        undoneTaskCount = 0;
                    }
                    int progress = (int) (StorageSpaceManager.getInstance().div((double) (totalTaskCount - undoneTaskCount), (double) (totalTaskCount), 2) * 100);
                    listener.taskInProgress(progress);
                }
            }
        };
        timer.schedule(timerTask, 1000, TIME_UPDATE_SPACE);
    }

    @Override
    public void copyTask(String sourcePath, String targetPath, long spaceOrNumber) {
        if (poolUtil != null) {
            isExecutingTask = true;
            //任务总数
            totalTaskCount = spaceOrNumber;
            undoneTaskCount = totalTaskCount;
            if (listener != null) {
                listener.startTask();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateStorageSpace();
                    //初始化任务计数器，主要记录线程池不同状态的任务数量
                    AtomicLong taskCounter = new AtomicLong();
                    String targetChildPath = createNewDir(targetPath);
                    //新建文件夹基准值
                    long newFolderReferenceValue = BaseExecutor.STORAGE_SPACE_500M;
                    Log.d(TAG, "copy kb task targetChildPath = " + targetChildPath);
                    ThreadPoolExecutor insertExecutor = poolUtil.getInsertExecutor();
                    BlockingQueue<Runnable> blockingQueue = insertExecutor.getQueue();
                    long timestamp = System.currentTimeMillis();
                    //队列中等待的任务数量
                    int index = 0;
                    while (isExecutingTask) {
                        if (TextUtils.isEmpty(targetChildPath)) {
                            Log.d(TAG, "copy kb task targetChildPath is null");
                            if (listener != null) {
                                listener.taskInProgress(100);
                                listener.completeTask();
                            }
                            destroyTask();
                            return;
                        }
                        long completeSpace = freeSize - currentFreeSize;
                        //未完成任务数
                        undoneTaskCount = totalTaskCount - completeSpace;
                        if (completeSpace >= totalTaskCount) {
                            //完成
                            isExecutingTask = false;
                            if (!insertExecutor.isShutdown()) {
                                insertExecutor.shutdownNow();
                            }
                            if (listener != null) {
                                listener.taskInProgress(100);
                                listener.completeTask();
                            }
                        } else if (completeSpace >= newFolderReferenceValue) {
                            //每10GB文件，就不再新增任务到队列中，等本轮任务完成后重新创建文件夹继续任务
                            Log.d(TAG, "file total size > 10GB, undoneTaskCount = " + taskCounter.get());
                            if (taskCounter.get() == 0) {
                                //更新基准值
                                newFolderReferenceValue = completeSpace + BaseExecutor.STORAGE_SPACE_500M;
                                //本轮已执行完毕
                                targetChildPath = createNewDir(targetPath);
                                Log.d(TAG, "change folder copy kb task targetChildPath = " + targetChildPath);
                            }
                            SystemClock.sleep(50);
                        } else {
                            int waitTasks = blockingQueue.size();
                            if (waitTasks < ThreadPoolUtil.MAX_QUEUE) {
                                CopyTask copyTask = new CopyTask(sourcePath, targetChildPath + "/" + timestamp + "-" + index, taskCounter);
                                copyTask.setTaskProgressListener(KbExecutor.this);
                                insertExecutor.execute(copyTask);
                                index++;
                            } else {
                                SystemClock.sleep(2);
                            }
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    public void destroyTask() {
        isExecutingTask = false;
        if (timer != null) {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
            timer.cancel();
            timer = null;
        }
        if (poolUtil != null && !poolUtil.getInsertExecutor().isShutdown()) {
            poolUtil.getInsertExecutor().shutdownNow();
        }
    }

    //线程池任务进度
    @Override
    public void taskProgress(long undoneCount) {

    }


}
