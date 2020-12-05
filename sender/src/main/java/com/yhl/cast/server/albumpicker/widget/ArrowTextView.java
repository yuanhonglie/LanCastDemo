package com.yhl.cast.server.albumpicker.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import com.yhl.cast.server.R;


public class ArrowTextView extends AppCompatTextView {

    public static final int ORIENTATION_DOWN = 0;
    public static final int ORIENTATION_UP = 1;


    /**
     * 记录箭头方向
     */
    private int mOrientation = ORIENTATION_DOWN;

    public ArrowTextView(Context context) {
        super(context);
    }

    public ArrowTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrowTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
        Drawable drawable = null;
        if (mOrientation == ORIENTATION_DOWN){
            drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.icon_photo_takeup, null);
        }else if (mOrientation == ORIENTATION_UP){
            drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.icon_photo_open, null);
        }

        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        }

        setCompoundDrawablePadding(5);
        setCompoundDrawables(null,null,drawable,null);

    }
}
