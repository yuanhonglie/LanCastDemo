package com.yhl.cast.server.albumpicker.widget.photoview.scrollerproxy;

import android.annotation.TargetApi;
import android.content.Context;

@TargetApi(14)
public class IcsScroller extends GingerScroller {

    public IcsScroller(Context context) {
        super(context);
    }

    @Override
    public boolean computeScrollOffset() {
        return mScroller.computeScrollOffset();
    }

}
