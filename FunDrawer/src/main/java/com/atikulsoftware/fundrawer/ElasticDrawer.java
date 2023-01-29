
package com.atikulsoftware.fundrawer;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;

/*
 * Created Atikul Software
 * Website : https://www.bdtopcoder.xyz
 */
public abstract class ElasticDrawer extends ViewGroup {

    private static final String TAG = "ElasticDrawer";

    private static final boolean DEBUG = false;
    protected static final int ANIMATION_DELAY = 1000 / 60;

    protected boolean mMenuVisible;
    protected int mMenuSize;

    protected int mTouchBezelSize;

    protected static final Interpolator SMOOTH_INTERPOLATOR = new SmoothInterpolator();
    protected int mTouchSlop;

    protected int mMaxVelocity;
    private Scroller mScroller;
    protected boolean mLayerTypeHardware;
    protected boolean mHardwareLayersEnabled = false;

    protected float mInitialMotionX;

    protected float mInitialMotionY;

    protected float mLastMotionX = -1;

    protected float mLastMotionY = -1;

    protected VelocityTracker mVelocityTracker;

    protected int mCloseEnough;

    private int mPosition;

    private int mResolvedPosition;
    protected int mTouchMode = TOUCH_MODE_BEZEL;
    protected int mTouchSize;

    protected BuildLayerFrameLayout mMenuContainer;

    protected BuildLayerFrameLayout mContentContainer;

    private FunMenuLayout mMenuView;

    protected int mMenuBackground;
    protected float mOffsetPixels;

    private static final int DEFAULT_DRAG_BEZEL_DP = 32;

    private static final int CLOSE_ENOUGH = 3;
    public static final int TOUCH_MODE_NONE = 0;

    public static final int TOUCH_MODE_BEZEL = 1;

    public static final int TOUCH_MODE_FULLSCREEN = 2;

    private OnDrawerStateChangeListener mOnDrawerStateChangeListener;
    protected OnInterceptMoveEventListener mOnInterceptMoveEventListener;

    private static final int DEFAULT_ANIMATION_DURATION = 600;
    protected int mMaxAnimationDuration = DEFAULT_ANIMATION_DURATION;

    public static final int STATE_CLOSED = 0;

    public static final int STATE_CLOSING = 1;

    public static final int STATE_DRAGGING_OPEN = 2;

    public static final int STATE_DRAGGING_CLOSE = 4;

    public static final int STATE_OPENING = 6;

    public static final int STATE_OPEN = 8;

    protected int mDrawerState = STATE_CLOSED;

    protected Bundle mState;

    private static final String STATE_MENU_VISIBLE = "ElasticDrawer.menuVisible";
    protected boolean mIsDragging;
    protected int mActivePointerId = INVALID_POINTER;

    public static final int INVALID_POINTER = -1;

    private float eventY;

    protected boolean isFirstPointUp;

    private final Runnable mDragRunnable = new Runnable() {
        @Override
        public void run() {
            postAnimationInvalidate();
        }
    };

    public ElasticDrawer(Context context) {
        super(context);
    }

    public ElasticDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.elasticDrawerStyle);
    }

    public ElasticDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initDrawer(context, attrs, defStyle);
    }

    @SuppressLint("NewApi")
    protected void initDrawer(Context context, AttributeSet attrs, int defStyle) {
        setWillNotDraw(false);
        setFocusable(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ElasticDrawer);

        mMenuSize = a.getDimensionPixelSize(R.styleable.ElasticDrawer_edMenuSize, dpToPx(240));

        mMenuBackground = a.getColor(R.styleable.ElasticDrawer_edMenuBackground, 0xFFdddddd);

        mTouchBezelSize = a.getDimensionPixelSize(R.styleable.ElasticDrawer_edTouchBezelSize,
                dpToPx(DEFAULT_DRAG_BEZEL_DP));

        mMaxAnimationDuration = a.getInt(R.styleable.ElasticDrawer_edMaxAnimationDuration, DEFAULT_ANIMATION_DURATION);

        final int position = a.getInt(R.styleable.ElasticDrawer_edPosition, 0);
        setPosition(position);
        a.recycle();

        mMenuContainer = new NoClickThroughFrameLayout(context);
        mMenuContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        mContentContainer = new NoClickThroughFrameLayout(context);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaxVelocity = configuration.getScaledMaximumFlingVelocity();

        mScroller = new Scroller(context, SMOOTH_INTERPOLATOR);
        mCloseEnough = dpToPx(CLOSE_ENOUGH);

        mContentContainer.setLayerType(View.LAYER_TYPE_NONE, null);
        mContentContainer.setHardwareLayersEnabled(false);
    }

    protected int dpToPx(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    public interface OnDrawerStateChangeListener {

        void onDrawerStateChange(int oldState, int newState);

        void onDrawerSlide(float openRatio, int offsetPixels);
    }

    public interface OnInterceptMoveEventListener {

        boolean isViewDraggable(View v, int delta, int x, int y);
    }

    class Position {
        // Positions the drawer to the left of the content.
        static final int LEFT = 1;
        // Positions the drawer to the right of the content.
        static final int RIGHT = 2;
        static final int START = 3;
        static final int END = 4;
    }

    protected void updateTouchAreaSize() {
        if (mTouchMode == TOUCH_MODE_BEZEL) {
            mTouchSize = mTouchBezelSize;
        } else if (mTouchMode == TOUCH_MODE_FULLSCREEN) {
            mTouchSize = getMeasuredWidth();
        } else {
            mTouchSize = 0;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() != 2) {
            throw new IllegalStateException(
                    "child count isn't equal to 2 , content and Menu view must be added in xml .");
        }
        View content = getChildAt(0);
        if (content != null) {
            removeView(content);
            mContentContainer.removeAllViews();
            mContentContainer
                    .addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else {
            throw new IllegalStateException(
                    "content view must be added in xml .");
        }
        View menu = getChildAt(0);
        if (menu != null) {
            removeView(menu);
            mMenuView = (FunMenuLayout) menu;
            mMenuView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            mMenuView.setPaintColor(mMenuBackground);
            mMenuView.setMenuPosition(getPosition());
            mMenuContainer.removeAllViews();
            mMenuContainer.addView(menu, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else {
            throw new IllegalStateException(
                    "menu view must be added in xml .");
        }
        addView(mContentContainer, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mMenuContainer, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    protected abstract void onOffsetPixelsChanged(int offsetPixels);

    public void toggleMenu() {
        toggleMenu(true);
    }

    public void toggleMenu(boolean animate) {
        if (mDrawerState == STATE_OPEN || mDrawerState == STATE_OPENING) {
            closeMenu(animate);
        } else if (mDrawerState == STATE_CLOSED || mDrawerState == STATE_CLOSING) {
            openMenu(animate);
        }
    }

    @SuppressWarnings("unused")
    public void openMenu() {
        openMenu(true);
    }

    public abstract void openMenu(boolean animate);

    public abstract void openMenu(boolean animate, float y);


    @SuppressWarnings("unused")
    public void closeMenu() {
        closeMenu(true);
    }

    public abstract void closeMenu(boolean animate);

    public abstract void closeMenu(boolean animate, float y);

    public boolean isMenuVisible() {
        return mMenuVisible;
    }

    @SuppressWarnings("unused")
    public void setMenuSize(final int size) {
        mMenuSize = size;
        if (mDrawerState == STATE_OPEN || mDrawerState == STATE_OPENING) {
            setOffsetPixels(mMenuSize, 0, FunMenuLayout.TYPE_NONE);
        }
        requestLayout();
        invalidate();
    }

    protected void smoothClose(final int eventY) {
        endDrag();
        setDrawerState(STATE_CLOSING);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mOffsetPixels, 0);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setOffsetPixels((Float) animation.getAnimatedValue(), eventY,
                        FunMenuLayout.TYPE_DOWN_SMOOTH);
            }
        });
        valueAnimator.addListener(new FunAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mMenuVisible = false;
                setOffsetPixels(0, 0, FunMenuLayout.TYPE_NONE);
                setDrawerState(STATE_CLOSED);
                stopLayerTranslation();
            }
        });
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new DecelerateInterpolator(4f));
        valueAnimator.start();
    }

    protected void animateOffsetTo(int position, int velocity, boolean animate, float eventY) {
        endDrag();
        final int startX = (int) mOffsetPixels;
        final int dx = position - startX;
        if (dx == 0 || !animate) {
            setOffsetPixels(position, 0, FunMenuLayout.TYPE_NONE);
            setDrawerState(position == 0 ? STATE_CLOSED : STATE_OPEN);
            stopLayerTranslation();
            return;
        }
        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000.f * Math.abs((float) dx / velocity));
        } else {
            duration = (int) (600.f * Math.abs((float) dx / mMenuSize));
        }
        duration = Math.min(duration, mMaxAnimationDuration);
        animateOffsetTo(position, duration, eventY);
    }

    protected void animateOffsetTo(int position, int duration, float eventY) {
        final int startX = (int) mOffsetPixels;
        final int dx = position - startX;
        if (getPosition() == Position.LEFT) {
            if (dx > 0) {
                setDrawerState(STATE_OPENING);
            } else {
                setDrawerState(STATE_CLOSING);
            }
        } else {
            if (dx > 0) {
                setDrawerState(STATE_CLOSING);
            } else {
                setDrawerState(STATE_OPENING);
            }
        }
        mScroller.startScroll(startX, 0, dx, 0, duration);
        this.eventY = eventY;
        startLayerTranslation();
        postAnimationInvalidate();
    }

    protected void setOffsetPixels(float offsetPixels, float eventY, int type) {
        final int oldOffset = (int) mOffsetPixels;
        final int newOffset = (int) offsetPixels;

        mOffsetPixels = offsetPixels;
        mMenuView.setClipOffsetPixels(mOffsetPixels, eventY, type);
        if (newOffset != oldOffset) {
            onOffsetPixelsChanged(newOffset);
            mMenuVisible = newOffset != 0;

            // Notify any attached listeners of the current open ratio
            final float openRatio = ((float) Math.abs(newOffset)) / mMenuSize;
            dispatchOnDrawerSlide(openRatio, newOffset);
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (getPosition() != mResolvedPosition) {
            mResolvedPosition = getPosition();
            setOffsetPixels(mOffsetPixels * -1, 0, FunMenuLayout.TYPE_NONE);
        }

        requestLayout();
        invalidate();
    }

    private void setPosition(int position) {
        mPosition = position;
        mResolvedPosition = getPosition();
    }

    protected int getPosition() {
        final int layoutDirection = ViewHelper.getLayoutDirection(this);
        switch (mPosition) {
            case Position.START:
                if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                    return Position.RIGHT;
                } else {
                    return Position.LEFT;
                }
            case Position.END:
                if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                    return Position.LEFT;
                } else {
                    return Position.RIGHT;
                }
        }
        return mPosition;
    }


    public void setOnDrawerStateChangeListener(OnDrawerStateChangeListener listener) {
        mOnDrawerStateChangeListener = listener;
    }


    @SuppressWarnings("unused")
    public void setOnInterceptMoveEventListener(OnInterceptMoveEventListener listener) {
        mOnInterceptMoveEventListener = listener;
    }

    @SuppressWarnings("unused")
    public void setMaxAnimationDuration(int duration) {
        mMaxAnimationDuration = duration;
    }

    @SuppressWarnings("unused")
    public ViewGroup getMenuContainer() {
        return mMenuContainer;
    }


    @SuppressWarnings("unused")
    public ViewGroup getContentContainer() {
        return mContentContainer;
    }

    @SuppressWarnings("unused")
    public int getDrawerState() {
        return mDrawerState;
    }

    protected void setDrawerState(int state) {
        if (state != mDrawerState) {
            final int oldState = mDrawerState;
            mDrawerState = state;
            if (mOnDrawerStateChangeListener != null) {
                mOnDrawerStateChangeListener.onDrawerStateChange(oldState, state);
            }
            if (DEBUG) {
                logDrawerState(state);
            }
        }
    }

    protected void logDrawerState(int state) {
        switch (state) {
            case STATE_CLOSED:
                Log.d(TAG, "[DrawerState] STATE_CLOSED");
                break;

            case STATE_CLOSING:
                Log.d(TAG, "[DrawerState] STATE_CLOSING");
                break;

            case STATE_DRAGGING_CLOSE:
                Log.d(TAG, "[DrawerState] STATE_DRAGGING_CLOSE");
                break;
            case STATE_DRAGGING_OPEN:
                Log.d(TAG, "[DrawerState] STATE_DRAGGING_OPEN");
                break;

            case STATE_OPENING:
                Log.d(TAG, "[DrawerState] STATE_OPENING");
                break;

            case STATE_OPEN:
                Log.d(TAG, "[DrawerState] STATE_OPEN");
                break;
            default:
                Log.d(TAG, "[DrawerState] Unknown: " + state);
        }
    }


    public void setTouchMode(int mode) {
        if (mTouchMode != mode) {
            mTouchMode = mode;
            updateTouchAreaSize();
        }
    }

    @Override
    public void postOnAnimation(Runnable action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.postOnAnimation(action);
        } else {
            postDelayed(action, ANIMATION_DELAY);
        }
    }

    protected void dispatchOnDrawerSlide(float openRatio, int offsetPixels) {
        if (mOnDrawerStateChangeListener != null) {
            mOnDrawerStateChangeListener.onDrawerSlide(openRatio, offsetPixels);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    void saveState(Bundle state) {
        final boolean menuVisible = mDrawerState == STATE_OPEN || mDrawerState == STATE_OPENING;
        state.putBoolean(STATE_MENU_VISIBLE, menuVisible);
    }

    public void restoreState(Parcelable in) {
        mState = (Bundle) in;
        final boolean menuOpen = mState.getBoolean(STATE_MENU_VISIBLE);
        if (menuOpen) {
            openMenu(false);
        } else {
            setOffsetPixels(0, 0, FunMenuLayout.TYPE_NONE);
        }
        mDrawerState = menuOpen ? STATE_OPEN : STATE_CLOSED;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        if (mState == null) {
            mState = new Bundle();
        }
        saveState(mState);

        state.mState = mState;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        restoreState(savedState.mState);
    }

    static class SavedState extends BaseSavedState {

        Bundle mState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressLint("ParcelClassLoader")
        SavedState(Parcel in) {
            super(in);
            mState = in.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(mState);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    protected float getXVelocity(VelocityTracker velocityTracker) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            return velocityTracker.getXVelocity(mActivePointerId);
        }

        return velocityTracker.getXVelocity();
    }

    protected boolean canChildrenScroll(int dx, int x, int y) {
        boolean canScroll = false;

        switch (getPosition()) {
            case Position.LEFT:
            case Position.RIGHT:
                if (!mMenuVisible) {
                    canScroll = canChildScrollHorizontally(mContentContainer, false, dx,
                            x - ViewHelper.getLeft(mContentContainer), y - ViewHelper.getTop(mContentContainer));
                } else {
                    canScroll = canChildScrollHorizontally(mMenuContainer, false, dx,
                            x - ViewHelper.getLeft(mMenuContainer), y - ViewHelper.getTop(mContentContainer));
                }
                break;
        }

        return canScroll;
    }

    protected boolean canChildScrollHorizontally(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;

            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);

                final int childLeft = child.getLeft() + supportGetTranslationX(child);
                final int childRight = child.getRight() + supportGetTranslationX(child);
                final int childTop = child.getTop() + supportGetTranslationY(child);
                final int childBottom = child.getBottom() + supportGetTranslationY(child);

                if (x >= childLeft && x < childRight && y >= childTop && y < childBottom
                        && canChildScrollHorizontally(child, true, dx, x - childLeft, y - childTop)) {
                    return true;
                }
            }
        }

        return checkV && mOnInterceptMoveEventListener.isViewDraggable(v, dx, x, y);
    }

    private int supportGetTranslationY(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return (int) v.getTranslationY();
        }

        return 0;
    }

    private int supportGetTranslationX(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return (int) v.getTranslationX();
        }

        return 0;
    }

    protected boolean isCloseEnough() {
        return Math.abs(mOffsetPixels) <= mCloseEnough;
    }


    private void postAnimationInvalidate() {
        if (mScroller.computeScrollOffset()) {
            final int oldX = (int) mOffsetPixels;
            final int x = mScroller.getCurrX();

            if (x != oldX) {
                if (mDrawerState == STATE_OPENING) {
                    setOffsetPixels(x, eventY, FunMenuLayout.TYPE_UP_AUTO);
                } else if (mDrawerState == STATE_CLOSING) {
                    setOffsetPixels(x, eventY, FunMenuLayout.TYPE_DOWN_AUTO);
                }
            }
            if (x != mScroller.getFinalX()) {
                postOnAnimation(mDragRunnable);
                return;
            }
        }
        if (mDrawerState == STATE_OPENING) {
            completeAnimation();
        } else if (mDrawerState == STATE_CLOSING) {
            mScroller.abortAnimation();
            final int finalX = mScroller.getFinalX();
            mMenuVisible = finalX != 0;
            setOffsetPixels(finalX, 0, FunMenuLayout.TYPE_NONE);
            setDrawerState(finalX == 0 ? STATE_CLOSED : STATE_OPEN);
            stopLayerTranslation();
        }

    }


    private void completeAnimation() {
        mScroller.abortAnimation();
        final int finalX = mScroller.getFinalX();
        flowDown(finalX);
    }

    private void flowDown(final int finalX) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMenuView.setUpDownFraction(animation.getAnimatedFraction());
            }
        });
        valueAnimator.addListener(new FunAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mDrawerState == STATE_OPENING) {
                    mMenuVisible = finalX != 0;
                    setOffsetPixels(finalX, 0, FunMenuLayout.TYPE_NONE);
                    setDrawerState(finalX == 0 ? STATE_CLOSED : STATE_OPEN);
                    stopLayerTranslation();
                }
            }
        });
        valueAnimator.setDuration(300);
        valueAnimator.setInterpolator(new OvershootInterpolator(4f));
        valueAnimator.start();
    }


    protected void endDrag() {
        mIsDragging = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    protected void stopAnimation() {
        removeCallbacks(mDragRunnable);
        mScroller.abortAnimation();
        stopLayerTranslation();
    }


    @SuppressLint("NewApi")
    protected void startLayerTranslation() {
        if (mHardwareLayersEnabled && !mLayerTypeHardware) {
            mLayerTypeHardware = true;
            mContentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mMenuContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }


    @SuppressLint("NewApi")
    protected void stopLayerTranslation() {
        if (mLayerTypeHardware) {
            mLayerTypeHardware = false;
            mContentContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            mMenuContainer.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    @SuppressWarnings("unused")
    public void setTouchBezelSize(int size) {
        mTouchBezelSize = size;
    }

    @SuppressWarnings("unused")
    public int getTouchBezelSize() {
        return mTouchBezelSize;
    }

    @SuppressWarnings("unused")
    public void setHardwareLayerEnabled(boolean enabled) {
        if (enabled != mHardwareLayersEnabled) {
            mHardwareLayersEnabled = enabled;
            mMenuContainer.setHardwareLayersEnabled(enabled);
            mContentContainer.setHardwareLayersEnabled(enabled);
            stopLayerTranslation();
        }
    }

}
