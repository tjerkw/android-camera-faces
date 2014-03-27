package com.mobepic.camerafaces;

import java.io.IOException;
import java.util.List;

import com.mobepic.camerafaces.R;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * An activity which shows the camera on the background.
 * @author tjerk
 */
public class CameraActivity extends Activity {
    protected boolean isShooting = false;
    protected CameraView cameraView;
    protected View view;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        cameraView = new CameraView(this);

        addContentView(cameraView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        view = getOverlayView();
        if(view != null) {
            addContentView(view, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        }
        if(view!=null) {
            view.bringToFront();
        }
    }

    protected View getOverlayView() {
        return null;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // always ensure the camera is released.
        // sometimes onSurfaceDestroy is not called
        // we can always be sure onPause will be called
        if(cameraView!=null) {
            cameraView.release();
        }
    }

    protected int getWidth() {
        return cameraView == null ? -1 : cameraView.width;
    }

    protected int getHeight() {
        return cameraView == null ? -1 : cameraView.height;
    }

    protected Camera getCamera() {
        if(cameraView!=null) {
            return cameraView.mCamera;
        }
        return null;
    }


    /**
     * Override these methods for camera callbacks
     */
    protected void onCameraStarted() {
    }

    protected void onCameraStopped() {
    }
}

// ----------------------------------------------------------------------

class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    SurfaceHolder mHolder;
    Camera mCamera;
    int width;
    int height;
    private CameraActivity mActivity;

    CameraView(CameraActivity context) {
        super(context);
        this.mActivity = context;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            mCamera = Camera.open();

            int angle;
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            switch (display.getRotation()) {
                case Surface.ROTATION_0: // This is display orientation
                    angle = 90; // This is camera orientation
                    break;
                case Surface.ROTATION_90:
                    angle = 0;
                    break;
                case Surface.ROTATION_180:
                    angle = 270;
                    break;
                case Surface.ROTATION_270:
                    angle = 180;
                    break;
                default:
                    angle = 90;
                    break;
            }
            mCamera.setDisplayOrientation(angle);
            //mCamera.getParameters().setRotation(display.getRotation());

            mCamera.setPreviewDisplay(mHolder);

        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            Toast.makeText(this.getContext(), R.string.error_fail_connect_camera, 2000).show();

        } catch(RuntimeException e) {
            //fail to connect to camera service
            Toast.makeText(this.getContext(), R.string.error_fail_connect_camera, 2000).show();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        this.release();

    }

    void release() {
        if(mCamera!=null) {
            mCamera.stopPreview();
            mActivity.onCameraStopped();
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        width = w;
        height = h;
        if(mCamera!=null) {
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            Camera.Parameters parameters = mCamera.getParameters();

            List<Size> sizes = Compatibility.getSupportedPreviewSizes(parameters);
            if(sizes!=null) {
                Log.i("CameraFa", "number of preview sizes: "+sizes.size());
                for(Size size: sizes) {
                    Log.i("VirtualWeaponActivity", "size "+size.width+"x"+size.height);
                }
                Size optimalSize = getOptimalPreviewSize(sizes, w, h);
                w = optimalSize.width;
                h = optimalSize.height;
            }
            parameters.setPreviewSize(w, h);

            try {
                mCamera.setParameters(parameters);
            } catch(RuntimeException e) {
                // sometimes we get an java.lang.RuntimeException: setParameters failed
            }
            mCamera.startPreview();
            //mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);


            mActivity.onCameraStarted();
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}