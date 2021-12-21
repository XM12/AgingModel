package com.lenovo.agingmodel.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.storage.StorageManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class StorageSpaceManager {
    private static final String TAG = "StorageManager";
    private static volatile StorageSpaceManager manager;
    public static final long KB_2_B = 1000;
    public static final long MB_2_B = 1000 * 1000;
    public static final long GB_2_B = 1000 * 1000 * 1000;
    private String[] units = {"B", "KB", "MB", "GB", "TB"};

    private StorageSpaceManager() {

    }

    public static StorageSpaceManager getInstance() {
        if (manager == null) {
            synchronized (StorageSpaceManager.class) {
                if (manager == null) {
                    manager = new StorageSpaceManager();
                }
            }
        }
        return manager;
    }

    @SuppressLint({"NewApi", "UsableSpace"})
    public Long[] getStorageSpaceSize(Context context) {
        Long[] longs = {0L, 0L, 0L};
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumes = storageManager.getClass().getDeclaredMethod("getVolumes");
            List<Object> getVolumeInfo = (List<Object>) getVolumes.invoke(storageManager);
            if (getVolumeInfo != null) {
                for (Object obj : getVolumeInfo) {
                    Field getType = obj.getClass().getField("type");
                    int type = getType.getInt(obj);
                    if (type == 1) {
                        //获取内置内存总大小
                        Method isMountedReadable = obj.getClass().getDeclaredMethod("isMountedReadable");
                        boolean readable = (boolean) isMountedReadable.invoke(obj);
                        if (readable) {
                            Method file = obj.getClass().getDeclaredMethod("getPath");
                            File f = (File) file.invoke(obj);
                            if (f != null) {
                                longs[0] = f.getTotalSpace();
                                longs[1] = f.getFreeSpace();
                                longs[2] = f.getUsableSpace();
                            }
//                            Log.d(TAG, "getStorageSpaceSize: total = " + getUnit(longs[0]) + ", free = " + getUnit(longs[1]) + ", usable = " + getUnit(longs[2]));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return longs;
    }

    /**
     * 单位转换
     */
    public String getUnit(float size) {
        int index = 0;
        while (size > 1000 && index < 4) {
            size = size / 1000;
            index++;
        }
        return String.format(Locale.getDefault(), " %.2f %s", size, units[index]);
    }

    /**
     * 单位转换
     */
    public String[] getStorage(float size) {
        String[] space = new String[2];
        int index = 0;
        while (size > 1000 && index < 4) {
            size = size / 1000;
            index++;
        }
        space[0] = String.format(Locale.getDefault(), "%.2f", size);
        space[1] = units[index];
        return space;
    }

    /**
     * 单位转换
     */
    public String getStorageKb(float size) {
        while (size > KB_2_B) {
            size = size / KB_2_B;
        }
        return String.format(Locale.getDefault(), " %.2f", size);
    }

    /**
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指
     * 定精度，以后的数字四舍五入。
     *
     * @param v1    被除数
     * @param v2    除数
     * @param scale 表示表示需要精确到小数点以后几位。
     * @return 两个参数的商
     */
    public double div(double v1, double v2, int scale) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

}
