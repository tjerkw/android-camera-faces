package com.mobepic.camerafaces.model;

/**
 * A single face with an image resource and a name
 */
public class Face {
    public int faceResId;
    public int nameResId;
    public float xOffsetPercent = 0f;
    public float yOffsetPercent = 0f;
    public float scale = 1.0f;

    Face(int faceResId, int nameResId, float x, float y, float scale) {
        this.faceResId = faceResId;
        this.nameResId = nameResId;
        this.xOffsetPercent = x;
        this.yOffsetPercent = y;
        this.scale = scale;
    }

    Face(int faceResId, int nameResId, float scale) {
        this(faceResId, nameResId, 0f, 0f, scale);
    }


    Face(int faceResId, int nameResId, float x, float y) {
        this(faceResId, nameResId, x, y, 1f);
    }

    Face(int faceResId, int nameResId) {
        this(faceResId, nameResId, 0f, 0f, 1f);
    }
}
