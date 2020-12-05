package com.yhl.cast.server.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yhl.cast.server.R;
import com.yhl.cast.server.albumpicker.api.OnItemClickListener;
import com.yhl.cast.server.albumpicker.model.AlbumFile;
import com.yhl.cast.server.albumpicker.widget.photoview.AttacherImageView;
import com.yhl.cast.server.albumpicker.widget.photoview.PhotoViewAttacher;

import java.util.List;

public class AlbumPagerAdapter extends PagerAdapter implements View.OnClickListener, View.OnLongClickListener {

    private Context mContext;
    private List<AlbumFile> mPreviewList;

    private OnItemClickListener mItemClickListener;
    private OnItemClickListener mItemLongClickListener;
    private final LayoutInflater mInflater;


    public AlbumPagerAdapter(Context context, List<AlbumFile> previewList) {
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
        this.mPreviewList = previewList;
    }


    public void UpdateDate(List<AlbumFile> previewList){
        this.mPreviewList = previewList;
        notifyDataSetChanged();
    }

    public void setItemClickListener(OnItemClickListener onClickListener) {
        this.mItemClickListener = onClickListener;
    }

    public void setItemLongClickListener(OnItemClickListener longClickListener) {
        this.mItemLongClickListener = longClickListener;
    }

    @Override
    public int getCount() {
        return mPreviewList == null ? 0 : mPreviewList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        //图片轮播或者视频播放
        if (mPreviewList!=null &&mPreviewList.size()>0){
            if (mPreviewList.get(0).getMediaType()== AlbumFile.TYPE_IMAGE){
                AttacherImageView imageView = new AttacherImageView(mContext);
                imageView.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
                final PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);
                imageView.setAttacher(attacher);
                AlbumFile file = mPreviewList.get(position);

                loadPreview(imageView, file, position);

                imageView.setId(position);
                if (mItemClickListener != null) {
                    imageView.setOnClickListener(this);
                }
                if (mItemLongClickListener != null) {
                    imageView.setOnLongClickListener(this);
                }

                container.addView(imageView);
                return imageView;
            }else{
                View view = mInflater.inflate(R.layout.album_preview_item_video, container, false);
                VideoView videoView = view.findViewById(R.id.iv_gallery_preview_video);
                AlbumFile file = mPreviewList.get(position);
                loadPreview(videoView, file, position);
                container.addView(view,0);
                return view;
            }
        }
        return null;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView( (View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void onClick(View v) {
        mItemClickListener.onItemClick(v, v.getId());
    }

    @Override
    public boolean onLongClick(View v) {
        mItemLongClickListener.onItemClick(v, v.getId());
        return true;
    }

    private void loadPreview(View view, AlbumFile item, int position) {
        if (item.getMediaType() == AlbumFile.TYPE_IMAGE) {
            Glide.with(mContext).load(item.getPath()).into((ImageView)view);
        } else {
            final VideoView videoView = (VideoView) view;
            videoView.setVideoPath(item.getPath());
            videoView.start();
            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //mp.setVolume(0f, 0f);
                    mp.setLooping(true);
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
            });
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    showShort(R.string.upload_select_album_album_preview_fail);
                    return true;
                }
            });
        }
    }

    private void showShort(@StringRes int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT);
    }
}