package com.mobepic.camerafaces;

import java.io.File;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.mobepic.camerafaces.model.Face;
import com.mobepic.camerafaces.model.Faces;

import static android.view.View.OnClickListener;

/**
 * Renders the chosen image (face) on top of the faces detected in the
 * camera result.
 */
public class CameraFacesActivity extends CameraFaceDetectionActivity implements OnClickListener {
    private Face[] faces;
    private Bitmap[] faceBitmaps = null;
    private FaceBoxView faceBoxView;
    private boolean facesLoaded = false;
    private ProgressDialog progressDialog;
    private ImageView shutter;

    // animations
    private Animation shutterIn;
    private Animation shutterOut;
    private Animation textIn;
    private Animation textOut;
    private boolean animating = false;

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }
    };
    private View noFacesDetectedView;
    private ImageButton toggleCamera;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading");
        progressDialog.setCancelable(false);
        progressDialog.show();

        final int[] chosenFaces = this.getIntent().getIntArrayExtra("faces");

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                // TODO: offload this to a loader or async task
                Faces facesDb = new Faces();
                if (chosenFaces.length == 0) {
                    faces = new Face[1];
                    faces[1] = facesDb.getRandom();
                } else {
                    int n = chosenFaces.length;
                    faces = new Face[n];
                    faceBitmaps = new Bitmap[n];
                    for (int i=0;i<chosenFaces.length;i++) {
                        faces[i] = facesDb.getFace(chosenFaces[i]);
                        faceBitmaps[i] = BitmapFactory.decodeResource(getResources(), faces[i].faceResId);
                    }
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void x) {
                facesLoaded = true;
                if (progressDialog != null) {
                    progressDialog.hide();
                }
                hideShutter();

            }
        };
        task.execute();
    }

    protected View getOverlayView() {
        View view = this.getLayoutInflater().inflate(R.layout.camera_overlay, null);
        shutter = (ImageView) view.findViewById(R.id.shutter);
        shutter.setOnClickListener(this);
        shutter.setVisibility(View.GONE);

        toggleCamera = (ImageButton) view.findViewById(R.id.toggle_front_back_camera);
        if (!cameraView.hasFrontCamera()) {
            toggleCamera.setVisibility(View.GONE);
        } else {
            toggleCamera.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraView.toggleFrontBackCamera();
                }
            });
        }

        noFacesDetectedView = view.findViewById(R.id.no_faces_detected);
        noFacesDetectedView.setVisibility(View.GONE);

        FrameLayout frame = (FrameLayout) view.findViewById(R.id.frame);

        faceBoxView = new FaceBoxView(this);
        this.setFaceDetectionListener(faceBoxView);

        frame.addView(faceBoxView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        Animation.AnimationListener animatingTracker = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        // setup animations
        shutterIn = AnimationUtils.loadAnimation(this, R.anim.shutter_in);
        shutterIn.setAnimationListener(animatingTracker);
        shutterOut = AnimationUtils.loadAnimation(this, R.anim.shutter_out);
        shutterOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                shutter.setVisibility(View.GONE);
                animating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        textIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        textIn.setAnimationListener(animatingTracker);
        textOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
        textOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animating = true;

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                noFacesDetectedView.setVisibility(View.GONE);
                animating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return view;
    }

    @Override
    public void onClick(View view) {
        takeScreenshot();
    }

    private void takeScreenshot() {

        if (!facesLoaded) {
            // should never occur since the button that triggers
            // this is hidden
            return;
        }
        shutter.setEnabled(false);
        onlyHideShutter();
        this.setDetecting(false);

        progressDialog.show();
        progressDialog.setMessage("Taking picture");

        // do in thread to prevent ANR
        try {
            camera.takePicture(shutterCallback, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(final byte[] data, Camera camera) {

                    if (data == null) {
                        Toast.makeText(CameraFacesActivity.this,
                                R.string.toast_camera_data_null, Toast.LENGTH_LONG)
                                .show();
                    } else {

                        AsyncTask<Void, CharSequence, Uri> task = new AsyncTask<Void, CharSequence, Uri>() {

                            @Override
                            protected Uri doInBackground(Void... voids) {

                                // TODO: this might throw an OutOfMemoryError
                                this.publishProgress("Creating picture");
                                // Get camera bitmap and scale it to appropriate size
                                Bitmap cameraBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                Bitmap result = cameraBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                cameraBitmap.recycle();

                                Canvas c = new Canvas(result);
                                faceBoxView.drawFaces(c, result.getWidth(), result.getHeight(), false);

                                this.publishProgress("Saving picture");
                                Uri mediaUri = saveBitmap(result);
                                result.recycle();
                                return mediaUri;
                            }


                            @Override
                            protected void onProgressUpdate(CharSequence... progress) {
                                progressDialog.setMessage(progress[0]);
                            }

                            @Override
                            protected void onPostExecute(Uri mediaUri) {
                                progressDialog.hide();
                                log("Sharing picture");
                                if (mediaUri!=null) {
                                    CameraFacesActivity.this.onPictureTaken(mediaUri);
                                }
                            }
                        };

                        task.execute();
                    }
                }

            });
        } catch(Exception e) {
            e.printStackTrace();
            progressDialog.setMessage("Picture taking failed!");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgress(100);
            progressDialog.setMax(100);
            //progressDialog.hide();
        }
    }

    private Uri saveBitmap(Bitmap bitmap) {
        CharSequence timeString = DateFormat.format(
                "MM-dd-y_hh_mm_ss", System.currentTimeMillis());
        String fileName = "derp"
                + "_" + timeString + ".png";

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            //publishProgress(getText(R.string.storage_read_only));
        } else if (!Environment.MEDIA_MOUNTED.equals(state)) {
            // not mounted
            log("media not mounted");
            //publishProgress(getText(R.string.storage_not_mounted));
        } else {

            try {
                ImageStorage storage = new ImageStorage(CameraFacesActivity.this);
                File file = storage.store(fileName, bitmap);

                if(file!=null) {

                    return storage.getLastMediaUri();
                }

            } catch (Exception e) {
                Log.i("WeaponActivity", "Failed saving picture " + e);
                //publishProgress(getText(R.string.storage_exception)
                //        .toString() + e);
                e.printStackTrace();
            }
        }
        return null;
    }


    public void onPictureTaken(final Uri mediaUri) {
        onSharePicture(mediaUri);
    }

    public void onSharePicture(Uri mediaUri) {

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/png");
        String msg = "I derpified this photo";
        intent.putExtra(Intent.EXTRA_SUBJECT, msg);
        intent.putExtra("sms_body", msg);

        intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        //intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mediaUri));
        intent = Intent.createChooser(intent,
                this.getText(R.string.share_picture));
        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progressDialog != null) {
            progressDialog.dismiss();;
            progressDialog = null;
        }
        // kill the activity
        this.finish();
    }


    /**
     * View that overlays the camera,
     * it paints all faces on top of the detected faces.
     */
    class FaceBoxView extends SurfaceView implements FaceDetectionListener {
        private PointF point = new PointF();
        private FaceDetector.Face[] faces;
        private int nFaces;

        public FaceBoxView(Context ctx) {
            super(ctx);
            this.getHolder().setFormat(PixelFormat.TRANSPARENT);
            setWillNotDraw(false);
        }

        @Override
        public void onFacesDetected(FaceDetector.Face[] faces, final int nFaces) {
            if (!facesLoaded || !isDetecting()) {
                return;
            }
            this.faces = faces;
            this.nFaces = nFaces;
            post(new Runnable() {
                @Override
                public void run() {
                    if (nFaces == 0) {
                        if (shutter.getVisibility() == View.VISIBLE) {
                            hideShutter();
                        }
                    } else {
                        if (noFacesDetectedView.getVisibility() == View.VISIBLE) {
                            showShutter();
                        }
                    }

                    // force a repaint
                    invalidate();
                }
            });
        }

        public void onDraw(Canvas c) {
            // just check if drawing is wokring
            super.onDraw(c);
            if(faceBitmaps == null) {
            	return;
            }

            drawFaces(c, getWidth(), getHeight(), cameraView.isFrontCamera());

        }

        void drawFaces(Canvas c, int width, int height, boolean isHorizontalInverted) {

            Bitmap faceBitmap = null;
            Face face = null;
            float scale;
            int faceWidth;
            if (faces == null) {
                return;
            }
            int i = 0;
            for (FaceDetector.Face f : faces) {
                if (f != null && faceBitmaps != null) {
                    faceBitmap = faceBitmaps[i % faceBitmaps.length];
                    face = CameraFacesActivity.this.faces[i % CameraFacesActivity.this.faces.length];
                    if (faceBitmap == null) {
                        continue;
                    }
                    int bitmapWidth = faceBitmap.getWidth();
                    int bitmapHeight = faceBitmap.getHeight();
                    log("Repainting face");
                    f.getMidPoint(point);
                    float previewX = point.x / DETECTOR_IMG_SCALE;
                    float previewY = point.y / DETECTOR_IMG_SCALE;
                    float previewE = f.eyesDistance() / DETECTOR_IMG_SCALE;

                    int x = (int)((previewX / previewWidth) * width);
                    int y = (int)((previewY / previewHeight) * height);
                    int e = (int)((previewE / previewWidth) * width);


                    if (isHorizontalInverted) {
                        x = width - x;
                    }

                    //c.drawRect(x - e, y - e, x + e, y + e, RECT_PAINT);

                    faceWidth = (int)(2.4 * e);
                    scale = faceWidth / (float)bitmapWidth;
                    scale *= face.scale;
                    c.save();
                    c.translate(x, y);
                    c.scale(scale, scale);

                    int offsetX = (int) (face.xOffsetPercent * bitmapWidth);
                    int offsetY = (int) (face.yOffsetPercent * bitmapHeight);


                    c.drawBitmap(
                        faceBitmap,
                        -bitmapWidth / 2 + offsetX,
                        -faceBitmap.getHeight() / 2 + offsetY,
                        null);
                    c.restore();

                    i++;
                }
            }
        }
    }

    private void showShutter() {
        if (animating) {
            return;
        }
        shutterOut.cancel();
        shutter.setAnimation(shutterIn);
        shutter.setVisibility(View.VISIBLE);
        shutterIn.start();

        textIn.cancel();
        noFacesDetectedView.setAnimation(textOut);
        textOut.start();
    }

    private void hideShutter() {
        if (animating) {
            return;
        }

        textOut.cancel();
        noFacesDetectedView.setAnimation(textIn);
        noFacesDetectedView.setVisibility(View.VISIBLE);
        textIn.start();

        onlyHideShutter();
    }

    private void onlyHideShutter() {
        shutterIn.cancel();
        shutter.setAnimation(shutterOut);
        shutterOut.start();
    }
}