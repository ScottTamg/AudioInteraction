package com.audio.demo.audiointeraction.bean;

/**
 * Created by wangzhiguo on 17/9/30.
 */

public class VideoViewObj {
    public int mIndex;
    public long mBindUid;
    public boolean mIsUsing;
    public boolean mIsMuted;

    public boolean mIsMuteRemote;
    public boolean mIsRemoteDisableAudio;
//    public ImageView mReserveCamera;
//    public TextView mRemoteUserID;
//    public TextView mMuteVoiceBT;
//    public ViewGroup mRoot;
//    public View mRootBG;
//    public View mContentRoot;
//    public ImageView mSpeakImage;
//    public ImageView mRootHead;


    public VideoViewObj(int index) {
        mIndex = index;
        mBindUid = 0;
    }

    public void clear() {
//        mRoot = null;
//        mRootBG = null;
//        mContentRoot = null;
//        mSpeakImage = null;
//        mReserveCamera = null;
//        mRemoteUserID = null;
//        mMuteVoiceBT = null;
        clearData();
    }

    public void clearData(){
        mBindUid = 0;
        mIsUsing = false;
        mIsMuted = false;
        mIsMuteRemote = false;
        mIsRemoteDisableAudio = false;
    }

    @Override
    public String toString() {
        return "VideoViewObj{" +
                "mIndex=" + mIndex +
                ", mBindUid=" + mBindUid +
                ", mIsUsing=" + mIsUsing +
                ", mIsMuted=" + mIsMuted +
                ", mIsMuteRemote=" + mIsMuteRemote +
                ", mIsRemoteDisableAudio=" + mIsRemoteDisableAudio +
                '}';
    }
}
