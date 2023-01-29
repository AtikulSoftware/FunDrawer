
package com.atikulsoftware.fundrawer;

import android.view.animation.Interpolator;

/*
 * Created Atikul Software
 * Website : https://www.bdtopcoder.xyz
 */

class SmoothInterpolator implements Interpolator {

    @Override
    public float getInterpolation(float t) {
        t -= 1.0f;
        return t * t * t * t * t + 1.0f;
    }
}
