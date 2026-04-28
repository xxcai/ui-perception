package com.hh.uiperception.baseline.nativepage;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

final class UiKit {
    static final int BLUE = 0xFF1593FF;
    static final int TEXT_PRIMARY = 0xFF2F3136;
    static final int TEXT_SECONDARY = 0xFF8A8D91;
    static final int DIVIDER = 0xFFEDEFF2;

    private UiKit() {
    }

    static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static ImageView iconImage(Context context, int drawableRes, int backgroundRes) {
        ImageView image = new ImageView(context);
        image.setImageResource(drawableRes);
        if (backgroundRes != 0) {
            image.setBackgroundResource(backgroundRes);
            int padding = dp(context, 8);
            image.setPadding(padding, padding, padding, padding);
        }
        image.setScaleType(ImageView.ScaleType.CENTER);
        return image;
    }

    static TextView title(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(TEXT_PRIMARY);
        view.setTextSize(24);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
}
