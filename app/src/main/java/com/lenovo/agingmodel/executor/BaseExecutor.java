package com.lenovo.agingmodel.executor;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.lenovo.agingmodel.utils.FileUtil;

import java.io.File;

public abstract class BaseExecutor {
    private static final String TAG = "BaseExecutor";
    public TaskListener listener;
    //每个文件夹最大容量为10G
    public static final long STORAGE_SPACE_2GB = 2 * 1000 * 1000 * 1000;
    public static final long STORAGE_SPACE_500M = 500 * 1000 * 1000;
    //媒体文件大小不确定，100MB以下的文件也进行分区存储，100MB以上的放在一个文件夹下
    public static final long FILE_PARTITION_STORAGE_SIZE = 100 * 1000 * 1000;
    public Context context;
    private String FILE_TYPE;
    public long totalTaskCount;
    public long undoneTaskCount;
    public boolean isExecutingTask = false;

    public BaseExecutor(Context context, String fileType) {
        this.context = context;
        this.FILE_TYPE = fileType;
    }

    public void copyTask(String sourcePath, String targetPath, long spaceOrNumber) {

    }

    public void deleteTask() {

    }

    public abstract void destroyTask();

    public String createNewDir(String targetPath) {
        File dir = new File(targetPath);
        if (!dir.exists()) {
            boolean mkdir = dir.mkdir();
            Log.i(TAG, "create target folder = " + mkdir);
            if (!mkdir) {
                return null;
            }
        }
        long currentTimeMillis = System.currentTimeMillis();
        String path = "";
        switch (FILE_TYPE) {
            case FileUtil.FILE_4KB:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_4KB + "-" + currentTimeMillis;
                break;
            case FileUtil.FILE_8KB:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_8KB + "-" + currentTimeMillis;
                break;
            case FileUtil.FILE_128KB:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_128KB + "-" + currentTimeMillis;
                break;
            case FileUtil.FILE_PHOTO:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_PHOTO + "-" + currentTimeMillis;
                break;
            case FileUtil.FILE_AUDIO:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_AUDIO + "-" + currentTimeMillis;
                break;
            case FileUtil.FILE_VIDEO:
                path = dir.getAbsolutePath() + "/" + FileUtil.FILE_VIDEO + "-" + currentTimeMillis;
                break;
        }
        if (!TextUtils.isEmpty(path)) {
            File newDir = new File(path);
            if (!newDir.exists()) {
                boolean mkdir = newDir.mkdir();
                Log.i(TAG, "create task folder = " + mkdir);
                if (mkdir) {
                    return newDir.getAbsolutePath();
                }
            } else {
                return newDir.getAbsolutePath();
            }
        }
        return null;
    }

    public File getDir() {
        String targetPath = "";
        switch (FILE_TYPE) {
            case FileUtil.FILE_4KB:
            case FileUtil.FILE_8KB:
            case FileUtil.FILE_128KB:
                targetPath = FileUtil.getInstance().getRootDir(context) + "/kb/" + FILE_TYPE;
                break;
            case FileUtil.FILE_PHOTO:
            case FileUtil.FILE_AUDIO:
            case FileUtil.FILE_VIDEO:
                targetPath = FileUtil.getInstance().getRootDir(context) + "/" + FILE_TYPE;
                break;
        }
        File file = new File(targetPath);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public void setOnTaskListener(TaskListener listener) {
        this.listener = listener;
    }

}
