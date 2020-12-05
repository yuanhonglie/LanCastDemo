package com.yhl.cast.server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yhl.cast.server.R;
import com.yhl.cast.server.albumpicker.api.OnCheckedClickListener;
import com.yhl.cast.server.albumpicker.api.OnItemClickListener;
import com.yhl.cast.server.albumpicker.model.AlbumFile;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_VIDEO = 3;

    public static final int MODE_MULTIPLE = 1;
    public static final int MODE_SINGLE = 2;

    private int selectedItem = 0;
    private Context mContext;
    private final LayoutInflater mInflater;
    private final int mChoiceMode;

    private boolean isPicFilter;
    private boolean isVideoFilter;

    private List<AlbumFile> mAlbumFiles;

    private OnItemClickListener mItemClickListener;
    private OnCheckedClickListener mCheckedClickListener;

    public AlbumAdapter(Context context, int choiceMode) {
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mChoiceMode = choiceMode;
    }

    public void setAlbumFiles(List<AlbumFile> albumFiles) {
        this.mAlbumFiles = albumFiles;
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public void setCheckedClickListener(OnCheckedClickListener checkedClickListener) {
        this.mCheckedClickListener = checkedClickListener;
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
        notifyDataSetChanged();
    }

    public int getSelectedItem() {
        return selectedItem;
    }

    @Override
    public int getItemCount() {
        return mAlbumFiles == null ? 0 : mAlbumFiles.size();
    }

    @Override
    public int getItemViewType(int position) {
        AlbumFile albumFile = mAlbumFiles.get(position);
        return albumFile.getMediaType() == AlbumFile.TYPE_VIDEO ? TYPE_VIDEO : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_IMAGE:
            case TYPE_VIDEO:
                AlbumHolder viewHolder = new AlbumHolder(mInflater.inflate(R.layout.album_item_content, parent, false),
                        mItemClickListener,
                        mCheckedClickListener);
                /*
                if (mChoiceMode == MODE_MULTIPLE) {
                    viewHolder.mCheckBox.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.mCheckBox.setVisibility(View.GONE);
                }
                 */
                return viewHolder;

            default:
                throw new AssertionError("This should not be the case.");

        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case TYPE_IMAGE:{
                AlbumHolder imageHolder = (AlbumHolder) holder;
                position = holder.getAdapterPosition();
                AlbumFile albumFile = mAlbumFiles.get(position);
                imageHolder.mCheckBox.setChecked(selectedItem == position);
                imageHolder.mTvDuration.setVisibility(View.GONE);
                imageHolder.mCheckBox.setVisibility(View.VISIBLE);
                /*
                if (isPicFilter) {
                    imageHolder.mCheckBox.setVisibility(View.GONE);
                    imageHolder.mLayoutLayer.setVisibility(View.VISIBLE);
                } else {
                    imageHolder.mCheckBox.setVisibility(View.VISIBLE);
                    imageHolder.mLayoutLayer.setVisibility(View.GONE);
                }*/
                Glide.with(mContext).load(albumFile.getPath())
                        .dontAnimate().placeholder(R.drawable.default_diagram)
                        .error(R.drawable.default_diagram)
                        .centerCrop().into(imageHolder.mIvImage);
                break;
            }
            case TYPE_VIDEO: {
                AlbumHolder videoHolder = (AlbumHolder) holder;
                position = holder.getAdapterPosition();
                AlbumFile albumFile = mAlbumFiles.get(position);
                videoHolder.mCheckBox.setChecked(selectedItem == position);
                videoHolder.mTvDuration.setVisibility(View.VISIBLE);
                videoHolder.mTvDuration.setText(convertDuration(albumFile.getDuration()));
                /*
                if (isVideoFilter) {
                    videoHolder.mCheckBox.setVisibility(View.GONE);
                    videoHolder.mLayoutLayer.setVisibility(View.VISIBLE);
                } else {
                    videoHolder.mCheckBox.setVisibility(View.VISIBLE);
                    videoHolder.mLayoutLayer.setVisibility(View.GONE);
                }*/
                videoHolder.mCheckBox.setVisibility(View.VISIBLE);
                Glide.with(mContext).load(albumFile.getPath())
                        .dontAnimate().placeholder(R.drawable.default_diagram).error(R.drawable.default_diagram)
                        .centerCrop().into(videoHolder.mIvImage);
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }


    public void setPicFilter(boolean isPicFilter){
        //this.isPicFilter = isPicFilter;
    }

    public void setVideoFilter(boolean isVideoFilter){
         //this.isVideoFilter = isVideoFilter;
    }

    private static class AlbumHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final OnItemClickListener mItemClickListener;
        private final OnCheckedClickListener mCheckedClickListener;
        private int mSelected = 0;
        private ImageView mIvImage;
        private AppCompatCheckBox mCheckBox;
        private TextView mTvDuration;

        private FrameLayout mLayoutLayer;

        AlbumHolder(View itemView, OnItemClickListener itemClickListener, OnCheckedClickListener checkedClickListener) {
            super(itemView);
            this.mItemClickListener = itemClickListener;
            this.mCheckedClickListener = checkedClickListener;

            mIvImage = itemView.findViewById(R.id.iv_album_content_image);
            mCheckBox = itemView.findViewById(R.id.check_box);
            mTvDuration = itemView.findViewById(R.id.tv_duration);
            mLayoutLayer = itemView.findViewById(R.id.layout_layer);

            itemView.setOnClickListener(this);
            //mCheckBox.setOnClickListener(this);
            //mLayoutLayer.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            /*
            if (v == itemView) {
                //mItemClickListener.onItemClick(v, getAdapterPosition() - camera);
                mCheckBox.setChecked(!mCheckBox.isChecked());
                mCheckedClickListener.onCheckedClick(mCheckBox, getAdapterPosition());
            } else if (v == mCheckBox) {
                mCheckedClickListener.onCheckedClick(mCheckBox, getAdapterPosition());
            } else if (v == mLayoutLayer) {

            }
             */

            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, getAdapterPosition());
            }

        }
    }

    @NonNull
    private String convertDuration(@IntRange(from = 1) long duration) {
        duration /= 1000;
        int hour = (int) (duration / 3600);
        int minute = (int) ((duration - hour * 3600) / 60);
        int second = (int) (duration - hour * 3600 - minute * 60);

        String hourValue = "";
        String minuteValue;
        String secondValue;
        if (hour > 0) {
            if (hour >= 10) {
                hourValue = Integer.toString(hour);
            } else {
                hourValue = "0" + hour;
            }
            hourValue += ":";
        }
        if (minute > 0) {
            if (minute >= 10) {
                minuteValue = Integer.toString(minute);
            } else {
                minuteValue = "0" + minute;
            }
        } else {
            minuteValue = "00";
        }
        minuteValue += ":";
        if (second > 0) {
            if (second >= 10) {
                secondValue = Integer.toString(second);
            } else {
                secondValue = "0" + second;
            }
        } else {
            secondValue = "00";
        }
        return hourValue + minuteValue + secondValue;
    }
}