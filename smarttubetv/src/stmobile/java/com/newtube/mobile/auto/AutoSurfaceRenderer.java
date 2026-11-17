package com.newtube.mobile.auto;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

/**
 * NEWTUBE-AUTO: draws on the surface Android Auto gives a navigation app. A private virtual display
 * wraps that surface and a {@link Presentation} on it hosts a normal {@link PlayerView} (fit, black
 * bars) - or, while driving, the cover, title and a "sound only" note.
 */
public final class AutoSurfaceRenderer implements SurfaceCallback, AutoPlayer.Listener {
    private final CarContext mCarContext;
    private final AutoPlayer mPlayer;

    @Nullable private VirtualDisplay mDisplay;
    @Nullable private Presentation mPresentation;
    @Nullable private PlayerView mPlayerView;
    @Nullable private View mInfo;
    @Nullable private ImageView mCover;
    @Nullable private TextView mTitle;
    @Nullable private TextView mNote;
    @Nullable private FrameLayout mRoot;
    @Nullable private String mCoverUrl;
    private String mParkedHint = "";
    private final Rect mVisible = new Rect();
    private int mWidth;
    private int mHeight;

    public AutoSurfaceRenderer(CarContext carContext) {
        mCarContext = carContext;
        mPlayer = AutoPlayer.get(carContext);
        mPlayer.addListener(this);
    }

    /** Shown under the title while video is off: why, and what to do about it. */
    public void setParkedHint(String hint) {
        mParkedHint = hint != null ? hint : "";
        refresh();
    }

    @Override
    public void onSurfaceAvailable(@NonNull SurfaceContainer container) {
        releaseDisplay();
        if (container.getSurface() == null || container.getWidth() <= 0 || container.getHeight() <= 0) {
            return;
        }
        mWidth = container.getWidth();
        mHeight = container.getHeight();
        DisplayManager dm = (DisplayManager) mCarContext.getSystemService(Context.DISPLAY_SERVICE);
        mDisplay = dm.createVirtualDisplay("NewTubeAuto", mWidth, mHeight, container.getDpi(),
                container.getSurface(), 0);
        mPresentation = new Presentation(mCarContext, mDisplay.getDisplay(),
                android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        mPresentation.setContentView(buildViews(mPresentation.getContext()));
        try {
            mPresentation.show();
        } catch (Throwable error) {
            mPresentation = null;
        }
        applyVisibleArea();
        refresh();
    }

    @Override
    public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
        mVisible.set(visibleArea);
        applyVisibleArea();
    }

    @Override
    public void onStableAreaChanged(@NonNull Rect stableArea) {
    }

    @Override
    public void onSurfaceDestroyed(@NonNull SurfaceContainer container) {
        releaseDisplay();
    }

    public void release() {
        mPlayer.removeListener(this);
        releaseDisplay();
    }

    private void releaseDisplay() {
        if (mPlayerView != null) mPlayerView.setPlayer(null);
        if (mPresentation != null) {
            try {
                mPresentation.dismiss();
            } catch (Throwable ignored) {
            }
        }
        mPresentation = null;
        if (mDisplay != null) mDisplay.release();
        mDisplay = null;
        mPlayerView = null;
        mInfo = null;
        mRoot = null;
        mCoverUrl = null;
    }

    private View buildViews(Context context) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);

        PlayerView playerView = new PlayerView(context);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShutterBackgroundColor(Color.BLACK);
        playerView.setKeepContentOnPlayerReset(false);
        root.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER);
        int pad = dp(context, 24);
        info.setPadding(pad, pad, pad, pad);

        ImageView cover = new ImageView(context);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int coverHeight = Math.max(dp(context, 90), mHeight / 3);
        info.addView(cover, new LinearLayout.LayoutParams(coverHeight * 16 / 9, coverHeight));

        TextView title = new TextView(context);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(dp(context, 18), mHeight / 18f));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(context, 16);
        info.addView(title, titleParams);

        TextView note = new TextView(context);
        note.setTextColor(0xFFBDBDBD);
        note.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(dp(context, 14), mHeight / 26f));
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(context, 8);
        info.addView(note, noteParams);

        root.addView(info, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mRoot = root;
        mPlayerView = playerView;
        mInfo = info;
        mCover = cover;
        mTitle = title;
        mNote = note;
        return root;
    }

    private void applyVisibleArea() {
        if (mRoot == null || mVisible.isEmpty() || mWidth <= 0) return;
        // Keep the picture inside the area the car does not cover with its own buttons.
        mRoot.setPadding(mVisible.left, mVisible.top,
                Math.max(0, mWidth - mVisible.right), Math.max(0, mHeight - mVisible.bottom));
    }

    @Override
    public void onAutoPlayerChanged() {
        refresh();
    }

    private void refresh() {
        if (mPlayerView == null || mInfo == null) return;
        boolean video = mPlayer.shouldShowVideo();
        if (video) {
            if (mPlayerView.getPlayer() != mPlayer.getExoPlayer()) {
                mPlayerView.setPlayer(mPlayer.getExoPlayer());
            }
            mPlayerView.setVisibility(View.VISIBLE);
        } else {
            mPlayerView.setPlayer(null);
            mPlayerView.setVisibility(View.INVISIBLE);
        }
        String status = mPlayer.getStatus();
        boolean showInfo = !video || !status.isEmpty();
        mInfo.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        mInfo.setBackgroundColor(video ? 0x99000000 : Color.TRANSPARENT);
        if (!showInfo) return;

        Video current = mPlayer.getVideo();
        mTitle.setText(current != null ? current.title : "NewTube");
        String note;
        if (!status.isEmpty()) {
            note = status;
        } else if (current == null) {
            note = "Escolhe um vídeo na lista";
        } else if (mPlayer.isAudioOnlyByUser()) {
            note = "Só áudio";
        } else {
            note = mParkedHint;
        }
        mNote.setText(note);
        mCover.setVisibility(current != null ? View.VISIBLE : View.GONE);
        String url = current != null ? current.getCardImageUrl() : null;
        if (url != null && !url.equals(mCoverUrl)) {
            mCoverUrl = url;
            try {
                Glide.with(mCover.getContext().getApplicationContext()).load(url).into(mCover);
            } catch (Throwable ignored) {
            }
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
