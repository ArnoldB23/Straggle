package roca.bajet.com.straggle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by Arnold on 7/21/2017.
 */

public class RatioTabletFrameLayout extends FrameLayout {
    public RatioTabletFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RatioTabletFrameLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        int newHeight = MeasureSpec.getSize(widthSpec) * 1/6;
        int newHeightSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, newHeightSpec);
    }
}
