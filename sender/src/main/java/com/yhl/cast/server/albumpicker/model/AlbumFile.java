package com.yhl.cast.server.albumpicker.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AlbumFile implements Serializable, Parcelable, Comparable<AlbumFile> {

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_IMAGE, TYPE_VIDEO})
    public @interface MediaType {
    }

    /**
     * File path.
     */
    private String mPath;
    /**
     * Folder mName.
     */
    private String mBucketName;
    /**
     * File mime type.
     */
    private String mMimeType;
    /**
     * Add date.
     */
    private long mAddDate;
    /**
     * Latitude
     */
    private float mLatitude;
    /**
     * Longitude.
     */
    private float mLongitude;
    /**
     * Size.
     */
    private long mSize;
    /**
     * Duration.
     */
    private long mDuration;

    /**
     * MediaType.
     */
    private int mMediaType;
    /**
     * Checked.
     */
    private boolean isChecked;
    /**
     * Enabled.
     */
    private boolean isDisable;

    /**
     * Cutting picture
     */
    private String cropPath;

    /**
     * 数据库id
     */
    private int id;

    /**
     * 记录id
     */
    private int recordId;

    /**
     * 是否用户手动裁剪
     */
    private boolean userCrop;

    public AlbumFile() {
    }

    @Override
    public int compareTo(AlbumFile o) {
        long time = o.getAddDate() - getAddDate();
        if (time > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (time < -Integer.MAX_VALUE) {
            return -Integer.MAX_VALUE;
        }
        return (int) time;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AlbumFile) {
            AlbumFile o = (AlbumFile) obj;
            String inPath = o.getPath();
            if (mPath != null && inPath != null) {
                return mPath.equals(inPath);
            }
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return mPath != null ? mPath.hashCode() : super.hashCode();
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getBucketName() {
        return mBucketName;
    }

    public void setBucketName(String bucketName) {
        mBucketName = bucketName;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public long getAddDate() {
        return mAddDate;
    }

    public void setAddDate(long addDate) {
        mAddDate = addDate;
    }

    public float getLatitude() {
        return mLatitude;
    }

    public void setLatitude(float latitude) {
        mLatitude = latitude;
    }

    public float getLongitude() {
        return mLongitude;
    }

    public void setLongitude(float longitude) {
        mLongitude = longitude;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    @MediaType
    public int getMediaType() {
        return mMediaType;
    }

    public void setMediaType(@MediaType int mediaType) {
        mMediaType = mediaType;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isDisable() {
        return isDisable;
    }

    public void setDisable(boolean disable) {
        this.isDisable = disable;
    }

    public String getCropPath() {
        return cropPath;
    }

    public void setCropPath(String cropPath) {
        this.cropPath = cropPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public boolean isUserCrop() {
        return userCrop;
    }

    public void setUserCrop(boolean userCrop) {
        this.userCrop = userCrop;
    }

    protected AlbumFile(Parcel in) {
        mPath = in.readString();
        mBucketName = in.readString();
        mMimeType = in.readString();
        mAddDate = in.readLong();
        mLatitude = in.readFloat();
        mLongitude = in.readFloat();
        mSize = in.readLong();
        mDuration = in.readLong();
        mMediaType = in.readInt();
        isChecked = in.readByte() != 0;
        isDisable = in.readByte() != 0;
        cropPath =in.readString();
        userCrop = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPath);
        dest.writeString(mBucketName);
        dest.writeString(mMimeType);
        dest.writeLong(mAddDate);
        dest.writeFloat(mLatitude);
        dest.writeFloat(mLongitude);
        dest.writeLong(mSize);
        dest.writeLong(mDuration);
        dest.writeInt(mMediaType);
        dest.writeByte((byte) (isChecked ? 1 : 0));
        dest.writeByte((byte) (isDisable ? 1 : 0));
        dest.writeString(cropPath);
        dest.writeByte((byte) (userCrop ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AlbumFile> CREATOR = new Creator<AlbumFile>() {
        @Override
        public AlbumFile createFromParcel(Parcel in) {
            return new AlbumFile(in);
        }

        @Override
        public AlbumFile[] newArray(int size) {
            return new AlbumFile[size];
        }
    };

    @Override
    public String toString() {
        return "AlbumFile{" +
                "mPath='" + mPath + '\'' +
                ", mBucketName='" + mBucketName + '\'' +
                ", mMimeType='" + mMimeType + '\'' +
                ", mAddDate=" + mAddDate +
                ", mLatitude=" + mLatitude +
                ", mLongitude=" + mLongitude +
                ", mSize=" + mSize +
                ", mDuration=" + mDuration +
                ", mMediaType=" + mMediaType +
                ", isChecked=" + isChecked +
                ", isDisable=" + isDisable +
                ", cropPath='" + cropPath + '\'' +
                ", id=" + id +
                ", recordId=" + recordId +
                '}';
    }
}