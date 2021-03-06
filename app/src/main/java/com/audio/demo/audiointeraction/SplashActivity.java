package com.audio.demo.audiointeraction;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.audio.demo.audiointeraction.bean.JniObjs;
import com.audio.demo.audiointeraction.bean.PermissionBean;
import com.audio.demo.audiointeraction.callback.MyTTTRtcEngineEventHandler;
import com.audio.demo.audiointeraction.dialog.VideoInfoDialog;
import com.audio.demo.audiointeraction.helper.TTTRtcEngineHelper;
import com.audio.demo.audiointeraction.utils.MyLog;
import com.audio.demo.audiointeraction.utils.PermissionUtils;
import com.audio.demo.audiointeraction.utils.SharedPreferencesUtil;
import com.jaeger.library.StatusBarUtil;
import com.wushuangtech.jni.NativeInitializer;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.util.ArrayList;
import java.util.List;

import static com.audio.demo.audiointeraction.LocalConfig.mLoginRoomID;
import static com.audio.demo.audiointeraction.LocalConfig.mPullUrlPrefix;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_AUDIENCE;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_BAD_VERSION;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_NOEXIST;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_TIMEOUT;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_UNKNOW;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_VERIFY_FAILED;

public class SplashActivity extends AppCompatActivity {

    public static final int VIDEO_MODE = 1;
    public static final int AUDIO_MODE = 2;

    private EditText mRoomIDET;
    private RadioButton mHostBT;
    private RadioButton mAuthorBT;
    private RadioButton mAudienceBT;
    private TTTRtcEngine mTTTEngine;
    private TTTRtcEngineHelper mTTTRtcEngineHelper;
    private VideoInfoDialog videoInfoDialog;
    private ProgressDialog mDialog;
    private Context mContext;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    public static boolean mIsLoging;
    private int mRole = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        mContext = this;

        initStatusBar();

        initView();

        checkPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.d("SplashActivity onDestroy....");
        TTTRtcEngine.destroy();
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void initView() {
        mAuthorBT = findViewById(R.id.vice);
        mAudienceBT = findViewById(R.id.audience);
        mRoomIDET = findViewById(R.id.room_id);
        mHostBT = findViewById(R.id.host);
        TextView mVersion = findViewById(R.id.version);
        String string = getResources().getString(R.string.version_info);
        String result = String.format(string, TTTRtcEngine.getInstance().getVersion());
        mVersion.setText(result);

        TextView mSdkVersion = findViewById(R.id.sdk_version);
        mSdkVersion.setText("sdk version : " + NativeInitializer.getIntance().getVersion());
    }

    private void initStatusBar() {
        //去掉标题栏
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //状态栏透明
        StatusBarUtil.setTranslucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void checkPermission() {
        final ArrayList<PermissionBean> mPermissionList = new ArrayList<>();
        mPermissionList.add(new PermissionBean(Manifest.permission.WRITE_EXTERNAL_STORAGE, getResources().getString(R.string.permission_write_external_storage)));
        mPermissionList.add(new PermissionBean(Manifest.permission.CAMERA, getResources().getString(R.string.permission_camera)));
        mPermissionList.add(new PermissionBean(Manifest.permission.RECORD_AUDIO, getResources().getString(R.string.permission_record_audio)));
        mPermissionList.add(new PermissionBean(Manifest.permission.READ_PHONE_STATE, getResources().getString(R.string.permission_read_phone_state)));
        boolean isOk = PermissionUtils.checkPermission(this, new PermissionUtils.PermissionUtilsInter() {
            @Override
            public List<PermissionBean> getApplyPermissions() {
                return mPermissionList;
            }

            @Override
            public AlertDialog.Builder getTipAlertDialog() {
                return null;
            }

            @Override
            public Dialog getTipDialog() {
                return null;
            }

            @Override
            public AlertDialog.Builder getTipAppSettingAlertDialog() {
                return null;
            }

            @Override
            public Dialog getTipAppSettingDialog() {
                return null;
            }
        });

        if (isOk) {
            init();
        }
    }

    private void init() {
        mTTTEngine = TTTRtcEngine.getInstance();
        mTTTRtcEngineHelper = new TTTRtcEngineHelper();
        // 读取保存的数据
        long roomID = (long) SharedPreferencesUtil.getParam(this, "RoomID", 0L);
        // 设置保存的数据
        if (roomID != 0) {
            String s = String.valueOf(roomID);
            mRoomIDET.setText(s);
            mRoomIDET.setSelection(s.length());
        }

        // 注册回调函数接收的广播
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        MyLog.d("SplashActivity onCreate.... model : " + Build.MODEL);
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("");
        mDialog.setMessage("正在进入房间...");

        videoInfoDialog = new VideoInfoDialog(mContext, R.style.NoBackGroundDialog);
    }

    public void onClickRoleButton(View v) {
        mHostBT.setChecked(false);
        mAuthorBT.setChecked(false);
        mAudienceBT.setChecked(false);

        ((RadioButton) v).setChecked(true);
        switch (v.getId()) {
            case R.id.host:
                mRole = CLIENT_ROLE_ANCHOR;
                findViewById(R.id.set).setEnabled(true);
                break;
            case R.id.vice:
                mRole = CLIENT_ROLE_BROADCASTER;
                findViewById(R.id.set).setEnabled(false);
                break;
            case R.id.audience:
                mRole = CLIENT_ROLE_AUDIENCE;
                findViewById(R.id.set).setEnabled(false);
                break;
        }
    }

    public void onClickEnterButton(View v) {
        boolean checkResult = mTTTRtcEngineHelper.splashCheckSetting(this, mRoomIDET.getText().toString());
        if (!checkResult) {
            return;
        }

        // 重置直播房间内的参数
        LocalConfig.mAudience = 0;
        LocalConfig.mAuthorSize = 0;

        // 设置频道属性
        mTTTEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);

        if (LocalConfig.mRoomMode == VIDEO_MODE) {
            // 启用视频模式
            mTTTEngine.enableVideo();
            mTTTEngine.muteLocalVideoStream(false);
        }
        // app里的录屏功能需开启高音质，仅支持5.0以上版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTTTEngine.setHighQualityAudioParameters(true);
        }

        // 设置进入直播房间的角色
        if (mRole == -1) {
            if (mHostBT.isChecked()) {
                mRole = CLIENT_ROLE_ANCHOR;
            } else if (mAuthorBT.isChecked()) {
                mRole = CLIENT_ROLE_BROADCASTER;
            } else if (mAudienceBT.isChecked()) {
                mRole = CLIENT_ROLE_AUDIENCE;
            }
        }
        LocalConfig.mRole = mRole;
        mTTTEngine.setClientRole(LocalConfig.mRole, null);
        // 拉流地址
        LocalConfig.mCDNAddress = mPullUrlPrefix + mLoginRoomID;
        // 保存配置
        SharedPreferencesUtil.setParam(this, "RoomID", mLoginRoomID);
        if (mIsLoging) {
            return;
        }
        mIsLoging = true;

        // 设置推流格式H264/H265
        if (videoInfoDialog.mCodingFormat == 0) {
            GlobalConfig.mPushUrl = LocalConfig.mPushUrlPrefix + mLoginRoomID + "?trans=1";
        } else {
            GlobalConfig.mPushUrl = LocalConfig.mPushUrlPrefix + mLoginRoomID;
        }

        if (LocalConfig.mExtraPushUrl != null) {
            GlobalConfig.mPushUrl = LocalConfig.mExtraPushUrl;
        }

        mTTTEngine.joinChannel("", String.valueOf(mLoginRoomID), LocalConfig.mLoginUserID);
        mDialog.show();
        return;

    }

    public void onSetButtonClick(View v) {
        videoInfoDialog.okButton.setOnClickListener(v1 -> {
            mTTTEngine.setVideoMixerParams(videoInfoDialog.mBitRate, videoInfoDialog.mFrameRate, videoInfoDialog.mWidth, videoInfoDialog.mHeight);
            mTTTEngine.setAudioMixerParams(videoInfoDialog.mAudioBitRate, videoInfoDialog.mSamplingRate, videoInfoDialog.mChannels);

            videoInfoDialog.dismiss();
        });
        videoInfoDialog.show();

    }

    public void onClickModeButton(View v) {
        ((RadioButton) findViewById(R.id.videolink)).setChecked(false);
        ((RadioButton) findViewById(R.id.audiolink)).setChecked(false);
        ((RadioButton) v).setChecked(true);
        switch (v.getId()) {
            case R.id.audiolink:
                LocalConfig.mRoomMode = AUDIO_MODE;
                break;
            case R.id.videolink:
                LocalConfig.mRoomMode = VIDEO_MODE;
                break;
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        boolean isOk = PermissionUtils.onActivityResults(this, resultCode);
        if (isOk) {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isOk = PermissionUtils.onRequestPermissionsResults(this, requestCode, permissions, grantResults);
        if (isOk) {
            init();
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(
                        MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM:
                        mDialog.dismiss();
                        //界面跳转
                        startActivity(new Intent(mContext, MainActivity.class));
                        PviewLog.testPrint("joinChannel", "end");
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        mIsLoging = false;
                        mDialog.dismiss();
                        final int errorType = mJniObjs.mErrorType;
                        runOnUiThread(() -> {
                            MyLog.d("onReceive CALL_BACK_ON_ERROR errorType : " + errorType);
                            if (errorType == ERROR_ENTER_ROOM_TIMEOUT) {
                                Toast.makeText(mContext, getResources().getString(R.string.error_timeout), Toast.LENGTH_SHORT).show();
                            } else if (errorType == ERROR_ENTER_ROOM_UNKNOW) {
                                Toast.makeText(mContext, getResources().getString(R.string.error_unconnect), Toast.LENGTH_SHORT).show();
                            } else if (errorType == ERROR_ENTER_ROOM_VERIFY_FAILED) {
                                Toast.makeText(mContext, getResources().getString(R.string.error_verification_code), Toast.LENGTH_SHORT).show();
                            } else if (errorType == ERROR_ENTER_ROOM_BAD_VERSION) {
                                Toast.makeText(mContext, getResources().getString(R.string.error_version), Toast.LENGTH_SHORT).show();
                            } else if (errorType == ERROR_ENTER_ROOM_NOEXIST) {
                                Toast.makeText(mContext, getResources().getString(R.string.error_noroom), Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
