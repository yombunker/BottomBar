package com.roughike.bottombar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
 * BottomBar library for Android
 * Copyright (c) 2016 Iiro Krankka (http://github.com/roughike).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class BottomBar extends FrameLayout implements View.OnClickListener {
    private static final long ANIMATION_DURATION = 150;
    private static final int MAX_FIXED_TAB_COUNT = 3;

    private static final String STATE_CURRENT_SELECTED_TAB = "com.roughike.bottombar.STATE_CURRENT_SELECTED_TAB";
    private static final String TAG_BOTTOM_BAR_VIEW_INACTIVE = "BOTTOM_BAR_VIEW_INACTIVE";
    private static final String TAG_BOTTOM_BAR_VIEW_ACTIVE = "BOTTOM_BAR_VIEW_ACTIVE";

    private Context mContext;

    private FrameLayout mUserContentContainer;
    private LinearLayout mItemContainer;

    private int mPrimaryColor;
    private int mInActiveColor;
    private int mWhiteColor;

    private int mTwoDp;
    private int mTenDp;
    private int mMaxFixedItemWidth;

    private OnTabSelectedListener mListener;
    private int mCurrentTabPosition;
    private boolean mIsShiftingMode;

    public BottomBar(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public BottomBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        mPrimaryColor = MiscUtils.getColor(mContext, R.attr.colorPrimary);
        mInActiveColor = ContextCompat.getColor(mContext, R.color.bb_inActiveBottomBarItemColor);
        mWhiteColor = ContextCompat.getColor(mContext, R.color.white);

        mTwoDp = MiscUtils.dpToPixel(mContext, 2);
        mTenDp = MiscUtils.dpToPixel(mContext, 10);
        mMaxFixedItemWidth = MiscUtils.dpToPixel(mContext, 168);

        initializeViews();
    }

    private void initializeViews() {
        ViewGroup.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);

        RelativeLayout itemContainerRoot = (RelativeLayout) View.inflate(mContext,
                R.layout.bb_bottom_bar_item_container, null);

        mUserContentContainer = (FrameLayout) itemContainerRoot.findViewById(R.id.bb_user_content_container);
        mItemContainer = (LinearLayout) itemContainerRoot.findViewById(R.id.bb_bottom_bar_item_container);

        addView(itemContainerRoot, params);
    }

    protected FrameLayout getUserContainer() {
        return mUserContentContainer;
    }

    /**
     * Set tabs for this BottomBar.
     * <p/>
     * Doesn't currently support more than 3 items per the Material Design
     * specs.
     *
     * @param bottomBarTabs an array of {@link BottomBarTab} objects.
     */
    public void setItems(BottomBarTab... bottomBarTabs) {
        clearItems();

        int index = 0;
        int biggestWidth = 0;
        mIsShiftingMode = MAX_FIXED_TAB_COUNT < bottomBarTabs.length;

        if (mIsShiftingMode) {
            mItemContainer.setBackgroundColor(mPrimaryColor);
        }

        View[] viewsToAdd = new View[bottomBarTabs.length];

        for (BottomBarTab bottomBarTab : bottomBarTabs) {
            ViewGroup bottomBarView = (ViewGroup) View.inflate(mContext, mIsShiftingMode ?
                    R.layout.bb_bottom_bar_item_shifting : R.layout.bb_bottom_bar_item_fixed, null);

            ImageView icon = (ImageView) bottomBarView.findViewById(R.id.bb_bottom_bar_icon);
            TextView title = (TextView) bottomBarView.findViewById(R.id.bb_bottom_bar_title);

            icon.setImageDrawable(bottomBarTab.getIcon(mContext));
            title.setText(bottomBarTab.getTitle(mContext));

            if (mIsShiftingMode) {
                icon.setColorFilter(mWhiteColor);
            }

            if (index == mCurrentTabPosition) {
                selectTab(bottomBarView, false);
            } else {
                unselectTab(bottomBarView, false);
            }

            if (bottomBarView.getWidth() > biggestWidth) {
                biggestWidth = bottomBarView.getWidth();
            }

            bottomBarView.setOnClickListener(this);
            viewsToAdd[index] = bottomBarView;
            index++;
        }

        int screenWidth = MiscUtils.getScreenWidth(mContext);
        int proposedItemWidth = Math.min(
                MiscUtils.dpToPixel(mContext, screenWidth / bottomBarTabs.length),
                mMaxFixedItemWidth
        );

        LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(proposedItemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

        for (View bottomBarView : viewsToAdd) {
            bottomBarView.setLayoutParams(params);
            mItemContainer.addView(bottomBarView);
        }
    }

    /**
     * Set a listener that gets fired when the selected item changes.
     *
     * @param listener a listener for monitoring changes in tab selection.
     */
    public void setOnItemSelectedListener(OnTabSelectedListener listener) {
        mListener = listener;
    }

    private void selectTab(ViewGroup bottomBarView, boolean animate) {
        bottomBarView.setTag(TAG_BOTTOM_BAR_VIEW_ACTIVE);
        ImageView icon = (ImageView) bottomBarView.findViewById(R.id.bb_bottom_bar_icon);
        TextView title = (TextView) bottomBarView.findViewById(R.id.bb_bottom_bar_title);

        if (!mIsShiftingMode) {
            icon.setColorFilter(mPrimaryColor);
            title.setTextColor(mPrimaryColor);
        }

        int translationY = mIsShiftingMode ? mTenDp : mTwoDp;

        if (animate) {
            title.animate()
                    .setDuration(ANIMATION_DURATION)
                    .scaleX(1)
                    .scaleY(1)
                    .start();
            bottomBarView.animate()
                    .setDuration(ANIMATION_DURATION)
                    .translationY(-translationY)
                    .start();

            if (mIsShiftingMode) {
                icon.animate()
                        .setDuration(ANIMATION_DURATION)
                        .alpha(1.0f)
                        .start();
            }
        } else {
            title.setScaleX(1);
            title.setScaleY(1);
            bottomBarView.setTranslationY(-translationY);

            if (mIsShiftingMode) {
                icon.setAlpha(1.0f);
            }
        }
    }

    private void unselectTab(ViewGroup bottomBarView, boolean animate) {
        bottomBarView.setTag(TAG_BOTTOM_BAR_VIEW_INACTIVE);
        ImageView icon = (ImageView) bottomBarView.findViewById(R.id.bb_bottom_bar_icon);
        TextView title = (TextView) bottomBarView.findViewById(R.id.bb_bottom_bar_title);

        if (!mIsShiftingMode) {
            icon.setColorFilter(mInActiveColor);
            title.setTextColor(mInActiveColor);
        }

        float scale = mIsShiftingMode ? 0 : 0.86f;

        if (animate) {
            title.animate()
                    .setDuration(ANIMATION_DURATION)
                    .scaleX(scale)
                    .scaleY(scale)
                    .start();
            bottomBarView.animate()
                    .setDuration(ANIMATION_DURATION)
                    .translationY(0)
                    .start();

            if (mIsShiftingMode) {
                icon.animate()
                        .setDuration(ANIMATION_DURATION)
                        .alpha(0.6f)
                        .start();
            }
        } else {
            title.setScaleX(scale);
            title.setScaleY(scale);
            bottomBarView.setTranslationY(0);

            if (mIsShiftingMode) {
                icon.setAlpha(0.6f);
            }
        }
    }

    private void clearItems() {
        int childCount = mItemContainer.getChildCount();

        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                mItemContainer.removeView(mItemContainer.getChildAt(i));
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag().equals(TAG_BOTTOM_BAR_VIEW_INACTIVE)) {
            unselectTab((ViewGroup) findViewWithTag(TAG_BOTTOM_BAR_VIEW_ACTIVE), true);
            selectTab((ViewGroup) v, true);

            if (mListener != null) {
                int position = 0;

                for (int i = 0; i < mItemContainer.getChildCount(); i++) {
                    View candidate = mItemContainer.getChildAt(i);

                    if (candidate.getTag().equals(TAG_BOTTOM_BAR_VIEW_ACTIVE)) {
                        position = i;
                        break;
                    }
                }

                mListener.onItemSelected(position);
                mCurrentTabPosition = position;
            }
        }
    }

    private void selectTabAtPosition(int position) {
        unselectTab((ViewGroup) mItemContainer.findViewWithTag(TAG_BOTTOM_BAR_VIEW_ACTIVE), false);
        selectTab((ViewGroup) mItemContainer.getChildAt(position), false);

        if (mListener != null) {
            mListener.onItemSelected(position);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SELECTED_TAB, mCurrentTabPosition);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentTabPosition = savedInstanceState.getInt(STATE_CURRENT_SELECTED_TAB);
        }
    }

    public static BottomBar bind(Activity activity, @LayoutRes int layoutRes, Bundle savedInstanceState) {
        BottomBar bottomBar = new BottomBar(activity);
        bottomBar.onRestoreInstanceState(savedInstanceState);

        View.inflate(activity, layoutRes, bottomBar.getUserContainer());
        activity.setContentView(bottomBar);

        return bottomBar;
    }
}
