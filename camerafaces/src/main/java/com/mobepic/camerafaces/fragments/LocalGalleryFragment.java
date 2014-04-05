package com.mobepic.camerafaces.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.mobepic.camerafaces.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class LocalGalleryFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private CursorAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void onViewCreated (View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //getListView().setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.slide_in));
        createAdapter();
        setListAdapter(adapter);


        // Start out with a progress indicator.
        setListShown(false);
        getListView().setDivider(null);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        //TODO: string empty text to label
        this.setEmptyText(getActivity().getText(R.string.local_gallery_empty_text));

    }


    public void createAdapter() {

        // Create an empty adapter we will use to display the loaded data.
        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.gallery_list_item,
                null, // load cursor via loader
                new String[] {},
                new int[] {}, 0
        ) {

            @Override
            public void bindView (View view, Context context, Cursor cursor) {

                final AQuery aq = new AQuery(view);

                // initialize the image loader
                ImageView imageView = (ImageView)view.findViewById(R.id.image);
                if(imageView.getHeight()>0) {
                    imageView.setMinimumHeight(imageView.getHeight());
                }

                // get the path on sdcard
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(columnIndex);

                // get the image id
                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int imageID = cursor.getInt(columnIndex);

                // obtain the image URI
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageID));

                // Set the content of the image based on the image URI
                //long originalImageId = Long.parseLong(url.substring(url.lastIndexOf("/") + 1, url.length()));
                log("Loading image " + path);

                final AQuery image = aq.find(R.id.image);
                image.image(new File(path), true, 300, new BitmapAjaxCallback() {

                    @Override
                    public void callback(String url, ImageView iv, Bitmap bm, AjaxStatus s) {
                        iv.setImageBitmap(bm);
                        image.animate(R.anim.fade_in);
                    }

                });

                imageView.setTag(uri);
                imageView.setOnClickListener(openButtonClickListener);

                Button shareButton = (Button) view.findViewById(R.id.share);
                shareButton.setTag(uri);
                shareButton.setOnClickListener(shareButtonClickListener);
            }
        };
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] projection = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA
        };
        String select = MediaStore.Images.Media.DATA + " like ? ";

        return new CursorLoader(
                getActivity(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                select,
                new String[] {"%Derpinator%"},
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        adapter.swapCursor(data);
        log("Finished loading cursor, size " + (data != null ? data.getCount() : "null"));
        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        adapter.swapCursor(null);
    }

    private void log(String msg) {
        Log.d("LocalGalleryFragment", msg);
    }

    private View.OnClickListener openButtonClickListener= new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getTag()!=null) {
                Uri uri = (Uri)view.getTag();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/png");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(intent);
            }
        }
    };

    private View.OnClickListener shareButtonClickListener= new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getTag()!=null) {
                Uri uri = (Uri)view.getTag();

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/png");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_picture)));
            }
        }
    };
}
