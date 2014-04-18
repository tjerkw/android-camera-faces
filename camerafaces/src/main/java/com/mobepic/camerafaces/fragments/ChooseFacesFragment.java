package com.mobepic.camerafaces.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobepic.camerafaces.CameraFacesActivity;
import com.mobepic.camerafaces.R;
import com.mobepic.camerafaces.model.Face;
import com.mobepic.camerafaces.model.Faces;

/**
 * Fragment used to choose a face
 */
public class ChooseFacesFragment extends Fragment implements AdapterView.OnItemClickListener {
    private View contentView;
    private GridView gridView;
    private Button cameraButton;
    private Animation slideIn;
    private Animation slideOut;
    // database of faces
    private Faces faces = new Faces();
    private SparseBooleanArray checks = new SparseBooleanArray();
    private int checkedItemCount;

    private void log(String msg) {
        Log.i("ChooseFacesFragment", msg);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_choose_faces, null);

        slideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in);
        slideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out);

        cameraButton = (Button) contentView.findViewById(R.id.camera_button);
        cameraButton.setVisibility(View.GONE);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });

        gridView = (GridView)contentView.findViewById(android.R.id.list);

        gridView.setAdapter(new ArrayAdapter<Face>(
                getActivity(),
                R.layout.list_item_face, R.id.name, faces.faces
        ) {

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                final Face face = faces.faces[position];
                ((TextView) v.findViewById(R.id.name)).setText(getResources().getString(face.nameResId));
                ((ImageView) v.findViewById(R.id.face)).setImageResource(face.faceResId);

                updateItem(v, position);
                return v;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }
        });

        gridView.setOnItemClickListener(this);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // somehow this state is not saved
        cameraButton.setVisibility(checkedItemCount > 0 ?  View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        //gridView.setItemChecked(position, !gridView.isItemChecked(position));
        log("onItemClick " + checkedItemCount);

        if (checks.get(position)) {
            // uncheck
            checks.put(position, false);
            checkedItemCount--;
        } else {
            checks.put(position, true);
            checkedItemCount++;
        }

        if (checkedItemCount > 0) {
            showCameraButton();
        } else if (checkedItemCount == 0) {
            hideCameraButton();
        }
        updateItem(v, position);
    }

    private void updateItem(View v, int position) {
        if (checks.get(position)) {
            v.setBackgroundResource(R.drawable.card_bg_selected_r4);
        } else {
            v.setBackgroundResource(R.drawable.card_bg_r4);
        }
        v.setPadding(0,0,0,0);
    }

    private void showCameraButton() {
        if (cameraButton.getVisibility() == View.GONE) {

            slideOut.cancel();

            log("showButton");
            cameraButton.setVisibility(View.VISIBLE);
            cameraButton.setAnimation(slideIn);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // be sure its shown
                    cameraButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            slideIn.start();
        }

    }

    private void hideCameraButton() {
        log("try hideButton");
        if (cameraButton.getVisibility() == View.VISIBLE) {

            slideIn.cancel();

            log("hideButton");
            cameraButton.setAnimation(slideOut);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    cameraButton.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            slideOut.start();
            contentView.requestLayout();
        }
    }

    private void openCamera() {
        int cnt = checkedItemCount;
        if (cnt == 0) {
            return;
        }
        int[] selected = new int[cnt];
        int i = 0;
        for (int pos=0;pos<faces.faces.length;pos++) {
            if (checks.get(pos)) {
                selected[i++] = pos;
            }
        }
        Intent intent = new Intent(this.getActivity(), CameraFacesActivity.class);
        intent.putExtra("faces", selected);
        this.startActivity(intent);
    }
}
