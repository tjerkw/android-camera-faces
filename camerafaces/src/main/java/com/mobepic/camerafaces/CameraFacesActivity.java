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
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.mobepic.camerafaces.model.Face;
import com.mobepic.camerafaces.model.Faces;

/**
 * Renders the chosen image (face) on top of the faces detected in the
 * camera result.
 */
public class CameraFacesActivity extends CameraFaceDetectionActivity {
    private Face face;
    private Bitmap faceBitmap = null;
    private FaceBoxView faceBoxView;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        int[] chosenFaces = this.getIntent().getIntArrayExtra("faces");
        int chosenFace = chosenFaces[0];
        Faces faces = new Faces();
        face = chosenFace == -1 ? faces.getRandom() : faces.getFace(chosenFace);
        int faceDrawable = face.faceResId;

        // TODO: offload this to a loader or async task
        faceBitmap = BitmapFactory.decodeResource(this.getResources(), faceDrawable);
        super.onCreate(savedInstanceState);
    }

    protected View getOverlayView() {
        faceBoxView = new FaceBoxView(this);
        this.setFaceDetectionListener(faceBoxView);
        return faceBoxView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {

            log("taking screenshot");
            takeScreenshot();
        }
        return super.onTouchEvent(e);
    }

    private void takeScreenshot() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
        }
        progressDialog.show();
        progressDialog.setMessage("Taking picture");

        // do in thread to prevent ANR
        try {
            camera.takePicture(null, null, new Camera.PictureCallback() {

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

                                this.publishProgress("Creating picture");
                                // Get camera bitmap and scale it to appropriate size
                                Bitmap cameraBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                Bitmap result = cameraBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                cameraBitmap.recycle();

                                Canvas c = new Canvas(result);
                                faceBoxView.drawFaces(c, result.getWidth(), result.getHeight());

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
                                    onSharePicture(mediaUri);
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
            progressDialog.hide();
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
        public void onFacesDetected(FaceDetector.Face[] faces, int nFaces) {
            this.faces = faces;
            this.nFaces = nFaces;
        }

        public void onDraw(Canvas c) {
            // just check if drawing is wokring
            super.onDraw(c);
            if(faceBitmap == null) {
            	return;
            }

            drawFaces(c, getWidth(), getHeight());

        }

        void drawFaces(Canvas c, int width, int height) {

            int bitmapWidth = faceBitmap.getWidth();
            int bitmapHeight = faceBitmap.getHeight();
            float scale;
            int faceWidth;
            if (faces == null) {
                return;
            }
            for (FaceDetector.Face f : faces) {
                if (f != null) {
                    log("Repainting face");
                    f.getMidPoint(point);
                    float previewX = point.x / DETECTOR_IMG_SCALE;
                    float previewY = point.y / DETECTOR_IMG_SCALE;
                    float previewE = f.eyesDistance() / DETECTOR_IMG_SCALE;

                    int x = (int)((previewX / previewWidth) * width);
                    int y = (int)((previewY / previewHeight) * height);
                    int e = (int)((previewE / previewWidth) * width);

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
                }
            }
        }
    };
}