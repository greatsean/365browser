// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Region;
import android.support.annotation.DrawableRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mogoweb.browser.R;

/**
 * View-related utility methods.
 */
public class ViewUtils {
    private static final int[] sLocationTmp = new int[2];

    /**
     * Invalidates a view and all of its descendants.
     */
    private static void recursiveInvalidate(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    recursiveInvalidate(child);
                }
            }
        }
    }

    /**
     * Sets the enabled property of a View and all of its descendants.
     */
    public static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    /**
     * Captures a bitmap of a View and draws it to a Canvas.
     */
    public static void captureBitmap(View view, Canvas canvas) {
        // Invalidate all the descendants of view, before calling view.draw(). Otherwise, some of
        // the descendant views may optimize away their drawing. http://crbug.com/415251
        recursiveInvalidate(view);
        view.draw(canvas);
    }

    /**
     * Return the position of {@code childView} relative to {@code rootView}.  {@code childView}
     * must be a child of {@code rootView}.  This returns the relative layout position, which does
     * not include translations.
     *
     * @param rootView    The parent of {@code childView} to calculate the position relative to.
     * @param childView   The {@link View} to calculate the position of.
     * @param outPosition The resulting position with the format [x, y].
     */
    public static void getRelativeLayoutPosition(View rootView, View childView, int[] outPosition) {
        assert outPosition.length == 2;
        outPosition[0] = 0;
        outPosition[1] = 0;
        if (rootView == null || childView == rootView) return;
        while (childView != null) {
            outPosition[0] += childView.getLeft();
            outPosition[1] += childView.getTop();
            if (childView.getParent() == rootView) break;
            childView = (View) childView.getParent();
        }
    }

    /**
     * Return the position of {@code childView} relative to {@code rootView}.  {@code childView}
     * must be a child of {@code rootView}.  This returns the relative draw position, which includes
     * translations.
     *
     * @param rootView    The parent of {@code childView} to calculate the position relative to.
     * @param childView   The {@link View} to calculate the position of.
     * @param outPosition The resulting position with the format [x, y].
     */
    public static void getRelativeDrawPosition(View rootView, View childView, int[] outPosition) {
        assert outPosition.length == 2;
        outPosition[0] = 0;
        outPosition[1] = 0;
        if (rootView == null || childView == rootView) return;
        while (childView != null) {
            outPosition[0] += childView.getX();
            outPosition[1] += childView.getY();
            if (childView.getParent() == rootView) break;
            childView = (View) childView.getParent();
        }
    }

    /**
     * Helper for overriding {@link ViewGroup#gatherTransparentRegion} for views that are fully
     * opaque and have children extending beyond their bounds. If the transparent region
     * optimization is turned on (which is the case whenever the view hierarchy contains a
     * SurfaceView somewhere), the children might otherwise confuse the SurfaceFlinger.
     */
    public static void gatherTransparentRegionsForOpaqueView(View view, Region region) {
        view.getLocationInWindow(sLocationTmp);
        region.op(sLocationTmp[0], sLocationTmp[1],
                sLocationTmp[0] + view.getRight() - view.getLeft(),
                sLocationTmp[1] + view.getBottom() - view.getTop(), Region.Op.DIFFERENCE);
    }

    /**
     * Sets the background of a view to the given 9-patch resource and restores its padding. This
     * works around a bug in Android where the padding is lost when a 9-patch resource is applied
     * programmatically.
     */
    public static void setNinePatchBackgroundResource(View view, @DrawableRes int resource) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        view.setBackgroundResource(resource);
        view.setPadding(left, top, right, bottom);
    }

    private static final String TAG = "ViewUtils";

    public static Toolbar initToolbar(AppCompatActivity activity, boolean showBackbtn) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        TextView titleTv = (TextView) toolbar.findViewById(R.id.toolbar_title);
        activity.setSupportActionBar(toolbar);
        CharSequence title = activity.getTitle();
        titleTv.setText(title);
        Log.i(TAG, "initToolbar: " + title);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(showBackbtn);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setOverflowIcon(activity.getResources().getDrawable(R.drawable.icon_yinchangxiang));
        return toolbar;
    }

    public static Toolbar initToolbar(AppCompatActivity activity) {
        return initToolbar(activity, true);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

}
