
package com.atikulsoftware.fundrawer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/*
 * Created Atikul Software
 * Website : https://www.bdtopcoder.xyz
 */

public class NoClickThroughFrameLayout  extends BuildLayerFrameLayout {

    public NoClickThroughFrameLayout(Context context) {
        super(context);
    }

    public NoClickThroughFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoClickThroughFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
