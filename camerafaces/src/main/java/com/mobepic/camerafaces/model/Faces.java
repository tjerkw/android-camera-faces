package com.mobepic.camerafaces.model;

import com.mobepic.camerafaces.R;

import java.util.Random;

/**
 * Faces database class
 */
public class Faces {
    public Face[] faces = {
        new Face(R.drawable.face_me_gusta_hq, R.string.face_me_gusta, 1.4f),
        new Face(R.drawable.face_lol_hq, R.string.face_lol, 0f, 0.15f, 1.4f),
        new Face(R.drawable.face_derp, R.string.face_derp, 1.55f),
        new Face(R.drawable.face_troll, R.string.face_trolo, 1.3f),
        new Face(R.drawable.face_lol, R.string.face_lol, 0f, 0.25f, 1.4f),
        new Face(R.drawable.face_me_gusta, R.string.face_me_gusta, 1.4f),
        new Face(R.drawable.face_forever_alone, R.string.face_forever_alone, -.2f, -.2f, 1.5f),
    };
    private static Random random = new Random();

    public Face getRandom() {
        return faces[random.nextInt(faces.length)];
    }

    public Face getFace(int count) {
        return faces[count % faces.length];
    }

}
