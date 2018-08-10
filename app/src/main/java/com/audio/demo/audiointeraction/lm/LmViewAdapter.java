package com.audio.demo.audiointeraction.lm;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.audio.demo.audiointeraction.R;
import com.audio.demo.audiointeraction.bean.VideoViewObj;
import com.audio.demo.audiointeraction.widget.WaveView;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * @author txw
 * @date 2018/7/12
 * 语音连麦用户界面
 */
public class LmViewAdapter extends RecyclerView.Adapter<LmViewAdapter.LmViewHolder> {

    private Context mContext;
    private VideoViewObj[] mList;
    private LmViewItemListener mListener;
    private String[] mNames = {"成龙", "林心如", "陈一发", "周星驰", "高圆圆", "贾静雯", "胡歌",
            "陈奕迅", "周杰伦", "佟丽娅", "papi酱", "陈赫"};
    private int[] mIcons = {R.drawable.chenglong, R.drawable.linxinru, R.drawable.chenyifa,
            R.drawable.zhouxingchi, R.drawable.gaoyuanyuan, R.drawable.jiajingwen, R.drawable.huge,
            R.drawable.chenyixun, R.drawable.zhoujielun, R.drawable.tongliya, R.drawable.papi,
            R.drawable.chenhe};

    public LmViewAdapter(Context context, VideoViewObj[] list, LmViewItemListener listener) {
        this.mContext = context;
        this.mList = list;
        this.mListener = listener;
    }

    public void setList(VideoViewObj[] list) {
        mList = list;
        notifyDataSetChanged();
    }

    public void addData(VideoViewObj dataBean) {
        int index = -1;
        for (int i = 0; i < mList.length; i++) {
            if (mList[i] == null) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            mList[index] = dataBean;
            notifyDataSetChanged();
        }
    }

    public void removeData(int index) {
        mList[index] = new VideoViewObj(index);
        notifyDataSetChanged();
    }

    public void removeData(VideoViewObj dataBean) {
        int index = -1;
        for (int i = 0; i < mList.length; i++) {
            if (dataBean.mBindUid == mList[i].mBindUid) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            this.removeData(index);
        }
    }

    @NonNull
    @Override
    public LmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_lm_view, parent, false);
        return new LmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LmViewHolder holder, int position) {
        holder.ivAvatarBg.setDuration(5000);
        holder.ivAvatarBg.setStyle(Paint.Style.FILL);
        holder.ivAvatarBg.setColor(mContext.getResources().getColor(R.color.color_lm_bg));
        holder.ivAvatarBg.setInterpolator(new LinearOutSlowInInterpolator());

        if (mList[position] == null || mList[position].mBindUid == 0) {
            holder.ivAvatarBg.setVisibility(View.INVISIBLE);
            holder.ivMute.setVisibility(View.GONE);
            holder.ivAvatarBg.stop();
            holder.ivAvatar.setImageResource(R.drawable.moremtupian);
            holder.tvNickname.setText(R.string.audio_lm_empty_name);
            holder.tvNickname.setTextColor(mContext.getResources().getColor(R.color.white));
        } else {
            holder.ivAvatar.setImageResource(mIcons[position]);
            holder.tvNickname.setText(String.valueOf(mNames[position]));
            holder.tvNickname.setTextColor(mContext.getResources().getColor(R.color.color_lm_bg));
            if (!mList[position].mIsRemoteDisableAudio) {
                holder.ivAvatarBg.setVisibility(View.VISIBLE);
                holder.ivAvatarBg.start();

                holder.ivMute.setVisibility(View.GONE);
            } else {
                holder.ivAvatarBg.setVisibility(View.INVISIBLE);
                holder.ivAvatarBg.stop();
                holder.ivMute.setVisibility(View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> mListener.onItemClick(position, mList[position]));
    }

    @Override
    public int getItemCount() {
        return 12;
    }

    class LmViewHolder extends RecyclerView.ViewHolder {
        WaveView ivAvatarBg;
        CircleImageView ivAvatar;
        ImageView ivMute;
        TextView tvNickname;
        FrameLayout mFlLayout;
        View itemView;

        public LmViewHolder(View itemView) {
            super(itemView);
            ivAvatarBg = itemView.findViewById(R.id.iv_avatar_bg);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivMute = itemView.findViewById(R.id.iv_mute);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            mFlLayout = itemView.findViewById(R.id.fl_layout);
            this.itemView = itemView;
        }
    }

    public interface LmViewItemListener {
        void onItemClick(int position, VideoViewObj bean);
    }
}