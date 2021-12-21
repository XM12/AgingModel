package com.lenovo.agingmodel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.lenovo.agingmodel.executor.DeleteExector;
import com.lenovo.agingmodel.executor.TaskListener;
import com.lenovo.agingmodel.utils.FileUtil;
import com.lenovo.agingmodel.utils.StorageSpaceManager;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeleteFileActivity extends Activity implements View.OnClickListener, TaskListener {
    private static final String TAG = "DeleteFileActivity";

    private TextView tv_free_storage;
    private TextView tv_task_status;
    private TextView tv_start_time;
    private TextView tv_end_time;
    private TextView tv_kb_used_space;
    private TextView tv_photo_used_space;
    private TextView tv_audio_used_space;
    private TextView tv_video_used_space;
    private RadioButton rb_video;
    private RadioButton rb_audio;
    private RadioButton rb_photo;
    private RadioButton rb_kb;
    private EditText input_space_number;
    private TextView tv_unit;
    private Button btn_delete;
    private RadioGroup rg;
    private RpHandler rpHandler;
    private String[] maxDeleteSpace = new String[2];
    private FileUtil fileUtil;
    private Button btn_cancel;
    private boolean isExecutingTask = false;
    private Button btn_back;
    private DeleteExector deleteExector;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_file);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        rpHandler = new RpHandler(this);
        fileUtil = new FileUtil();
        initView();
        long freeSize = StorageSpaceManager.getInstance().getStorageSpaceSize(this)[1];
        tv_free_storage.setText(StorageSpaceManager.getInstance().getUnit(freeSize));
    }

    private void initView() {
        tv_free_storage = findViewById(R.id.tv_free_storage);
        tv_task_status = findViewById(R.id.tv_task_status);
        tv_start_time = findViewById(R.id.tv_start_time);
        tv_end_time = findViewById(R.id.tv_end_time);
        tv_kb_used_space = findViewById(R.id.tv_kb_used_space);
        tv_photo_used_space = findViewById(R.id.tv_photo_used_space);
        tv_audio_used_space = findViewById(R.id.tv_audio_used_space);
        tv_video_used_space = findViewById(R.id.tv_video_used_space);
        tv_unit = findViewById(R.id.tv_unit);

        input_space_number = findViewById(R.id.input_space_number);

        rg = findViewById(R.id.rg);
        rb_kb = findViewById(R.id.rb_4kb);
        rb_photo = findViewById(R.id.rb_photo);
        rb_audio = findViewById(R.id.rb_audio);
        rb_video = findViewById(R.id.rb_video);

        btn_delete = findViewById(R.id.btn_delete);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_back = findViewById(R.id.btn_back);

        btn_delete.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_back.setOnClickListener(this);
        rb_kb.setChecked(true);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_delete:
                if (deleteExector == null) {
                    switch (rg.getCheckedRadioButtonId()) {
                        case R.id.rb_4kb:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_4KB);
                            break;
                        case R.id.rb_8kb:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_8KB);
                            break;
                        case R.id.rb_128kb:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_128KB);
                            break;
                        case R.id.rb_photo:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_PHOTO);
                            break;
                        case R.id.rb_audio:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_AUDIO);
                            break;
                        case R.id.rb_video:
                            deleteExector = new DeleteExector(this, FileUtil.FILE_VIDEO);
                            break;
                    }
                    deleteExector.setOnTaskListener(this);
                    deleteExector.deleteTask();
                } else {
                    Toast.makeText(this, "正在删除文件，请先取消任务", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_cancel:
                if (deleteExector != null) {
                    deleteExector.destroyTask();
                    deleteExector = null;
                    rpHandler.removeCallbacksAndMessages(null);
                    rpHandler.sendEmptyMessage(RpHandler.TASKC_COMPLETE);
                    Toast.makeText(this, "任务已取消", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "没有任务执行", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_back:
                if (deleteExector != null) {
                    deleteExector.destroyTask();
                }
                finish();
                break;
        }
    }

    @Override
    public void startTask() {
        Log.d(TAG, "delete start");
        rpHandler.sendEmptyMessage(RpHandler.TASKC_START);
    }

    @Override
    public void taskInProgress(int progress) {

    }

    @Override
    public void completeTask() {
        Log.d(TAG, "delete end");
        if (deleteExector != null) {
            deleteExector.destroyTask();
            deleteExector = null;
        }
        rpHandler.removeCallbacksAndMessages(null);
        rpHandler.sendEmptyMessage(RpHandler.TASKC_COMPLETE);
    }

    private static class RpHandler extends Handler {
        public static final int REFRESH_STORAGE_SPACE_TIME = 2000;
        public static final int REFRESH_STORAGE_SPACE = 0;
        public static final int TASKC_START = 2;
        public static final int TASKC_COMPLETE = 3;
        private final WeakReference<DeleteFileActivity> weakReference;
        private DateFormat df;

        public RpHandler(DeleteFileActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            DeleteFileActivity activity = weakReference.get();
            switch (msg.what) {
                case TASKC_START:
                    activity.tv_task_status.setText("正在删除文件...，请耐心等待");
                    df = new SimpleDateFormat("HH:mm:ss");
                    activity.tv_start_time.setText(df.format(new Date()));
                    activity.tv_end_time.setText("");
                    sendEmptyMessageDelayed(REFRESH_STORAGE_SPACE, REFRESH_STORAGE_SPACE_TIME);
                    break;
                case REFRESH_STORAGE_SPACE:
                    //更新存储显示存储空间
                    Long[] storageSpaceSize = StorageSpaceManager.getInstance().getStorageSpaceSize(activity);
                    activity.tv_free_storage.setText(StorageSpaceManager.getInstance().getUnit(storageSpaceSize[1]));
                    sendEmptyMessageDelayed(REFRESH_STORAGE_SPACE, REFRESH_STORAGE_SPACE_TIME);
                    break;
                case TASKC_COMPLETE:
                    activity.isExecutingTask = false;
                    activity.tv_task_status.setText("任务完成");
                    df = new SimpleDateFormat("HH:mm:ss");
                    activity.tv_end_time.setText(df.format(new Date()));
                    sendEmptyMessage(REFRESH_STORAGE_SPACE);
                    Toast.makeText(activity, "任务完成", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isExecutingTask = false;
        if (deleteExector != null) {
            deleteExector.destroyTask();
        }
        if (rpHandler != null) {
            rpHandler.removeCallbacksAndMessages(null);
            rpHandler = null;
        }
    }
}