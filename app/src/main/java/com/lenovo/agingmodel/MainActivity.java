package com.lenovo.agingmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lenovo.agingmodel.executor.BaseExecutor;
import com.lenovo.agingmodel.executor.KbExecutor;
import com.lenovo.agingmodel.executor.MediaExecutor;
import com.lenovo.agingmodel.executor.TaskListener;
import com.lenovo.agingmodel.utils.FileUtil;
import com.lenovo.agingmodel.utils.StorageSpaceManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressLint("SimpleDateFormat")
public class MainActivity extends Activity implements View.OnClickListener, TaskListener {
    private static final String TAG = "MainActivity";
    private FileUtil fileUtil;
    private TextView tv_free_storage;
    private TextView tv_task_progress;
    private TextView tv_file_number;
    private TextView tv_file_path;
    private TextView tv_start_time;
    private TextView tv_end_time;
    private EditText input_space_number;
    private EditText input_file_number;

    private final int REQUEST_CHOOSE_PHOTO = 0;
    private final int REQUEST_CHOOSE_AUDIO = 1;
    private final int REQUEST_CHOOSE_VIDEO = 2;
    private RpHandler rpHandler;
    private Button btn_copy_kb;
    private Button btn_copy_file;
    private Spinner sn_kb;
    private Spinner sn_file;

    private BaseExecutor executor;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        fileUtil = new FileUtil();
        rpHandler = new RpHandler(this);
        initView();
        initData();
        long freeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(this)[1];
        tv_free_storage.setText(StorageSpaceManager.getInstance().getUnit(freeSize));
    }

    private void initView() {
        ProgressBar progress_bar = findViewById(R.id.progress_bar);

        tv_free_storage = findViewById(R.id.tv_free_storage);
        tv_task_progress = findViewById(R.id.tv_task_progress);
        tv_file_number = findViewById(R.id.tv_file_number);
        tv_file_path = findViewById(R.id.tv_file_path);
        tv_start_time = findViewById(R.id.tv_start_time);
        tv_end_time = findViewById(R.id.tv_end_time);

        sn_kb = findViewById(R.id.sn_kb);
        sn_file = findViewById(R.id.sn_file);

        input_space_number = findViewById(R.id.input_space_number);
        input_file_number = findViewById(R.id.input_file_number);

        btn_copy_kb = findViewById(R.id.btn_copy_kb);
        btn_copy_file = findViewById(R.id.btn_copy_file);

        tv_file_path.setOnClickListener(this);
        btn_copy_kb.setOnClickListener(this);
        btn_copy_file.setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
    }

    private void initData() {
        List<String> kbTypes = new ArrayList<>();
        kbTypes.add(FileUtil.FILE_4KB);
        kbTypes.add(FileUtil.FILE_8KB);
        kbTypes.add(FileUtil.FILE_128KB);
        ArrayAdapter<String> kbAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, kbTypes);
        sn_kb.setAdapter(kbAdapter);

        List<String> mediaTypes = new ArrayList<>();
        mediaTypes.add("图片");
        mediaTypes.add("音频");
        mediaTypes.add("视频");
        ArrayAdapter<String> mediaAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, mediaTypes);
        sn_file.setAdapter(mediaAdapter);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_file_path:
                if (sn_file.getSelectedItemPosition() == 0) {
                    fileUtil.openFileManager(this, "image/*", REQUEST_CHOOSE_PHOTO);
                } else if (sn_file.getSelectedItemPosition() == 1) {
                    fileUtil.openFileManager(this, "audio/*", REQUEST_CHOOSE_AUDIO);
                } else {
                    fileUtil.openFileManager(this, "video/*", REQUEST_CHOOSE_VIDEO);
                }
                break;
            case R.id.btn_copy_kb:
                if (executor == null) {
                    String spaceNumber = input_space_number.getText().toString();
                    if (!TextUtils.isEmpty(spaceNumber)) {
                        long agingSpace = StorageSpaceManager.GB_2_B * Integer.parseInt(spaceNumber);
                        if (agingSpace > 0) {
                            String sourcePath = getKbSourcePath();
                            String targetPath = getKbTargetPath();
                            if (!TextUtils.isEmpty(sourcePath) && !TextUtils.isEmpty(targetPath)) {
                                executor = new KbExecutor(this, sourceFileType(v));
                                executor.setOnTaskListener(MainActivity.this);
                                executor.copyTask(sourcePath, targetPath, agingSpace);
                            } else {
                                Toast.makeText(this, TextUtils.isEmpty(sourcePath) ? "源文件不存在" : "目标文件夹不存在", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "填充的空间大小不能小于0", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "请输入需要填充的空间大小", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "任务进行中，请先取消任务", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_copy_file:
                if (executor == null) {
                    String photoNubmer = input_file_number.getText().toString();
                    if (!TextUtils.isEmpty(photoNubmer)) {
                        long fileNumber = Integer.parseInt(photoNubmer);
                        if (fileNumber > 0) {
                            String sourcePath = tv_file_path.getText().toString();
                            String targetPath = getMediaTargetPath();
                            if (!TextUtils.isEmpty(sourcePath) && !TextUtils.isEmpty(targetPath)) {
                                executor = new MediaExecutor(this, sourceFileType(v));
                                executor.setOnTaskListener(MainActivity.this);
                                executor.copyTask(sourcePath, targetPath, fileNumber);
                            } else {
                                Toast.makeText(this, TextUtils.isEmpty(sourcePath) ? "源文件不存在" : "目标文件夹不存在", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "填充的文件数量不能小于0", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "请输入需要填充的文件的数量", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "任务进行中，请先取消任务", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_cancel:
                if (executor != null) {
                    executor.destroyTask();
                    executor = null;
                }
                rpHandler.removeCallbacksAndMessages(null);
                rpHandler.sendEmptyMessage(RpHandler.TASK_COMPLETE);
                Toast.makeText(this, "任务已取消", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_delete:
                Intent intent = new Intent(this, DeleteFileActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void startTask() {
        rpHandler.removeCallbacksAndMessages(null);
        rpHandler.sendEmptyMessage(RpHandler.TASK_START);
        rpHandler.sendEmptyMessageDelayed(RpHandler.REFRESH_STORAGE_SPACE, RpHandler.REFRESH_STORAGE_SPACE_TIME);
    }

    @Override
    public void taskInProgress(int progress) {
        Message msg = rpHandler.obtainMessage();
        msg.what = RpHandler.REFRESH_TASK_PROGRESS;
        msg.obj = progress;
        rpHandler.sendMessage(msg);
    }

    @Override
    public void completeTask() {
        executor.destroyTask();
        executor = null;
        rpHandler.sendEmptyMessage(RpHandler.TASK_COMPLETE);
    }

    private static class RpHandler extends Handler {
        public static final int REFRESH_STORAGE_SPACE_TIME = 2000;
        public static final int TASK_START = 1;
        public static final int TASK_COMPLETE = 2;
        public static final int REFRESH_STORAGE_SPACE = 3;
        public static final int REFRESH_TASK_PROGRESS = 4;
        private final WeakReference<MainActivity> weakReference;
        private DateFormat df;

        public RpHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity activity = weakReference.get();
            switch (msg.what) {
                case TASK_START:
                    long freeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(activity)[1];
                    activity.tv_free_storage.setText(StorageSpaceManager.getInstance().getUnit(freeSize));
                    activity.tv_task_progress.setText("0%");
                    df = new SimpleDateFormat("HH:mm:ss");
                    activity.tv_start_time.setText(df.format(new Date()));
                    activity.tv_end_time.setText("");
                    break;
                case REFRESH_STORAGE_SPACE:
                    //更新存储显示存储空间
                    long currentFreeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(activity)[1];
                    activity.tv_free_storage.setText(StorageSpaceManager.getInstance().getUnit(currentFreeSize));
                    sendEmptyMessageDelayed(REFRESH_STORAGE_SPACE, REFRESH_STORAGE_SPACE_TIME);
                    break;
                case REFRESH_TASK_PROGRESS:
                    //更新任务进度
                    int progress = (int) msg.obj;
                    activity.tv_task_progress.setText(progress + "%");
                    break;
                case TASK_COMPLETE:
                    removeCallbacksAndMessages(null);
                    activity.tv_task_progress.setText("任务完成");
                    df = new SimpleDateFormat("HH:mm:ss");
                    activity.tv_end_time.setText(df.format(new Date()));
                    break;
            }
        }
    }

    //获取源文件类型
    private String sourceFileType(View view) {
        if (view.getId() == R.id.btn_copy_kb) {
            if (sn_kb.getSelectedItemPosition() == 0) {
                return FileUtil.FILE_4KB;
            } else if (sn_kb.getSelectedItemPosition() == 1) {
                return FileUtil.FILE_8KB;
            } else {
                return FileUtil.FILE_128KB;
            }
        } else {
            if (sn_file.getSelectedItemPosition() == 0) {
                return FileUtil.FILE_PHOTO;
            } else if (sn_file.getSelectedItemPosition() == 1) {
                return FileUtil.FILE_AUDIO;
            } else {
                return FileUtil.FILE_VIDEO;
            }
        }
    }

    //获取kb文件源文件路径
    private String getKbSourcePath() {
        String targetPath = FileUtil.getInstance().getRootDir(this) + "/kb";
        File _kbDir = new File(targetPath);
        if (!_kbDir.exists()) {
            _kbDir.mkdirs();
        }
        switch (sn_kb.getSelectedItemPosition()) {
            case 0:
                File _4kbFile = fileUtil.doCopyAssetslFile(this, "4kb-file", targetPath);
                if (_4kbFile != null && _4kbFile.exists()) {
                    return _4kbFile.getAbsolutePath();
                }
                return null;
            case 1:
                File _8kbFile = fileUtil.doCopyAssetslFile(this, "8kb-file", targetPath);
                if (_8kbFile != null && _8kbFile.exists()) {
                    return _8kbFile.getAbsolutePath();
                }
                return null;
            case 2:
                File _128kbFile = fileUtil.doCopyAssetslFile(this, "128kb-file", targetPath);
                if (_128kbFile != null && _128kbFile.exists()) {
                    return _128kbFile.getAbsolutePath();
                }
                return null;
        }
        return null;
    }

    //获取kb文件目标文件夹路径
    private String getKbTargetPath() {
        String targetPath = FileUtil.getInstance().getRootDir(this) + "/kb";
        File _kbDir = new File(targetPath);
        if (!_kbDir.exists()) {
            _kbDir.mkdirs();
        }
        File _4kbDir = new File(targetPath + "/" + FileUtil.FILE_4KB);
        if (!_4kbDir.exists()) {
            _4kbDir.mkdirs();
        }
        File _8kbDir = new File(targetPath + "/" + FileUtil.FILE_8KB);
        if (!_8kbDir.exists()) {
            _8kbDir.mkdirs();
        }
        File _128kbDir = new File(targetPath + "/" + FileUtil.FILE_128KB);
        if (!_128kbDir.exists()) {
            _128kbDir.mkdirs();
        }
        switch (sn_kb.getSelectedItemPosition()) {
            case 0:
                if (_4kbDir.exists()) {
                    return _4kbDir.getAbsolutePath();
                }
                return null;
            case 1:
                if (_8kbDir.exists()) {
                    return _8kbDir.getAbsolutePath();
                }
                return null;
            case 2:
                if (_128kbDir.exists()) {
                    return _128kbDir.getAbsolutePath();
                }
                return null;
        }
        return null;
    }

    //获取媒体文件目标文件夹路径
    private String getMediaTargetPath() {
        String targetPath = FileUtil.getInstance().getRootDir(this);
        File _photoDir = new File(targetPath + "/" + FileUtil.FILE_PHOTO);
        if (!_photoDir.exists()) {
            _photoDir.mkdirs();
        }
        File _audioDir = new File(targetPath + "/" + FileUtil.FILE_AUDIO);
        if (!_audioDir.exists()) {
            _audioDir.mkdirs();
        }
        File _videoDir = new File(targetPath + "/" + FileUtil.FILE_VIDEO);
        if (!_videoDir.exists()) {
            _videoDir.mkdirs();
        }
        switch (sn_file.getSelectedItemPosition()) {
            case 0:
                if (_photoDir.exists()) {
                    return _photoDir.getAbsolutePath();
                }
                return null;
            case 1:
                if (_audioDir.exists()) {
                    return _audioDir.getAbsolutePath();
                }
                return null;
            case 2:
                if (_videoDir.exists()) {
                    return _videoDir.getAbsolutePath();
                }
                return null;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri == null) {
                Log.d(TAG, "onActivityResult: " + requestCode + ", uri is null");
                return;
            }
            try {
                String path = fileUtil.getChooseFileResultPath(this, uri);
                tv_file_path.setText(path);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "choose file error : " + e.toString());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.destroyTask();
        }
        if (rpHandler != null) {
            rpHandler.removeCallbacksAndMessages(null);
            rpHandler = null;
        }
    }

}