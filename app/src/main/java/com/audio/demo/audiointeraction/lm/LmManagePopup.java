package com.audio.demo.audiointeraction.lm;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.audio.demo.audiointeraction.R;
import com.audio.demo.audiointeraction.bean.VideoViewObj;

/**
 * @author txw
 * @date 2018-07-15
 */
public class LmManagePopup extends PopupWindow implements View.OnTouchListener,
        View.OnClickListener, View.OnKeyListener {

    private TextView mTvMute;
    private TextView mTvExit;
    private TextView mTvCancel;
    private Context mContext;
    private VideoViewObj mBean;
    private LmManagePopupListener mListener;

    private View mRootView;

    public LmManagePopup(Context context, VideoViewObj bean, LmManagePopupListener listener) {
        mContext = context;
        mBean = bean;
        mListener = listener;
        initView();
        initData();
    }

    public void setBean(VideoViewObj bean) {
        mBean = bean;
        initData();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mRootView = inflater.inflate(R.layout.popup_lm_manage, null);
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        this.setFocusable(true);
        //设置动画效果
        this.setAnimationStyle(R.style.MyPopupWindow_anim_style);
        setContentView(mRootView);

        mTvMute = (TextView)mRootView.findViewById(R.id.tv_mute);
        mTvExit = (TextView)mRootView.findViewById(R.id.tv_exit);
        mTvCancel = (TextView)mRootView.findViewById(R.id.tv_cancel);

        mTvMute.setOnClickListener(this);
        mTvExit.setOnClickListener(this);
        mTvCancel.setOnClickListener(this);
    }

    private void initData() {
        if (LmDataBean.MUTE_CLOSE.equals(mBean.getMute())) {
            mTvMute.setText(mContext.getString(R.string.popup_lm_manage_mute));
        } else {
            mTvMute.setText(mContext.getString(R.string.popup_lm_manage_unmute));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_mute) {
            mListener.onMute(mBean);
            dismiss();
        } else if (id == R.id.tv_exit) {
            mListener.onExit(mBean);
            dismiss();
        } else if (id == R.id.tv_cancel) {
            dismiss();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && this.isShowing()) {
            this.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int height = mRootView.findViewById(R.id.rl_user_info).getTop();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (y < height) {
                dismiss();
            }
        }
        return true;
    }

    public interface LmManagePopupListener {
        /**
         * 静音、取消静音
         * @param bean
         */
        void onMute(VideoViewObj bean);

        /**
         * 关闭连麦
         * @param bean
         */
        void onExit(VideoViewObj bean);
    }
}
