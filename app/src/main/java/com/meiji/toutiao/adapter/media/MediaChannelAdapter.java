package com.meiji.toutiao.adapter.media;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.meiji.toutiao.R;
import com.meiji.toutiao.bean.media.MediaChannelBean;
import com.meiji.toutiao.interfaces.IOnItemClickListener;
import com.meiji.toutiao.utils.ImageLoader;
import com.meiji.toutiao.widget.CircleImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Meiji on 2017/4/9.
 */
@Deprecated
public class MediaChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<MediaChannelBean> list = new ArrayList<>();
    private Context context;
    private IOnItemClickListener onItemClickListener;

    public MediaChannelAdapter(List<MediaChannelBean> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public void setOnItemClickListener(IOnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_channel, parent, false);
        return new MediaChannelViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MediaChannelViewHolder viewHolder = (MediaChannelViewHolder) holder;
        MediaChannelBean bean = list.get(position);
        String url = bean.getAvatar();
        ImageLoader.loadCenterCrop(context, url, viewHolder.cv_avatar, R.color.viewBackground);
        viewHolder.tv_mediaName.setText(bean.getName());
        viewHolder.tv_descText.setText(bean.getDescText());
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    private class MediaChannelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private CircleImageView cv_avatar;
        private TextView tv_mediaName;
        private TextView tv_followCount;
        private TextView tv_descText;
        private IOnItemClickListener onItemClickListener;

        MediaChannelViewHolder(View itemView, IOnItemClickListener onItemClickListener) {
            super(itemView);

            this.onItemClickListener = onItemClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onClick(v, getLayoutPosition());
            }
        }
    }
}
