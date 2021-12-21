package com.lenovo.agingmodel.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static final String FILE_4KB = "4kb";
    public static final String FILE_8KB = "8kb";
    public static final String FILE_128KB = "128kb";
    public static final String FILE_PHOTO = "photo";
    public static final String FILE_AUDIO = "audio";
    public static final String FILE_VIDEO = "video";

    public static final String[] FILE_TYPES = {FILE_4KB, FILE_8KB, FILE_128KB, FILE_PHOTO, FILE_AUDIO, FILE_VIDEO};

//    public static final String FILE_ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/aging_model_data";

    private static volatile FileUtil fileUtil;

    public FileUtil() {

    }

    public static FileUtil getInstance() {
        if (fileUtil == null) {
            synchronized (FileUtil.class) {
                if (fileUtil == null) {
                    fileUtil = new FileUtil();
                }
            }
        }
        return fileUtil;
    }

    public String getRootDir(Context context) {
//        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/aging_model_data";
        return context.getExternalFilesDir("").getAbsolutePath();
    }

    /**
     * copy文件
     *
     * @param targetPath sd文件路径
     */
    public void doCopyLocalFile(String sroucePath, String targetPath) {
        try {
            FileInputStream inputStream = new FileInputStream(sroucePath);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(targetPath));
            byte[] buffer = new byte[2048];
            int n = 0;
            while (-1 != (n = inputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, n);
            }
            fileOutputStream.flush();
            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            //if directory fails exception
            e.printStackTrace();
            Log.i(TAG, "doCopy error" + e.toString());
        }
    }

    /**
     * copy文件
     *
     * @param targetPath sd文件路径
     */
    public File doCopyAssetslFile(Context context, String assetsName, String targetPath) {
        try {
            Log.i(TAG, "doCopyAssetslFile: assets file targetPath = " + targetPath);
            InputStream inputStream = context.getAssets().open(assetsName);
            File file = new File(targetPath + "/" + assetsName);
            if (!file.exists()) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] buffer = new byte[2048];
                int n = 0;
                while (-1 != (n = inputStream.read(buffer))) {
                    fileOutputStream.write(buffer, 0, n);
                }
                fileOutputStream.flush();
                inputStream.close();
                fileOutputStream.close();
            }
            return file;
        } catch (IOException e) {
            //if directory fails exception
            e.printStackTrace();
            Log.i(TAG, "doCopy assets error" + e.toString());
        }
        return null;
    }

    public long getFileSize(String filePath) {
        FileChannel fc = null;
        long fileSize = 0;
        try {
            File f = new File(filePath);
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                fc = fis.getChannel();
                fileSize = fc.size();
            } else {
                Log.e("getFileSize", "file doesn't exist or is not a file");
            }
        } catch (IOException e) {
            Log.e("getFileSize", e.getMessage());
        } finally {
            if (null != fc) {
                try {
                    fc.close();
                } catch (IOException e) {
                    Log.e("getFileSize", e.getMessage());
                }
            }
        }
        return fileSize;
    }

    //打开文件管理器
    public void openFileManager(Activity context, String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);//选择图片
        context.startActivityForResult(intent, requestCode);
    }

    public String getChooseFileResultPath(Context context, Uri uri) {
        String chooseFilePath = null;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            //使用第三方应用打开
            chooseFilePath = uri.getPath();
            return chooseFilePath;
        }
        return getPath(context, uri);
    }

    private String getPath(final Context context, final Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            uri.getPath();

        }
        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * 获取文件长度
     *
     * @param file
     */
    public static void getFileSize(File file) {
        if (file.exists() && file.isFile()) {
            String fileName = file.getName();
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
