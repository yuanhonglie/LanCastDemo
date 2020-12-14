package com.yhl.cast.server;

import android.Manifest;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yhl.cast.server.adapter.AlbumAdapter;
import com.yhl.cast.server.albumpicker.api.Filter;
import com.yhl.cast.server.albumpicker.api.OnItemClickListener;
import com.yhl.cast.server.albumpicker.data.MediaReadTask;
import com.yhl.cast.server.albumpicker.data.MediaReader;
import com.yhl.cast.server.albumpicker.model.AlbumFile;
import com.yhl.cast.server.albumpicker.model.AlbumFolder;
import com.yhl.cast.server.albumpicker.widget.photoview.ItemDivider;
import com.yhl.cast.server.data.UserInfo;
import com.yhl.lanlink.LanLinkSender;
import com.yhl.lanlink.ServiceInfo;
import com.yhl.lanlink.base.BaseActivity;
import com.yhl.lanlink.data.ActionType;
import com.yhl.lanlink.data.ControlInfo;
import com.yhl.lanlink.data.Media;
import com.yhl.lanlink.data.MediaType;
import com.yhl.lanlink.data.PlayMode;
import com.yhl.lanlink.data.TaskInfo;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by tali on 2018/9/15.
 */
public class AlbumPickActivity extends BaseActivity implements MediaReadTask.Callback {

    private static final String TAG = "AlbumPickActivity";
    LinearLayout mLayoutLoading;
    RecyclerView mRecyclerView;
    ImageView mIvPreview;
    VideoView mVvPreview;

    TextView tvVersion;

    View ivBack;

    private static final int PHOTO_LIMIT_COUNT = 10;
    private static final int VIDEO_LIMIT_SIZE = 200 * 1024 * 1024;

    private GridLayoutManager mLayoutManager;
    private AlbumAdapter mAdapter;

    private MediaReadTask mMediaReadTask;

    private AlbumFolder mAlbumFolder;
    private AlbumFolder mVideoFolder;

    private ArrayList<AlbumFile> mCheckedList = new ArrayList<>();

    private boolean isShowDevice = false;
    private int deviceType = 0;
    private ServiceInfo mServiceInfo;
    private int mMediaType = AlbumFile.TYPE_VIDEO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_pick);
        initView();
        initData();
    }

    private void initView() {
        mLayoutLoading = findViewById(R.id.layout_loading);
        mRecyclerView = findViewById(R.id.recyclerView);
        mIvPreview = findViewById(R.id.iv_preview);
        mVvPreview = findViewById(R.id.vv_preview);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(this::onClick);
        findViewById(R.id.btn_cast).setOnClickListener(this::onClick);
        findViewById(R.id.btn_transfer).setOnClickListener(this::onClick);
    }

    protected void initData() {
        if (getIntent() != null) {
            isShowDevice = getIntent().getBooleanExtra("isShowDevice", false);
            deviceType = getIntent().getIntExtra("DeviceType", 0);
            mServiceInfo = getIntent().getParcelableExtra(ConnectionActivityKt.SERVICE_INFO);
            mMediaType = getIntent().getIntExtra(ConnectionActivityKt.MEDIA_TYPE, AlbumFile.TYPE_VIDEO);
        }

        Configuration config = getResources().getConfiguration();
        mLayoutManager = new GridLayoutManager(this, 3, getOrientation(config), false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setNestedScrollingEnabled(false);
        int dividerSize = getResources().getDimensionPixelSize(R.dimen.album_divider);
        mRecyclerView.addItemDecoration(new ItemDivider(android.graphics.Color.TRANSPARENT, dividerSize, dividerSize));
        mAdapter = new AlbumAdapter(this, AlbumAdapter.MODE_MULTIPLE);
        mAdapter.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mAdapter.setSelectedItem(position);
                AlbumFolder folder = mMediaType == AlbumFile.TYPE_IMAGE ? mAlbumFolder : mVideoFolder;
                AlbumFile albumFile = folder.getAlbumFiles().get(mAdapter.getSelectedItem());
                startPreview(albumFile);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        Glide.with(AlbumPickActivity.this).resumeRequests();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        Glide.with(AlbumPickActivity.this).pauseRequests();
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        Glide.with(AlbumPickActivity.this).pauseRequests();
                        break;
                }
            }
        });
        mLayoutLoading.setVisibility(View.VISIBLE);
        AndPermission.with(this)
                .runtime()
                .permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest
                        .permission.WRITE_EXTERNAL_STORAGE)
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> strings) {
                        mLayoutLoading.setVisibility(View.GONE);
                        showShort(R.string.permission_storage);
                    }
                })
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> strings) {
                        getAlbumData();
                    }
                }).start();
    }


    private int getOrientation(Configuration config) {
        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return LinearLayoutManager.VERTICAL;
            case Configuration.ORIENTATION_LANDSCAPE:
                return LinearLayoutManager.HORIZONTAL;
            default:
                throw new AssertionError("This should not be the case.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void onClick(View view) {
        if (R.id.btn_cast == view.getId()) {
            AlbumFolder folder = mMediaType == AlbumFile.TYPE_IMAGE ? mAlbumFolder : mVideoFolder;
            AlbumFile albumFile = folder.getAlbumFiles().get(mAdapter.getSelectedItem());
            startPlayMedia(albumFile, ActionType.cast);
        } else if (R.id.btn_transfer == view.getId()) {
            AlbumFolder folder = mMediaType == AlbumFile.TYPE_IMAGE ? mAlbumFolder : mVideoFolder;
            AlbumFile albumFile = folder.getAlbumFiles().get(mAdapter.getSelectedItem());
            LanLinkSender.Companion.getInstance().castExit(mServiceInfo);
            LanLinkSender.Companion.getInstance().sendMessage(mServiceInfo, getUser(), "user-info");
            startPlayMedia(albumFile, ActionType.store);
        } else if (R.id.ivBack == view.getId()) {
            onBackPressed();
        }
    }

    private UserInfo getUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("leo");
        userInfo.setEmail("xxx@royole.com");
        userInfo.setPhone("+8615229839374");
        userInfo.setAge(18);
        userInfo.setSex(1);
        return userInfo;
    }

    /**
     * 获取相册数据
     */
    private void getAlbumData() {

        Filter<String> mimeFilter = new Filter<String>() {
            @Override
            public boolean filter(String attributes) {
                return attributes == null || attributes.contains("image/gif");
            }
        };

        MediaReader mediaReader = new MediaReader(this, null, mimeFilter, null, false);
        mMediaReadTask = new MediaReadTask(MediaReadTask.FUNCTION_CHOICE_ALBUM, mediaReader, this);
        mMediaReadTask.execute();
    }


    @Override
    public void onScanCallback(int function, ArrayList<AlbumFolder> albumFolders) {
        System.out.println("function:"+function + ",size:"+albumFolders.get(0).getAlbumFiles().size());
        if (function == MediaReadTask.FUNCTION_CHOICE_ALBUM){
            mMediaReadTask = null;
            mLayoutLoading.setVisibility(View.GONE);
            mAlbumFolder = albumFolders.get(0);
            mAlbumFolder.setChecked(true);

            //默认选中第一个
            selectFirstAlbumFile();

            //取出视频文件
            mVideoFolder = new AlbumFolder();
            mVideoFolder.setChecked(false);
            mVideoFolder.setName(getString(R.string.upload_select_album_video));
            for (AlbumFile file : mAlbumFolder.getAlbumFiles()) {
                if (file.getMediaType() == AlbumFile.TYPE_VIDEO) {
                    mVideoFolder.addAlbumFile(file);
                }
            }
            AlbumFolder folder = mMediaType == AlbumFile.TYPE_VIDEO ? mVideoFolder : mAlbumFolder;
            mAdapter.setAlbumFiles(folder.getAlbumFiles());
            mAdapter.notifyDataSetChanged();
            mRecyclerView.scrollToPosition(0);
            AlbumFile albumFile = folder.getAlbumFiles().get(mAdapter.getSelectedItem());
            startPreview(albumFile);
        }
    }

    private void selectFirstAlbumFile() {
        if (mAlbumFolder.getAlbumFiles().size() > 0) {
            for (AlbumFile file : mAlbumFolder.getAlbumFiles()) {
                if (file.getMediaType() == AlbumFile.TYPE_IMAGE) {
                    mAdapter.setVideoFilter(true);
                    mAdapter.setPicFilter(false);
                    file.setChecked(true);
                    mCheckedList.add(file);
                    break;
                }else if (file.getSize() <= VIDEO_LIMIT_SIZE ){
                    mAdapter.setPicFilter(true);
                    mAdapter.setVideoFilter(false);
                    file.setChecked(true);
                    mCheckedList.add(file);
                    break;
                }
            }
        }
    }

    private void selectFirstVideoFile(){
        if (mVideoFolder.getAlbumFiles().size() > 0){
            for (AlbumFile file : mVideoFolder.getAlbumFiles()) {
                if (file.getSize() <= VIDEO_LIMIT_SIZE){
                    mAdapter.setPicFilter(true);
                    mAdapter.setVideoFilter(false);
                    file.setChecked(true);
                    mCheckedList.add(file);
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mServiceInfo != null) {
            //LanLink.Companion.getInstance().sendCastExit(mServiceInfo);
        }

        if (mMediaReadTask != null) {
            mMediaReadTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startPreview(AlbumFile albumFile) {
        View view = null;
        if (albumFile.getMediaType() == AlbumFile.TYPE_IMAGE) {
            mIvPreview.setVisibility(View.VISIBLE);
            mVvPreview.setVisibility(View.INVISIBLE);
            view = mIvPreview;
        } else if (albumFile.getMediaType() == AlbumFile.TYPE_VIDEO) {
            mVvPreview.setVisibility(View.VISIBLE);
            mIvPreview.setVisibility(View.INVISIBLE);
            view = mVvPreview;
        }
        loadPreview(view, albumFile);
    }

    private void loadPreview(View view, AlbumFile item) {
        if (item.getMediaType() == AlbumFile.TYPE_IMAGE) {
            Glide.with(this).load(item.getPath()).into((ImageView)view);
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

    void startPlayMedia(AlbumFile albumFile, ActionType actionType) {
        if (null == mServiceInfo) {
            Toast.makeText(getApplicationContext(), "请连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = albumFile.getPath();

        MediaType type = MediaType.image;
        switch (albumFile.getMediaType()) {
            case AlbumFile.TYPE_IMAGE:
                type = MediaType.image;
                break;
            case AlbumFile.TYPE_VIDEO:
                type = MediaType.video;
                break;
        }
        String name = new File(path).getName();
        String url = LanLinkSender.Companion.getInstance().serveFile(path);
        Media media = new Media(url, type, name);
        TaskInfo taskInfo = new TaskInfo(media, actionType, PlayMode.single, "test-album-name");
        if (ActionType.cast == actionType) {
            LanLinkSender.Companion.getInstance().castMedia(mServiceInfo, path, type);
        } else if (ActionType.store == actionType) {
            LanLinkSender.Companion.getInstance().transferFile(mServiceInfo, path);
        }

    }

    private void showShort(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT);
    }
}
