package com.mobepic.camerafaces;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: tjerk
 * Date: 6/5/12
 * Time: 10:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageStorage {
    private Context context;
    private Uri lastMediaUri;

    public ImageStorage(Context context) {
        this.context = context;
    }

    public File getTargetDir() {
        File root = Environment.getExternalStorageDirectory();
        if (!root.canWrite()) {
            return null;
        }
        File dir = new File(root, "Pictures");
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir = new File(dir, "Derpinator");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public File store(String fileName, Bitmap bitmap) throws IOException {
        File dir = getTargetDir();
        if(dir == null) {
            return null;
        }

        File file = new File(dir, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        Log.i("WeaponActivity", "Saving weapon3d picture: "
                + file.getPath());
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.flush();
        fos.close();

        String path = file.getPath();

        lastMediaUri = Uri.parse(MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                path,
                fileName,
                "Shot with Weapons3D Application using weapon"
        ));

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        new MediaScannerWrapper(context, path, "image/png").scan();

        return file;
    }

    public Uri getLastMediaUri() {
        return lastMediaUri;
    }
}

class MediaScannerWrapper implements MediaScannerConnection.MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private String mPath;
    private String mMimeType;

    // filePath - where to scan;
    // mime type of media to scan i.e. "image/jpeg".
    // use "*/*" for any media
    public MediaScannerWrapper(Context ctx, String filePath, String mime){
        mPath = filePath;
        mMimeType = mime;
        mConnection = new MediaScannerConnection(ctx, this);
    }

    // do the scanning
    public void scan() {
        mConnection.connect();
    }

    // start the scan when scanner is ready
    public void onMediaScannerConnected() {
        mConnection.scanFile(mPath, mMimeType);
        Log.w("MediaScannerWrapper", "media file scanned: " + mPath);
    }

    public void onScanCompleted(String path, Uri uri) {
        // when scan is completes, update media file tags
        mConnection.disconnect();
    }
}
