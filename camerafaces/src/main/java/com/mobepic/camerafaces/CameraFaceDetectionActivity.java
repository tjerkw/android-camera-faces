package com.mobepic.camerafaces;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Handler;
import android.util.Log;

/**
 * CameraActivity extended with face detection on the preview frame.
 * It will not affect the framerate, but face detection is slow
 * the rate of face detection is probably slower than the preview img rate.
 */
public abstract class CameraFaceDetectionActivity extends CameraActivity implements
        PreviewCallback {
    protected Camera camera;
    private byte[] preview;
    private int buffLen;
    private int[] rgb;
    private byte[] grayBuff;
    protected int previewWidth;
    protected int previewHeight;
    // scales down the preview frame to speedup face detection (but it will be less accurate)
    protected float DETECTOR_IMG_SCALE = 0.4f;
    // the actual face detector
    private static final int MAX_FACES = 5;

    /* Face Detection Threads */
    private boolean isThreadWorking = false;
    // the detector itself
    private FaceDetectRunnable detector = null;
    // the detector is ran in this executor
    private Executor executor = Executors.newFixedThreadPool(1);
    // used to post back to the ui from the detector
    private Handler handler = new Handler();

    // this listens for the face to be detected
    private FaceDetectionListener listener;
    // wether the face detection is running
    private boolean detecting = true;

    protected void log(String msg) {
        Log.d("CameraFaceDetection", msg);
    }

    public void setFaceDetectionListener(FaceDetectionListener listener) {
        this.listener = listener;
    }

    protected boolean isDetecting() {
        return detecting;
    }

    protected void setDetecting(boolean flag) {
        if (!detecting && flag) {
            // restart the detection loop
            getNextPreviewFrame();
        }
        detecting = flag;
    }

    private Camera.Size getBestSize(int width, int height, List<Size> sizes) {
        Camera.Size result = null;

        for (Camera.Size size : sizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result=size;
                }
                else {
                    int resultArea=result.width * result.height;
                    int newArea=size.width * size.height;

                    if (newArea > resultArea) {
                        result=size;
                    }
                }
            }
        }
        if (result == null) {
            result = sizes.get(0);
        }

        return result;
    }

    protected void onCameraStarted() {
        log("onCameraStarted");
        camera = super.getCamera();
        Parameters p = camera.getParameters();
        Size previewSize = p.getPreviewSize();

        // limit max size of picture to sppedup the app
        Size size = getBestSize(1024*2, 1024*2, p.getSupportedPictureSizes());
        p.setPictureSize(size.width, size.height);

        camera.setParameters(p);

        // setup face detector
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;
        log("PreviewSize: " + previewWidth + "x" + previewHeight);

        int bitsPerPixel = ImageFormat.getBitsPerPixel(p.getPreviewFormat());
        buffLen = previewSize.width * previewSize.height;
        int n = buffLen * bitsPerPixel / 8;
        preview = new byte[n];
        rgb = new int[buffLen];
        grayBuff = new byte[buffLen];

        log("listening for camera preview callbacks");
        /*
        hasNewFaceDetectionApi = false;
         *
        try {
            camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    faces[0].
                    listener.onFacesDetected(faces, faces.length);
                }
            });
        } catch(Throwable t) {
            */
        camera.addCallbackBuffer(preview);
        camera.setPreviewCallbackWithBuffer(this);
        //}
    }

    protected void onCameraStopped() {
        // stops receiving callbacks
        log("stop listening for camera preview callbacks");
        camera.setPreviewCallbackWithBuffer(null);
    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data.length < buffLen) {
            log("preview frame is empty");
            return;
        }
        // run only one analysis thread at one time
        if (!isThreadWorking && detecting) {
            log("We got a previes frame, start detecing");
            isThreadWorking = true;
            // copy only Y buffer
            ByteBuffer bbuffer = ByteBuffer.wrap(data);
            bbuffer.get(grayBuff, 0, buffLen);
            // start thread
            if (detector == null) {
                detector = new FaceDetectRunnable(handler);
            }
            detector.setBuffer(grayBuff);
            executor.execute(detector);
        }
    }

    void getNextPreviewFrame() {
        if (!detecting) {
            return;
        }
        // repaint in ui thread
        log("postInvalidate abd sleep" + view);
        view.postInvalidate();
        // allow for repainting
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (camera!=null) {
            camera.addCallbackBuffer(preview);
        }
    }

    /**
     * This runnable does the actual face detectoin.
     * It should be run in a separe thread to not affect
     * the ui thread. (We use a Executor )
     */
    private class FaceDetectRunnable implements Runnable {
        /* variables */
        private Handler handler;
        private byte[] grayBuff = null;
        // The actual android face detector
        private FaceDetector faceDetector;
        // the results of face detection
        private FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
        // the number of faces that have been detected
        private int nFaces = 0;

        public FaceDetectRunnable(Handler handler) {
            this.handler = handler;
        }

        /* set buffer */
        public void setBuffer(byte[] graybuff) {
            this.grayBuff = graybuff;
        }

        /* run the thread */
        @Override
        public void run() {
            /* face detector only needs grayscale image */
            // grayToRgb(graybuff_,rgbs_); // jni method
            gray8toRGB32(grayBuff, previewWidth, previewHeight, rgb); // java
            // method
            int w = (int) (previewWidth * DETECTOR_IMG_SCALE);
            int h = (int) (previewHeight * DETECTOR_IMG_SCALE);
            Bitmap bmp = Bitmap.createScaledBitmap(Bitmap.createBitmap(rgb,
                    previewWidth, previewHeight, Bitmap.Config.RGB_565), w, h,
                    false);
            if (faceDetector == null) {
                faceDetector = new FaceDetector(w, h, MAX_FACES);
            }
            // reset array;
            for(int i=0;i<faces.length;i++) {
                faces[i] = null;
            }
            nFaces = faceDetector.findFaces(bmp, faces);
            // callback
            if (listener!=null) {
                listener.onFacesDetected(faces, nFaces);
            }
            // post message to UI
            handler.post(new Runnable() {
                public void run() {;
                    // turn off thread lock
                    isThreadWorking = false;
                    getNextPreviewFrame();
                }
            });
        }

        /*
         * convert 8bit grayscale to RGB32bit (fill R,G,B with Y) process may
         * take time and differs according to OS load. (100-1000ms)
         */
        @SuppressWarnings("unused")
        private void gray8toRGB32(byte[] gray8, int width, int height,
                                  int[] rgb_32s) {
            final int endPtr = width * height;
            int ptr = 0;
            while (true) {
                if (ptr == endPtr) {
                    break;
                }
                final int Y = gray8[ptr] & 0xff;
                rgb_32s[ptr] = 0xff000000 + (Y << 16) + (Y << 8) + Y;
                ptr++;
            }
        }
    }

    interface FaceDetectionListener {
        public void onFacesDetected(FaceDetector.Face[] faces, int nFaces);
    }
}