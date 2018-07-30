package com.audio.demo.audiointeraction;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.audio.demo.audiointeraction.bean.DisplayDevice;
import com.audio.demo.audiointeraction.bean.EnterUserInfo;
import com.audio.demo.audiointeraction.bean.VideoViewObj;
import com.audio.demo.audiointeraction.dialog.ExitRoomDialog;
import com.audio.demo.audiointeraction.dialog.MoreInfoDialog;
import com.audio.demo.audiointeraction.dialog.MusicListDialog;
import com.audio.demo.audiointeraction.helper.TTTRtcEngineHelper;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;

public class MainActivity extends AppCompatActivity {

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

    private MoreInfoDialog mMoreInfoDialog;
    private MusicListDialog mMusicListDialog;
    private ExitRoomDialog mExitRoomDialog;

    private TTTRtcEngineHelper mTTTRtcEngineHelper;
    private TTTRtcEngine mTTTEngine;

    public ArrayList<VideoViewObj> mLocalSeiList;
    public HashSet<Long> mMutedAudioUserID;
    public HashSet<Long> mMutedSpeakUserID;
    public List<EnterUserInfo> listData;
    public ConcurrentHashMap<Long, DisplayDevice> mShowingDevices;

    private boolean mIsMute;
    private boolean mIsPhoneComing;
    private boolean mIsStop;
    private boolean mIsSpeaker;
    public boolean mIsHeadset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTTTEngine = TTTRtcEngine.getInstance();
        mTTTRtcEngineHelper = new TTTRtcEngineHelper(this);

        initView();
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
        }

        setTextViewContent(mHourseID, R.string.main_title, String.valueOf(LocalConfig.mLoginRoomID));

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        findViewById(R.id.main_btn_more).setOnClickListener(v -> {
            mMoreInfoDialog.show();
        });

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
            } else {
                if (mIsHeadset) {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                } else {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                }
                mTTTEngine.muteLocalAudioStream(true);
                mIsMute = true;
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
        mShowingDevices = new ConcurrentHashMap<>();
        mMutedAudioUserID = new HashSet<>();
        mMutedSpeakUserID = new HashSet<>();
        mLocalSeiList = new ArrayList<>();

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
            for (VideoViewObj obj : mLocalSeiList) {
                obj.mRootHead.setImageResource(R.drawable.audiobg);
                obj.mRemoteUserID.setTextColor(Color.rgb(137, 137, 137));
                ((TextView) obj.mContentRoot.findViewById(R.id.videoly_audio_down)).setTextColor(Color.rgb(137, 137, 137));
            }
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
        }

        if (LocalConfig.mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
            } else {
                for (int i = 0; i < mLocalSeiList.size(); i++) {
                    VideoViewObj videoCusSei = mLocalSeiList.get(i);
                    if (videoCusSei.mBindUid == LocalConfig.mLoginUserID) {
                        videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_headset_selector);
                        break;
                    }
                }
            }
        }
    }

    public void removeListData(long mLoginUserID) {

    }

    public void switchRole(String string) {

    }

    public void addListData(EnterUserInfo enterUserInfo) {

    }
}
