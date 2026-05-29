package com.hh.uiperception.reflectionverifier.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.hh.uiperception.reflectionverifier.probe.ListenerProbe;
import com.hh.uiperception.reflectionverifier.probe.ProbeReport;
import com.hh.uiperception.reflectionverifier.probe.ProbeResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化验证 Activity。
 * 创建各种 listener 配置的 ListView / RecyclerView，运行反射探测，展示结果。
 * 从桌面启动即可查看。
 */
public class ReflectionVerifierActivity extends Activity {

    private static final String TAG = "ReflectionVerifier";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Listener Reflection Probe Verifier");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        // 执行所有探测场景
        List<ProbeReport> reports = runAllScenarios();

        // 展示结果
        int passCount = 0;
        int failCount = 0;

        for (ProbeReport report : reports) {
            TextView scenarioLabel = new TextView(this);
            scenarioLabel.setText(report.scenario);
            scenarioLabel.setTextSize(16);
            scenarioLabel.setPadding(0, 16, 0, 4);
            scenarioLabel.setTextColor(report.passed ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
            root.addView(scenarioLabel);

            for (ProbeResult result : report.results) {
                TextView resultText = new TextView(this);
                resultText.setText("  " + result.toString());
                resultText.setTextSize(13);
                resultText.setTextColor(Color.DKGRAY);
                root.addView(resultText);
            }

            TextView statusText = new TextView(this);
            statusText.setText(report.passed ? "  PASS" : "  FAIL");
            statusText.setTextSize(14);
            statusText.setTextColor(report.passed ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
            statusText.setPadding(0, 2, 0, 8);
            root.addView(statusText);

            if (report.passed) passCount++;
            else failCount++;
        }

        // 汇总
        TextView summary = new TextView(this);
        summary.setText("\n=== SUMMARY: " + passCount + " passed, " + failCount + " failed ===");
        summary.setTextSize(16);
        summary.setPadding(0, 16, 0, 16);
        summary.setTextColor(failCount == 0 ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        root.addView(summary);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private List<ProbeReport> runAllScenarios() {
        List<ProbeReport> reports = new ArrayList<>();

        // --- Scenario 1: ListView 无 listener ---
        ListView lv1 = new ListView(this);
        reports.add(probeAdapterView(lv1, "ListView 无 listener",
                false, false));

        // --- Scenario 2: ListView 有 OnItemClickListener ---
        ListView lv2 = new ListView(this);
        lv2.setOnItemClickListener((parent, view, position, id) -> {});
        reports.add(probeAdapterView(lv2, "ListView 有 OnItemClickListener",
                true, false));

        // --- Scenario 3: ListView 有 OnItemSelectedListener ---
        ListView lv3 = new ListView(this);
        lv3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {}
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        reports.add(probeAdapterView(lv3, "ListView 有 OnItemSelectedListener",
                false, true));

        // --- Scenario 4: ListView 同时有 Click + Selected ---
        ListView lv4 = new ListView(this);
        lv4.setOnItemClickListener((parent, view, position, id) -> {});
        lv4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {}
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        reports.add(probeAdapterView(lv4, "ListView 同时有 Click + Selected",
                true, true));

        // --- Scenario 5: mOnItemLongClickListener hidden API 受限测试 ---
        ListView lv5 = new ListView(this);
        lv5.setOnItemLongClickListener((parent, view, position, id) -> true);
        reports.add(probeLongClickBlocked(lv5, "ListView mOnItemLongClickListener (预期被 hidden API 拦截)"));

        // --- Scenario 6: RecyclerView 无 listener ---
        RecyclerView rv1 = new RecyclerView(this);
        reports.add(probeRecyclerView(rv1, "RecyclerView 无 listener", false));

        // --- Scenario 7: RecyclerView 有 OnItemTouchListener ---
        RecyclerView rv2 = new RecyclerView(this);
        rv2.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());
        reports.add(probeRecyclerView(rv2, "RecyclerView 有 OnItemTouchListener", true));

        // --- Scenario 8: RecyclerView 多个 listener ---
        RecyclerView rv3 = new RecyclerView(this);
        rv3.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());
        rv3.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }
        });
        reports.add(probeRecyclerView(rv3, "RecyclerView 有 2 个 OnItemTouchListener", true));

        // --- Scenario 9: RecyclerView 子类 ---
        RecyclerView rv4 = new RecyclerView(this) {};
        rv4.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());
        reports.add(probeRecyclerView(rv4, "RecyclerView 匿名子类 + OnItemTouchListener", true));

        // --- Scenario 10: View hasOnClickListeners ---
        View v1 = new View(this);
        reports.add(probeGenericView(v1, "普通 View 无 OnClickListener", false));

        View v2 = new View(this);
        v2.setOnClickListener(view -> {});
        reports.add(probeGenericView(v2, "普通 View 有 OnClickListener", true));

        return reports;
    }

    /**
     * 探测 AdapterView 的 Click + Selected 两个可用字段。
     * mOnItemLongClickListener 因 hidden API 限制 (api=max-target-o) 被跳过，仅记录探测状态。
     */
    private ProbeReport probeAdapterView(AdapterView<?> view, String scenario,
                                          boolean expectClick, boolean expectSelected) {
        List<ProbeResult> results = ListenerProbe.probeAllAdapterViewListeners(view);

        // results[0] = mOnItemClickListener, results[1] = mOnItemLongClickListener (受限), results[2] = mOnItemSelectedListener
        boolean clickOk = results.get(0).valueDetected == expectClick;
        boolean selectedOk = results.size() >= 3 && results.get(2).valueDetected == expectSelected;
        // mOnItemLongClickListener 被隐藏 API 拦截是预期行为，不影响 pass/fail
        boolean longClickBlocked = results.size() >= 2 && results.get(1).error != null
                && results.get(1).error.startsWith("BLOCKED_BY_HIDDEN_API");

        boolean passed = clickOk && selectedOk;

        boolean allFieldsFound = true;
        for (int i = 0; i < results.size(); i++) {
            if (i == 1) continue; // 跳过 mOnItemLongClickListener 的 fieldFound 判定
            if (!results.get(i).fieldFound) { allFieldsFound = false; break; }
        }

        Log.d(TAG, "Scenario: " + scenario
                + " | click=" + clickOk + ", selected=" + selectedOk
                + ", longClickBlocked=" + longClickBlocked
                + " -> " + (passed ? "PASS" : "FAIL"));
        return new ProbeReport(view.getClass().getName(), scenario, results, allFieldsFound, passed);
    }

    /**
     * 单独测试 mOnItemLongClickListener 的 hidden API 受限情况。
     * 预期结果：fieldFound=false，error 包含 BLOCKED_BY_HIDDEN_API。
     */
    private ProbeReport probeLongClickBlocked(AdapterView<?> view, String scenario) {
        List<ProbeResult> results = ListenerProbe.probeAllAdapterViewListeners(view);

        ProbeResult longClickResult = results.get(1);
        boolean blocked = longClickResult.error != null
                && longClickResult.error.startsWith("BLOCKED_BY_HIDDEN_API");
        boolean passed = blocked;

        Log.d(TAG, "Scenario: " + scenario + " | blocked=" + blocked
                + " -> " + (passed ? "PASS (预期被拦截)" : "FAIL (意外地未被拦截)"));
        return new ProbeReport(view.getClass().getName(), scenario, results, false, passed);
    }

    private ProbeReport probeRecyclerView(RecyclerView rv, String scenario,
                                           boolean expectTouch) {
        ProbeResult touchResult = ListenerProbe.probeRecyclerViewItemTouchListeners(rv);
        List<ProbeResult> results = new ArrayList<>();
        results.add(touchResult);

        boolean passed = touchResult.valueDetected == expectTouch;
        Log.d(TAG, "Scenario: " + scenario + " -> " + (passed ? "PASS" : "FAIL"));
        return new ProbeReport(rv.getClass().getName(), scenario, results, touchResult.fieldFound, passed);
    }

    private ProbeReport probeGenericView(View view, String scenario,
                                           boolean expectClick) {
        ProbeResult clickResult = ListenerProbe.probeHasOnClickListeners(view);
        List<ProbeResult> results = new ArrayList<>();
        results.add(clickResult);

        boolean passed = clickResult.valueDetected == expectClick;
        Log.d(TAG, "Scenario: " + scenario + " -> " + (passed ? "PASS" : "FAIL"));
        return new ProbeReport(view.getClass().getName(), scenario, results, clickResult.fieldFound, passed);
    }
}
