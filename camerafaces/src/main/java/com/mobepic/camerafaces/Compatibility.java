package com.mobepic.camerafaces;

import java.lang.reflect.Method;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceView;

public class Compatibility {
    private static Method getSupportedPreviewSizes;
    private static Method setZOrderMediaOverlay;

    static {
        initCompatibility();
    };

    private static void initCompatibility() {
        try {
            getSupportedPreviewSizes = Camera.Parameters.class.getMethod("getSupportedPreviewSizes",
                    new Class[] { });
            /* success, this is a newer device */
        } catch (NoSuchMethodException nsme) {
            /* failure, must be older device */
        }
        try {
            setZOrderMediaOverlay = SurfaceView.class.getMethod("setZOrderMediaOverlay",
                    new Class[] { boolean.class });
            /* success, this is a newer device */
        } catch (NoSuchMethodException nsme) {
            /* failure, must be older device */
            nsme.printStackTrace();
        }
    }

    public static List<Size> getSupportedPreviewSizes(Camera.Parameters parameters) {
        if (getSupportedPreviewSizes != null) {
            /* feature is supported */
            try {
                return (List<Size>)getSupportedPreviewSizes.invoke(parameters);
            } catch (Exception e) {
                Log.d("Compatiblity", "getSupportedPreviewSizes not supported because of exception "+e);
            }
        }
        Log.d("Compatiblity", "getSupportedPreviewSizes not supported");
        return null;
    }

    public static void setZOrderMediaOverlay(SurfaceView view,
            boolean b) {
        if (setZOrderMediaOverlay != null) {
            /* feature is supported */
            try {
                setZOrderMediaOverlay.invoke(view, b);
            } catch (Exception e) {
                Log.d("Compatiblity", "getSupportedPreviewSizes not supported because of exception "+e);
            }
        }
    }

    /*
    public static void setActivityTitleStyle(FragmentActivity a) {
        try {
            a.getActionBar().setDisplayShowCustomEnabled(true);
            a.getActionBar().setDisplayShowTitleEnabled(false);
            a.getActionBar().setDisplayUseLogoEnabled(true);
            //this.getActionBar().setCustomView(R.layout.title);
        } catch(Throwable t) {
            // we are probably not android 3.0 (v11) so class not found will be thrown
        }
    }
    */
}