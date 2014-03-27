package nl.tjerk.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView wich has the possibility to stretch to the whole screen.
 * While still keeping the aspect ratio.
 *
 * @author Tjerk, Patrick Boos & Jason Sturges (from stack overflow)
 * @date 8/14/12
 */
public class AspectRatioImageView extends ImageView {

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        Drawable d = this.getDrawable();
        if(d!=null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int diw = d.getIntrinsicWidth();
            if(diw>0) {
                int height = width * d.getIntrinsicHeight() / diw;
                setMeasuredDimension(width, height);
                return; // done
            }
        }
        // fallback
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
