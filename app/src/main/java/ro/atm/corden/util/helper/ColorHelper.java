package ro.atm.corden.util.helper;

import android.graphics.Color;

import androidx.annotation.ColorInt;

public class ColorHelper {
    @ColorInt
    public static int adjustColor(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @ColorInt
    public static int adjustPolygonBackground(@ColorInt int color){
        float factor = 0.6f;

        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
