// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.bookmarks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.bookmarks.BookmarkBridge.BookmarkItem;
import org.chromium.chrome.browser.util.ViewUtils;
import org.chromium.chrome.browser.widget.TintedImageButton;
import org.chromium.chrome.browser.widget.selection.SelectableItemView;
import org.chromium.components.bookmarks.BookmarkId;

import java.util.List;

/**
 * Common logic for bookmark and folder rows.
 */
abstract class BookmarkRow extends SelectableItemView<BookmarkId> implements BookmarkUIObserver {

    protected ImageView mIconImageView;
    protected TextView mTitleView;
    protected TextView mSubTitleView;
    protected TintedImageButton mMoreIcon;

    protected BookmarkDelegate mDelegate;
    protected BookmarkId mBookmarkId;
    private ListPopupWindow mPopupMenu;
    private boolean mIsAttachedToWindow;

    /**
     * Constructor for inflating from XML.
     */
    public BookmarkRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Updates this row for the given {@link BookmarkId}.
     * @return The {@link BookmarkItem} corresponding the given {@link BookmarkId}.
     */
    BookmarkItem setBookmarkId(BookmarkId bookmarkId) {
        mBookmarkId = bookmarkId;
        BookmarkItem bookmarkItem = mDelegate.getModel().getBookmarkById(bookmarkId);
        clearPopup();

        mMoreIcon.setVisibility(bookmarkItem.isEditable() ? VISIBLE : GONE);
        setChecked(mDelegate.getSelectionDelegate().isItemSelected(bookmarkId));

        super.setItem(bookmarkId);
        return bookmarkItem;
    }

    private void initialize() {
        mDelegate.addUIObserver(this);
        updateSelectionState();
    }

    private void clearPopup() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }
    }

    private void cleanup() {
        clearPopup();
        if (mDelegate != null) mDelegate.removeUIObserver(this);
    }

    private void updateSelectionState() {
        mMoreIcon.setClickable(!mDelegate.getSelectionDelegate().isSelectionEnabled());
    }
    /**
     * Show drop-down menu after user click on more-info icon
     * @param view The anchor view for the menu
     */
    private void showMenu(View view) {
        if (mPopupMenu == null) {
            mPopupMenu = new ListPopupWindow(getContext(), null, 0, R.style.BookmarkMenuStyle);
            mPopupMenu.setAdapter(new ArrayAdapter<String>(
                    getContext(), R.layout.bookmark_popup_item, new String[] {
                            getContext().getString(R.string.bookmark_item_select),
                            getContext().getString(R.string.bookmark_item_edit),
                            getContext().getString(R.string.bookmark_item_move),
                            getContext().getString(R.string.bookmark_item_delete)}) {
                private static final int MOVE_POSITION = 2;

                @Override
                public boolean areAllItemsEnabled() {
                    return false;
                }

                @Override
                public boolean isEnabled(int position) {
                    // In some erroneous states, the popup window might hang around even if the
                    // activity is killed (crbug.com/594213), so null check here.
                    if (mDelegate == null || mDelegate.getModel() == null) return false;
                    if (position == MOVE_POSITION) {
                        BookmarkItem bookmark = mDelegate.getModel().getBookmarkById(mBookmarkId);
                        if (bookmark == null) return false;
                        return bookmark.isMovable();
                    }
                    return true;
                }
                private int[] icons={R.drawable.icon_shuqian_choose,
                        R.drawable.icon_shuqian_edit,
                        R.drawable.icon_shuqian_move,
                        R.drawable.icon_shuqian_del,
                };
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setEnabled(isEnabled(position));

                    Context context = getContext();
                    Drawable leftDrawable = context.getResources().getDrawable(icons[position]);
                    view.setCompoundDrawablesWithIntrinsicBounds(leftDrawable,null,null,null);
                    view.setCompoundDrawablePadding(ViewUtils.dip2px(context,12));
                    return view;
                }
            });
            mPopupMenu.setAnchorView(view);
            Resources resources = getResources();
            mPopupMenu.setWidth(resources.getDimensionPixelSize(
                    R.dimen.bookmark_item_popup_width));
            mPopupMenu.setVerticalOffset(-view.getHeight());
//            mPopupMenu.setVerticalOffset(-resources.getDimensionPixelSize(
//                    R.dimen.bookmark_item_popup_v_offset));
//            mPopupMenu.setHorizontalOffset(-resources.getDimensionPixelSize(
//                    R.dimen.bookmark_item_popup_h_offset));
            mPopupMenu.setModal(true);
            mPopupMenu.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    if (position == 0) {
                        setChecked(mDelegate.getSelectionDelegate().toggleSelectionForItem(
                                mBookmarkId));
                    } else if (position == 1) {
                        BookmarkItem item = mDelegate.getModel().getBookmarkById(mBookmarkId);
                        if (item.isFolder()) {
                            BookmarkAddEditFolderActivity.startEditFolderActivity(
                                    getContext(), item.getId());
                        } else {
                            BookmarkUtils.startEditActivity(getContext(), item.getId());
                        }
                    } else if (position == 2) {
                        BookmarkFolderSelectActivity.startFolderSelectActivity(getContext(),
                                mBookmarkId);
                    } else if (position == 3) {
                        if (mDelegate != null && mDelegate.getModel() != null) {
                            mDelegate.getModel().deleteBookmarks(mBookmarkId);
                        }
                    }
                    // Somehow the on click event can be triggered way after we dismiss the popup.
                    // http://crbug.com/600642
                    if (mPopupMenu != null) mPopupMenu.dismiss();
                }
            });
        }
        mPopupMenu.show();
        mPopupMenu.getListView().setDivider(null);
    }

    // FrameLayout implementations.

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIconImageView = (ImageView) findViewById(R.id.bookmark_image);
        mTitleView = (TextView) findViewById(R.id.title);
        mSubTitleView = (TextView) findViewById(R.id.subTitle);

        mMoreIcon = (TintedImageButton) findViewById(R.id.more);
        mMoreIcon.setVisibility(VISIBLE);
        mMoreIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu(view);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachedToWindow = true;
        if (mDelegate != null) {
            initialize();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
        cleanup();
    }

    // SelectableItem overrides.

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public void onSelectionStateChange(List<BookmarkId> selectedBookmarks) {
        super.onSelectionStateChange(selectedBookmarks);
        updateSelectionState();
    }

    // BookmarkUIObserver implementations.

    @Override
    public void onBookmarkDelegateInitialized(BookmarkDelegate delegate) {
        super.setSelectionDelegate(delegate.getSelectionDelegate());
        mDelegate = delegate;
        if (mIsAttachedToWindow) initialize();
    }

    @Override
    public void onDestroy() {
        cleanup();
    }

    @Override
    public void onFolderStateSet(BookmarkId folder) {}

    @Override
    public void onSearchStateSet() {}
}
