package com.hh.uiperception;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView titleView = new TextView(this);
        titleView.setText(R.string.app_name);
        titleView.setTextSize(24);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setBackgroundColor(getColor(R.color.screen_background));

        setContentView(titleView);
    }
}
