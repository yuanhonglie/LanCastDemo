package com.yhl.cast.server.albumpicker.data;

import android.os.AsyncTask;

import com.yhl.cast.server.albumpicker.model.AlbumFolder;

import java.util.ArrayList;

/**
 * add by tali
 */
public class MediaReadTask extends AsyncTask<Void, Void, MediaReadTask.ResultWrapper> {

    public static final int FUNCTION_CHOICE_IMAGE = 0;
    public static final int FUNCTION_CHOICE_VIDEO = 1;
    public static final int FUNCTION_CHOICE_ALBUM = 2;

    public interface Callback {
        /**
         * Callback the results.
         *
         * @param albumFolders album folder list.
         */
        void onScanCallback(int function, ArrayList<AlbumFolder> albumFolders);
    }

    static class ResultWrapper {
        private ArrayList<AlbumFolder> mAlbumFolders;
    }

    private int mFunction;
    private MediaReader mMediaReader;
    private Callback mCallback;

    public MediaReadTask(int function,MediaReader mediaReader, Callback callback) {
        this.mFunction = function;
        this.mMediaReader = mediaReader;
        this.mCallback = callback;
    }

    @Override
    protected ResultWrapper doInBackground(Void... params) {
        ArrayList<AlbumFolder> albumFolders;
        switch (mFunction) {
            case FUNCTION_CHOICE_IMAGE:
                albumFolders = mMediaReader.getAllImage();
                break;

            case FUNCTION_CHOICE_VIDEO:
                albumFolders = mMediaReader.getAllVideo();
                break;

            case FUNCTION_CHOICE_ALBUM:
                albumFolders = mMediaReader.getAllMedia();
                break;

            default:
                throw new AssertionError("This should not be the case.");
        }

        ResultWrapper wrapper = new ResultWrapper();
        wrapper.mAlbumFolders = albumFolders;
        return wrapper;
    }

    @Override
    protected void onPostExecute(ResultWrapper wrapper) {
        mCallback.onScanCallback(mFunction,wrapper.mAlbumFolders);
    }
}