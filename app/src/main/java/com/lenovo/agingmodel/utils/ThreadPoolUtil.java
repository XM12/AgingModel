package com.lenovo.agingmodel.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    private static final String TAG = "ThreadPoolUtil";

    private static volatile ThreadPoolUtil threadPoolUtil;
    private ThreadPoolExecutor insertExecutor;
    private ThreadPoolExecutor deleteExecutor;
    //复制任务线程池空闲线程超时时间
    public static final long INSERT_THREAD_TIME_OUT = 60L;
    //删除任务线程池空闲线程超时时间
    public static final long DELETE_THREAD_TIME_OUT = 5L;
    //线程池最大线程
    public static final int MAX_THREAD = 100;
    //线程池最大核心线程
    public static final int MAX_CORE_THREAD = 16;
    //线程池最大队列
    public static final int MAX_QUEUE = 10000;

    private ThreadPoolUtil() {

    }

    public static ThreadPoolUtil getInstance() {
        if (threadPoolUtil == null) {
            synchronized (ThreadPoolUtil.class) {
                if (threadPoolUtil == null) {
                    threadPoolUtil = new ThreadPoolUtil();
                }
            }
        }
        return threadPoolUtil;
    }

    public ThreadPoolExecutor getInsertExecutor() {
        if (insertExecutor == null || insertExecutor.isShutdown()) {
            insertExecutor = new ThreadPoolExecutor(MAX_CORE_THREAD, MAX_THREAD,
                    INSERT_THREAD_TIME_OUT, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(MAX_QUEUE));
        }
        return insertExecutor;
    }

    public ThreadPoolExecutor getDeleteExecutor() {
        if (deleteExecutor == null || deleteExecutor.isShutdown()) {
            deleteExecutor = new ThreadPoolExecutor(MAX_CORE_THREAD, MAX_THREAD,
                    DELETE_THREAD_TIME_OUT, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(MAX_QUEUE));
        }
        return deleteExecutor;
    }

}
