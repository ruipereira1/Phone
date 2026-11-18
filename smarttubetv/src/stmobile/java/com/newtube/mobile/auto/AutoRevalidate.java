package com.newtube.mobile.auto;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * NEWTUBE-AUTO: asks Android Auto to check NewTube again once the car has connected.
 *
 * <p>When the phone connects, Android Auto checks every car app against the Play Store
 * ({@code CAR.VALIDATOR: isPackageInstalledByPlayCheck}). If the Play Store service is not ready
 * yet, a sideloaded app is denied for the whole drive ({@code Package DENIED}) and only the media
 * entry (sound) is left. Android Auto checks an app again whenever the package changes, so a few
 * seconds after the car binds the media entry, this flips a dummy component on and off: a
 * {@code PACKAGE_CHANGED} with nothing visible to the user, by which time the Play Store answers.
 */
public final class AutoRevalidate {
    private static final String TAG = "NewTubeAuto";
    private static final long[] DELAYS_MS = {6_000, 20_000};
    private static final long MIN_GAP_MS = 60_000;
    private static long sLastConnectAt = -MIN_GAP_MS;

    private AutoRevalidate() {
    }

    /** Android Auto just bound us at a car connection. */
    public static void onCarConnected(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (now - sLastConnectAt < MIN_GAP_MS) return;
        sLastConnectAt = now;
        Context app = context.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        for (long delay : DELAYS_MS) {
            handler.postDelayed(() -> nudge(app), delay);
        }
        Log.i(TAG, "car connected: asking Android Auto to re-check in " + DELAYS_MS[0] / 1000 + " s");
    }

    private static void nudge(Context context) {
        if (AutoSession.isActive()) {
            Log.i(TAG, "re-check skipped: the car screen is already open");
            return;
        }
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName dummy = new ComponentName(context, Marker.class);
            pm.setComponentEnabledSetting(dummy, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(dummy, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Log.i(TAG, "asked Android Auto to re-check NewTube (package changed)");
        } catch (Throwable error) {
            Log.w(TAG, "re-check failed: " + error);
        }
    }

    /** Never receives anything; only its enabled state is flipped. */
    public static class Marker extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }
}
