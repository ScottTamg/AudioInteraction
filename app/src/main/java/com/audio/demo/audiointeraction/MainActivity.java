package com.audio.demo.audiointeraction;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.audio.demo.audiointeraction.bean.DisplayDevice;
import com.audio.demo.audiointeraction.bean.EnterUserInfo;
import com.audio.demo.audiointeraction.bean.JniObjs;
import com.audio.demo.audiointeraction.bean.VideoViewObj;
import com.audio.demo.audiointeraction.callback.MyTTTRtcEngineEventHandler;
import com.audio.demo.audiointeraction.callback.PhoneListener;
import com.audio.demo.audiointeraction.dialog.DataInfoShowCallback;
import com.audio.demo.audiointeraction.dialog.ExitRoomDialog;
import com.audio.demo.audiointeraction.dialog.MoreInfoDialog;
import com.audio.demo.audiointeraction.dialog.MusicListDialog;
import com.audio.demo.audiointeraction.helper.TTTRtcEngineHelper;
import com.audio.demo.audiointeraction.lm.LmManagePopup;
import com.audio.demo.audiointeraction.lm.LmViewAdapter;
import com.audio.demo.audiointeraction.lm.SpaceItemDecoration;
import com.audio.demo.audiointeraction.utils.MyLog;
import com.audio.demo.audiointeraction.widget.WaveView;
import com.jaeger.library.StatusBarUtil;
import com.wushuangtech.bean.VideoCompositingLayout;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_AUDIENCE;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.LocalSDKConstants.CAPTURE_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements DataInfoShowCallback {

    public static final int DISCONNECT = 100;

    public TextView mAudioSpeedShow;
    public TextView mVideoSpeedShow;
    private TextView mBroadcasterID;
    public ImageView mAudioChannel;
    private View mCannelMusicBT;
    private View mLocalMusicListBT;
    private View mReversalCamera;
    public TextView mRecordScreen;
    public ScrollView mScrollView;
    public ViewGroup mFullScreenShowView;
    private WaveView mWaveView;

    private MoreInfoDialog mMoreInfoDialog;
    private MusicListDialog mMusicListDialog;
    private ExitRoomDialog mExitRoomDialog;

    private LmManagePopup mLmManagePopup;
    private RecyclerView mRvLmLayout;
    private LmViewAdapter mLmViewAdapter;

    private TTTRtcEngineHelper mTTTRtcEngineHelper;
    private TTTRtcEngine mTTTEngine;
    private Context mContext;

    public VideoViewObj[] mLocalSeiList;
    public HashSet<Long> mMutedAudioUserID;
    public HashSet<Long> mMutedSpeakUserID;
    public List<EnterUserInfo> listData;
    public ConcurrentHashMap<Long, DisplayDevice> mShowingDevices;

    private boolean mIsMute;
    private boolean mIsPhoneComing;
    private boolean mIsStop;
    private boolean mIsSpeaker;
    public boolean mIsHeadset;
    private MyLocalBroadcastReceiver mLocalBroadcast;

    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mTTTEngine = TTTRtcEngine.getInstance();
        mTTTRtcEngineHelper = new TTTRtcEngineHelper(this);

        initStatusBar();
        initView();
        initData();
        initEngine();
        initDialog();
        initLmLiveView();

        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        if (LocalConfig.mRole == Constants.CLIENT_ROLE_ANCHOR) {
            mTTTEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, true);
        } else {
            mTTTEngine.setVideoProfile(Constants.VIDEO_PROFILE_120P, true);
        }
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        MyLog.d("MainActivity onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.d("MainActivity onStart mIsMute : " + mIsMute);
        mIsStop = false;
        if (LocalConfig.mRole != Constants.CLIENT_ROLE_AUDIENCE) {
            mTTTEngine.resumeAudioMixing();
            if (!mIsMute && !LocalConfig.mLocalMuteAuido) {
                mTTTEngine.muteLocalAudioStream(false);
            }
        }
        mTTTEngine.muteAllRemoteAudioStreams(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.d("MainActivity onStop");
        mIsStop = true;
        if (LocalConfig.mRole != Constants.CLIENT_ROLE_AUDIENCE) {
            mTTTEngine.pauseAudioMixing();
            mTTTEngine.muteLocalAudioStream(true);
        }
        mTTTEngine.muteAllRemoteAudioStreams(true);
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        mMoreInfoDialog.dismiss();
        mMusicListDialog.dismiss();
        LocalConfig.mUserEnterOrder.clear();
        for (int i = 0; i < mLocalSeiList.length; i++) {
            VideoViewObj videoViewObj = mLocalSeiList[i];
            if (videoViewObj != null) {
                videoViewObj.clear();
            }
        }

        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }

        unregisterReceiver(mLocalBroadcast);
        mTTTEngine.stopRecordScreen();
        mTTTEngine.enableEarsBack(false);
        super.onDestroy();
        SplashActivity.mIsLoging = false;
        MyLog.d("MainActivity onDestroy");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            mTTTRtcEngineHelper.realStartCapture(data);
        } else {
            mTTTRtcEngineHelper.mFlagRecord = 0;
            mMoreInfoDialog.closeShareScreen();
        }
    }

    private void initStatusBar() {
        //去掉标题栏
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //状态栏透明
        StatusBarUtil.setTranslucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFullScreenShowView = findViewById(R.id.main_background);
        mBroadcasterID = findViewById(R.id.main_btn_host);
        mCannelMusicBT = findViewById(R.id.main_btn_cannel_music);
        mLocalMusicListBT = findViewById(R.id.main_btn_music_channel);
        mRecordScreen = findViewById(R.id.main_btn_video_recorder_time);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);
        TextView mHourseID = findViewById(R.id.main_btn_title);
        mReversalCamera = findViewById(R.id.main_btn_camera);
        mScrollView = findViewById(R.id.main_btn_listly);
        mReversalCamera.setOnClickListener(v -> mTTTEngine.switchCamera());

        if (LocalConfig.mRoomMode == SplashActivity.AUDIO_MODE) {
            mReversalCamera.setVisibility(View.GONE);
            mVideoSpeedShow.setVisibility(View.GONE);
            findViewById(R.id.audio_bg).setVisibility(View.VISIBLE);
            mWaveView = findViewById(R.id.wv_audio_bg);
            mWaveView.setDuration(5000);
            mWaveView.setStyle(Paint.Style.FILL);
            mWaveView.setColor(getResources().getColor(R.color.color_lm_bg));
            mWaveView.setInterpolator(new LinearOutSlowInInterpolator());
            mWaveView.start();
        }

        setTextViewContent(mHourseID, R.string.main_title, String.valueOf(LocalConfig.mLoginRoomID));

        findViewById(R.id.main_btn_exit).setOnClickListener(v -> mExitRoomDialog.show());

        findViewById(R.id.main_btn_more).setOnClickListener(v -> mMoreInfoDialog.show());

        mLocalMusicListBT.setOnClickListener(v -> mMusicListDialog.show());

        mCannelMusicBT.setOnClickListener(v -> {
            mTTTEngine.stopAudioMixing();
            mMusicListDialog.setPlaying(false);
            mCannelMusicBT.setVisibility(View.INVISIBLE);
        });

        mAudioChannel.setOnClickListener(v -> {
            if (mIsMute) {
                if (mIsHeadset) {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                } else {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                }
                mTTTEngine.muteLocalAudioStream(false);
                mIsMute = false;
                mWaveView.start();
                mWaveView.setVisibility(View.VISIBLE);
            } else {
                if (mIsHeadset) {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                } else {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                }
                mTTTEngine.muteLocalAudioStream(true);
                mIsMute = true;
                mWaveView.stop();
                mWaveView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initData() {
        listData = new ArrayList<>();
        mShowingDevices = new ConcurrentHashMap<>(12);
        mMutedAudioUserID = new HashSet<>();
        mMutedSpeakUserID = new HashSet<>();
        mLocalSeiList = new VideoViewObj[12];

        mTTTRtcEngineHelper.initRemoteLayout(mLocalSeiList);

        if (LocalConfig.mRole == Constants.CLIENT_ROLE_AUDIENCE) {
            LocalConfig.mAudience++;
        } else if (LocalConfig.mRole == CLIENT_ROLE_BROADCASTER) {
            LocalConfig.mAuthorSize++;
            EnterUserInfo localInfo = new EnterUserInfo(LocalConfig.mLoginUserID, CLIENT_ROLE_BROADCASTER);
            addListData(localInfo);

            if (LocalConfig.mRoomMode == SplashActivity.AUDIO_MODE) {
                mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, localInfo);
            }
        }

        if (LocalConfig.mRoomMode == SplashActivity.AUDIO_MODE) {
//            for (VideoViewObj obj : mLocalSeiList) {
//                obj.mRootHead.setImageResource(R.drawable.audiobg);
//                obj.mRemoteUserID.setTextColor(Color.rgb(137, 137, 137));
//                ((TextView) obj.mContentRoot.findViewById(R.id.videoly_audio_down)).setTextColor(Color.rgb(137, 137, 137));
//            }
        }

        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR && LocalConfig.mRoomMode == SplashActivity.VIDEO_MODE) {
            SurfaceView localSurfaceView;
            localSurfaceView = mTTTEngine.CreateRendererView(mContext);
            localSurfaceView.setZOrderMediaOverlay(false);
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN,
                    localSurfaceView), getRequestedOrientation());
            mFullScreenShowView.addView(localSurfaceView, 0);
        }

        if (LocalConfig.mRole != CLIENT_ROLE_ANCHOR) {
            mAudioChannel.setClickable(false);
        } else {
            mAudioChannel.setClickable(true);
        }

        TextView mRoleShow = findViewById(R.id.main_btn_role);
        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
            setTextViewContent(mBroadcasterID, R.string.main_broadcaster, String.valueOf(LocalConfig.mLoginUserID));
        }

        switch (LocalConfig.mRole) {
            case CLIENT_ROLE_ANCHOR:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_anchor));
                LocalConfig.mBroadcasterID = LocalConfig.mLoginUserID;
                break;
            case CLIENT_ROLE_BROADCASTER:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_auxiliary));
                mLocalMusicListBT.setVisibility(View.GONE);
                mReversalCamera.setVisibility(View.GONE);
                break;
            case Constants.CLIENT_ROLE_AUDIENCE:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_audience));
                mLocalMusicListBT.setVisibility(View.GONE);
                mReversalCamera.setVisibility(View.GONE);
                break;
            default:
                break;
        }

        if (LocalConfig.mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
            } else {
                for (int i = 0; i < mLocalSeiList.length; i++) {
                    VideoViewObj videoCusSei = mLocalSeiList[i];
                    if (videoCusSei == null) {
                        continue;
                    }
                    if (videoCusSei.mBindUid == LocalConfig.mLoginUserID) {
//                        videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_headset_selector);
                        break;
                    }
                }
            }
        }
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    private void initDialog() {
        mMoreInfoDialog = new MoreInfoDialog(mContext, R.style.NoBackGroundDialog, mTTTEngine, mTTTRtcEngineHelper);
        mMoreInfoDialog.setDataInfoShowCallback(this);

        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            mTTTEngine.stopRecordScreen();
            exitRoom();
            mExitRoomDialog.dismiss();
        });

        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());

        mMusicListDialog = new MusicListDialog(mContext, R.style.NoBackGroundDialog);
        mMusicListDialog.setCanceledOnTouchOutside(true);
        mMusicListDialog.setMusicListOnClickListener(new MusicListDialog.MusicListOnClickListener() {
            @Override
            public void startAudioMixing(String filePath, boolean loopback, boolean replace, int cycle) {
                mTTTEngine.startAudioMixing(filePath, loopback, replace, cycle);
            }

            @Override
            public void stopAudioMixing() {
                mTTTEngine.stopAudioMixing();
            }

            @Override
            public void showCannelMusicPlayingBT() {
                mCannelMusicBT.setVisibility(View.VISIBLE);
            }
        });
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room , id : " + LocalConfig.mLoginUserID);
        mTTTEngine.stopAudioMixing();
        mTTTEngine.leaveChannel();
        LocalConfig.mLocalMuteAuido = false;
        finish();
    }

    public void switchRole(String string) {
        TextView mRoleShow = findViewById(R.id.main_btn_role);
        setTextViewContent(mRoleShow, R.string.main_local_role, string);
    }

    public EnterUserInfo removeListData(long mLoginUserID) {
        int index = -1;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == mLoginUserID) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            return listData.remove(index);
        }
        return null;
    }

    public void addListData(EnterUserInfo info) {
        boolean bupdate = false;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == info.getId()) {
                listData.set(i, info);
                bupdate = true;
                break;
            }
        }
        if (!bupdate) {
            listData.add(info);
        }
    }

    @Override
    public void showDataInfo(boolean isShow) {
        Set<Map.Entry<Long, DisplayDevice>> entries = mShowingDevices.entrySet();
        if (isShow) {
            mAudioSpeedShow.setVisibility(View.VISIBLE);
            mVideoSpeedShow.setVisibility(View.VISIBLE);
            for (Map.Entry<Long, DisplayDevice> next : entries) {
//                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_video_down).setVisibility(View.VISIBLE);
//                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_audio_down).setVisibility(View.VISIBLE);
            }
        } else {
            mAudioSpeedShow.setVisibility(View.INVISIBLE);
            mVideoSpeedShow.setVisibility(View.INVISIBLE);
            for (Map.Entry<Long, DisplayDevice> next : entries) {
//                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_video_down).setVisibility(View.INVISIBLE);
//                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_audio_down).setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * 初始化连麦界面
     */
    private void initLmLiveView() {
        mRvLmLayout = findViewById(R.id.rv_lm);
        mRvLmLayout.setLayoutManager(new GridLayoutManager(this, 6));
        mRvLmLayout.addItemDecoration(new SpaceItemDecoration(20));
        LmViewAdapter.LmViewItemListener listener = new LmViewAdapter.LmViewItemListener() {
            @Override
            public void onItemClick(int position, VideoViewObj bean) {
                if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                    showLmUserManage(bean, true);
                } else if (bean.mBindUid == LocalConfig.mLoginUserID) {
                    showLmUserManage(bean, false);
                }
            }
        };
        mLmViewAdapter = new LmViewAdapter(this, mLocalSeiList, listener);
        mRvLmLayout.setAdapter(mLmViewAdapter);
    }

    /**
     * 显示连麦管理弹出框
     */
    protected void showLmUserManage(VideoViewObj bean, boolean isMaster) {
        if (mLmManagePopup == null) {
            LmManagePopup.LmManagePopupListener listener = new LmManagePopup.LmManagePopupListener() {
                @Override
                public void onMute(VideoViewObj bean) {
                    if (!bean.mIsMuteRemote && !isMaster) {
                        Toast.makeText(mContext, getString(!isMaster
                                        ? R.string.popup_lm_mute_error_anchor : R.string.popup_lm_mute_error_audience),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (bean.mIsMuteRemote) {
                        mTTTEngine.muteRemoteSpeaking((int) bean.mBindUid, false);
                    } else {
                        mTTTEngine.muteRemoteSpeaking((int) bean.mBindUid, true);
                    }
                }

                @Override
                public void onExit(VideoViewObj bean) {
                    mTTTEngine.kickChannelUser(bean.mBindUid);
                }
            };
            mLmManagePopup = new LmManagePopup(mContext, bean, listener);
        } else {
            mLmManagePopup.setBean(bean);
        }
        if (!mLmManagePopup.isShowing()) {
            mLmManagePopup.showAtLocation(mFullScreenShowView,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        }
    }

    private void updateLmUserMuteState() {
        mLmViewAdapter.setList(mLocalSeiList);
    }

    private void removeLmUser(long userId) {
        int index = -1;
        for (int i = 0; i < mLocalSeiList.length; i++) {
            if (mLocalSeiList[i] == null) {
                continue;
            }
            if (mLocalSeiList[i].mIsUsing && mLocalSeiList[i].mBindUid == userId) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            mLmViewAdapter.removeData(index);
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
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        MyLog.d("UI onReceive CALL_BACK_ON_ERROR... ");
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.error_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.error_rtmp);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.error_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.error_anchorexited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.error_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.error_otherenter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.error_noaudio);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.error_novideo);
                        } else if (errorType == DISCONNECT) {
                            message = getResources().getString(R.string.error_network_disconnected);
                        }
                        mTTTRtcEngineHelper.showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_ROLE_CHANGED:
                        long changedUserId = mJniObjs.mUid;
                        int changedRole = mJniObjs.mIdentity;
                        LocalConfig.mRole = mJniObjs.mIdentity;
                        mMutedSpeakUserID.remove(changedUserId);
                        mMutedAudioUserID.remove(changedUserId);
                        if (changedRole == CLIENT_ROLE_AUDIENCE) {
                            mTTTRtcEngineHelper.removeUserByView(changedUserId);
                        } else {
                            if (LocalConfig.mRoomMode == SplashActivity.AUDIO_MODE) {
                                mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, new EnterUserInfo(LocalConfig.mLoginUserID, CLIENT_ROLE_BROADCASTER));
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_JOIN... uid : " + uid + " identity : " + identity);
                        EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                        addListData(userInfo);
                        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                            if (identity == CLIENT_ROLE_BROADCASTER) {
                                if (LocalConfig.mAuthorSize == TTTRtcEngineHelper.AUTHOR_MAX_NUM) {
                                    LocalConfig.mAuthorSize++;
                                    mTTTEngine.kickChannelUser(uid);
                                    return;
                                }
                            }

                            if (identity == CLIENT_ROLE_BROADCASTER) {
                                LocalConfig.mAuthorSize++;
                                // 打开视频
                                DisplayDevice mRemoteDisplayDevice = mShowingDevices.get(uid);
                                if (mRemoteDisplayDevice == null) {
                                    mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, userInfo);
                                }
                            } else {
                                if (LocalConfig.mRoomMode == SplashActivity.VIDEO_MODE) {
                                    // 向观众发送sei
                                    VideoCompositingLayout layout = new VideoCompositingLayout();
                                    layout.regions = mTTTRtcEngineHelper.buildRemoteLayoutLocation();
                                    mTTTEngine.setVideoCompositingLayout(layout);
                                }
                                LocalConfig.mAudience++;
                            }
                        } else {
                            if (identity == CLIENT_ROLE_ANCHOR) {
                                LocalConfig.mBroadcasterID = uid;
                                setTextViewContent(mBroadcasterID, R.string.main_broadcaster, String.valueOf(uid));
                                DisplayDevice mRemoteDisplayDevice = mShowingDevices.get(uid);
                                if (mRemoteDisplayDevice == null) {
                                    mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, userInfo);
                                }
                            } else if (identity == Constants.CLIENT_ROLE_AUDIENCE) {
                                LocalConfig.mAudience++;
                            } else if (identity == CLIENT_ROLE_BROADCASTER) {
                                if (LocalConfig.mRoomMode == SplashActivity.AUDIO_MODE) {
                                    DisplayDevice mRemoteDisplayDevice = mShowingDevices.get(uid);
                                    if (mRemoteDisplayDevice == null) {
                                        mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, userInfo);
                                    }
                                }
                                LocalConfig.mAuthorSize++;
                            }
                        }
                        if (mMoreInfoDialog != null) {
                            mMoreInfoDialog.updatePersonNum();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_OFFLINE... offLineUserID : " + offLineUserID);
                        EnterUserInfo enterUserInfo = removeListData(offLineUserID);
                        if (enterUserInfo != null) {
                            if (enterUserInfo.getRole() == Constants.CLIENT_ROLE_AUDIENCE) {
                                LocalConfig.mAudience--;
                            } else if (enterUserInfo.getRole() == CLIENT_ROLE_BROADCASTER) {
                                LocalConfig.mAuthorSize--;
                                removeLmUser(offLineUserID);
                            }
                            mTTTRtcEngineHelper.mRemoteUserLastVideoData.remove(enterUserInfo.getId());
                            mTTTRtcEngineHelper.mRemoteUserLastAudioData.remove(enterUserInfo.getId());
                            mTTTRtcEngineHelper.adJustRemoteViewDisplay(false, enterUserInfo);
                        }
                        if (mMoreInfoDialog != null) {
                            mMoreInfoDialog.updatePersonNum();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        mTTTRtcEngineHelper.audioVolumeIndication(mJniObjs.mUid, mJniObjs.mAudioLevel, mIsMute);
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        Log.d("zhxtest", "jie shou sei!!!: ");
                        String sei = mJniObjs.mSEI;
                        TreeSet<EnterUserInfo> mInfos = new TreeSet<>();
                        try {
                            JSONObject jsonObject = new JSONObject(sei);
                            String mid = (String) jsonObject.get("mid");
                            LocalConfig.mBroadcasterID = Integer.valueOf(mid);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                int z = Integer.valueOf(jsonobject2.getString("zOrder"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }

                                if (userId != LocalConfig.mBroadcasterID) {
                                    for (int j = 0; j < listData.size(); j++) {
                                        EnterUserInfo temp = listData.get(j);
                                        if (temp.getId() == userId) {
                                            temp.setXYLocation(y, x);
                                            mInfos.add(temp);
                                            break;
                                        }
                                    }
                                }

                                mLocalSeiList[z] = mShowingDevices.get(userId).getDisplayView();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        mTTTRtcEngineHelper.removeErrorIndexView(mInfos);
                        Iterator<EnterUserInfo> iterator = mInfos.iterator();
                        while (iterator.hasNext()) {
                            EnterUserInfo next = iterator.next();
                            mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, next);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        mTTTRtcEngineHelper.remoteVideoStatus(mJniObjs.mRemoteVideoStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        mTTTRtcEngineHelper.remoteAudioStatus(mJniObjs.mRemoteAudioStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        mTTTRtcEngineHelper.localVideoStatus(mJniObjs.mLocalVideoStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        mTTTRtcEngineHelper.LocalAudioStatus(mJniObjs.mLocalAudioStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        Log.d("WebRtcAudioRecord COME", "mIsStop : " + mIsStop);
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        Log.d("WebRtcAudioRecord IDLE", "mIsStop : " + mIsStop
                                + " | mIsPhoneComing ： " + mIsPhoneComing);
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_SCREEN_RECORD_TIME:
                        if (!mTTTRtcEngineHelper.mIsShareRecordering) {
                            runOnUiThread(() -> {
                                String s = mTTTRtcEngineHelper.showTimeCount(mJniObjs.mScreenRecordTime);
                                mRecordScreen.setText(s);
                            });
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_MUTE_AUDIO:
                        long muteUid = mJniObjs.mUid;
                        boolean mIsMuteAuido = mJniObjs.mIsDisableAudio;
                        MyLog.i("OnRemoteAudioMuted CALL_BACK_ON_MUTE_AUDIO start! .... " + mJniObjs.mUid
                                + " | mIsMuteAuido : " + mIsMuteAuido);
                        boolean mIsFound = false;
                        for (int i = 0; i < mLocalSeiList.length; i++) {
                            VideoViewObj videoCusSei = mLocalSeiList[i];
                            if (videoCusSei == null) {
                                continue;
                            }
                            if (videoCusSei.mBindUid == muteUid) {
                                mIsFound = true;
                                videoCusSei.mIsMuteRemote = mIsMuteAuido;
                                updateLmUserMuteState();

//                                if (mIsMuteAuido || videoCusSei.mIsRemoteDisableAudio) {
//                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_cancel_ban));
//                                    videoCusSei.mSpeakImage.setImageResource(R.drawable.jinyan);
//                                } else {
//                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_ban));
//                                    videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
//                                }
                                break;
                            }
                        }

                        if (!mIsFound && mJniObjs.mUid != LocalConfig.mBroadcasterID
                                && mIsMuteAuido) {
                            MyLog.i("OnRemoteAudioMuted could't find it .... " + mJniObjs.mUid);
                            mMutedAudioUserID.add(mJniObjs.mUid);
                        }
                        break;

                    case LocalConstans.CALL_BACK_ON_SPEAK_MUTE_AUDIO:
                        long speakUid = mJniObjs.mUid;
                        boolean mIsSpeakingMute = mJniObjs.mIsDisableAudio;
                        mIsFound = false;
                        MyLog.i("CALL_BACK_ON_SPEAK_MUTE_AUDIO " + mJniObjs.mUid + " | mIsMuteAuido : " + mIsSpeakingMute);
                        for (int i = 0; i < mLocalSeiList.length; i++) {
                            VideoViewObj videoCusSei = mLocalSeiList[i];
                            if (videoCusSei == null) {
                                continue;
                            }
                            if (videoCusSei.mBindUid == speakUid) {
                                mIsFound = true;
                                videoCusSei.mIsRemoteDisableAudio = mIsSpeakingMute;
                                updateLmUserMuteState();

                                if (mIsSpeakingMute) {
//                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_cancel_ban));
//                                    videoCusSei.mSpeakImage.setImageResource(R.drawable.jinyan);
                                } else {
//                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_ban));
//                                    videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                    videoCusSei.mIsMuteRemote = false;
                                    if (speakUid == LocalConfig.mLoginUserID) {
                                        mTTTEngine.muteLocalAudioStream(false);
                                    }
                                }
                                break;
                            }
                        }
                        if (!mIsFound && mJniObjs.mUid != LocalConfig.mBroadcasterID
                                && mIsSpeakingMute) {
                            MyLog.i("OnRemoteAudioMuted could't find it .... " + mJniObjs.mUid);
                            mMutedSpeakUserID.add(mJniObjs.mUid);
                        }

                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE:
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (mMoreInfoDialog != null) {
                            mMoreInfoDialog.audioRouteChange(mAudioRoute);
                        }
                        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                            if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                                mIsHeadset = false;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                }
                            } else {
                                mIsHeadset = true;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                                }
                            }
                        } else if (LocalConfig.mRole == CLIENT_ROLE_BROADCASTER) {
                            for (int i = 0; i < mLocalSeiList.length; i++) {
                                VideoViewObj videoCusSei = mLocalSeiList[i];
                                if (videoCusSei == null) {
                                    continue;
                                }
                                if (videoCusSei.mBindUid == LocalConfig.mLoginUserID) {
                                    if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                                        mIsHeadset = false;
                                        updateLmUserMuteState();
//                                        if (videoCusSei.mIsMuteRemote) {
//                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
//                                        } else {
//                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
//                                        }
                                    } else {
                                        mIsHeadset = true;
//                                        if (videoCusSei.mIsMuteRemote) {
//                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
//                                        } else {
//                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_headset_selector);
//                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

        }
    }
}
