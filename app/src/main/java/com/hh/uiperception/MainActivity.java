package com.hh.uiperception;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hh.uiperception.baseline.BaselineRegistry;
import com.hh.uiperception.baseline.BaselineRouter;
import com.hh.uiperception.baseline.BaselineSpec;
import com.hh.uiperception.smallmodelplugin.ui.SmallModelDebugActivity;

import java.util.List;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.screen_background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView titleView = new TextView(this);
        titleView.setText("基准页面");
        titleView.setTextSize(28);
        titleView.setGravity(Gravity.START);
        titleView.setTextColor(getColor(R.color.text_primary));
        content.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitleView = new TextView(this);
        subtitleView.setText("Phase1 页面准备入口");
        subtitleView.setTextSize(15);
        subtitleView.setTextColor(0xFF5F6368);
        subtitleView.setPadding(0, dp(4), 0, dp(16));
        content.addView(subtitleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        List<BaselineSpec> specs = BaselineRegistry.getAll();
        for (BaselineSpec spec : specs) {
            content.addView(createSpecRow(spec), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        TextView smallModelDebug = actionButton("Gemma 小模型调试", 0xFFE6F4EA);
        smallModelDebug.setOnClickListener(v ->
                startActivity(new Intent(this, SmallModelDebugActivity.class)));
        LinearLayout.LayoutParams debugParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        debugParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(smallModelDebug, debugParams);

        TextView windowTest = actionButton("窗口捕获测试", 0xFFFCE8E6);
        windowTest.setOnClickListener(v ->
                startActivity(new Intent(this, WindowTestActivity.class)));
        LinearLayout.LayoutParams windowTestParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        windowTestParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(windowTest, windowTestParams);

        TextView agentBridgeTest = actionButton("Agent Bridge 测试", 0xFFE8F7E8);
        agentBridgeTest.setOnClickListener(v ->
                startActivity(new Intent(this, WebViewTestActivity.class)));
        LinearLayout.LayoutParams agentBridgeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        agentBridgeParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(agentBridgeTest, agentBridgeParams);

        setContentView(scrollView);
    }

    private View createSpecRow(BaselineSpec spec) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundColor(0xFFFFFFFF);

        TextView title = new TextView(this);
        title.setText(spec.title());
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(18);

        TextView description = new TextView(this);
        description.setText(spec.description());
        description.setTextColor(0xFF5F6368);
        description.setTextSize(14);
        description.setPadding(0, dp(6), 0, 0);

        TextView route = new TextView(this);
        route.setText(spec.route());
        route.setTextColor(0xFF7A7F87);
        route.setTextSize(12);
        route.setPadding(0, dp(8), 0, 0);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);

        TextView open = actionButton("打开页面", 0xFFE8F0FE);
        open.setOnClickListener(v -> {
            boolean opened = BaselineRouter.open(this, spec.route());
            if (!opened) {
                Toast.makeText(this, "入口暂未实现：" + spec.title(), Toast.LENGTH_SHORT).show();
            }
        });

        TextView evaluation = actionButton("查看最新评测", 0xFFE8F0FE);
        evaluation.setOnClickListener(v -> {
            Intent intent = new Intent(this, EvaluationResultActivity.class);
            intent.putExtra(EvaluationResultActivity.EXTRA_BASELINE_ID, spec.id());
            startActivity(intent);
        });

        actions.addView(open, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        LinearLayout.LayoutParams evaluationParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        evaluationParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(evaluation, evaluationParams);

        row.addView(title);
        row.addView(description);
        row.addView(route);
        row.addView(actions);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, dp(12));
        wrapper.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return wrapper;
    }

    private TextView actionButton(String text, int backgroundColor) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(0xFF1A73E8);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(40));
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setBackgroundColor(backgroundColor);
        button.setClickable(true);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
