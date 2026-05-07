package com.hh.uiperception;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EvaluationResultActivity extends Activity {
    public static final String EXTRA_BASELINE_ID = "baseline_id";

    private static final int COLOR_PASS = 0xFF137333;
    private static final int COLOR_FAIL = 0xFFC5221F;
    private static final int COLOR_MUTED = 0xFF6F7782;
    private static final int COLOR_CARD = 0xFFFFFFFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.screen_background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(24));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        String baselineId = getIntent().getStringExtra(EXTRA_BASELINE_ID);
        File resultFile = findLatestResultFile(baselineId);
        if (resultFile == null) {
            content.addView(title("评测结果"));
            content.addView(emptyState(baselineId));
            setContentView(scrollView);
            return;
        }

        try {
            JSONObject result = new JSONObject(read(resultFile));
            render(content, result);
        } catch (JSONException | IOException e) {
            content.addView(title("评测结果"));
            content.addView(infoText("结果文件读取失败：" + e.getMessage()));
        }
        setContentView(scrollView);
    }

    private void render(LinearLayout content, JSONObject result) throws JSONException {
        JSONObject summary = result.optJSONObject("summary");
        String status = summary != null ? summary.optString("status", "UNKNOWN") : "UNKNOWN";

        LinearLayout header = card();
        LinearLayout titleRow = horizontal();
        TextView title = title(result.optString("baselineId", "unknown"));
        titleRow.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(statusBadge(status));
        header.addView(titleRow);
        header.addView(infoText("runId: " + result.optString("runId", "-")));
        header.addView(infoText("generatedAt: " + formatTime(result.optLong("generatedAt", 0))));
        content.addView(header);

        if (summary != null) {
            content.addView(sectionTitle("总览"));
            LinearLayout overview = card();
            overview.addView(metricRow("Targets",
                    summary.optInt("targetPassCount", 0) + " / " + summary.optInt("targetCount", 0)));
            overview.addView(metricRow("Evidence",
                    summary.optInt("evidencePassCount", 0) + " / " + summary.optInt("evidenceCount", 0)));
            overview.addView(metricRow("Artifacts",
                    String.valueOf(summary.optInt("artifactCount", 0))));
            content.addView(overview);
        }

        content.addView(sectionTitle("产物"));
        LinearLayout artifacts = card();
        JSONArray candidates = result.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            artifacts.addView(infoText("没有产物"));
        } else {
            for (int i = 0; i < candidates.length(); i++) {
                JSONObject candidate = candidates.optJSONObject(i);
                if (candidate != null) {
                    artifacts.addView(artifactRow(candidate));
                }
            }
        }
        content.addView(artifacts);

        JSONArray targetResults = result.optJSONArray("targetResults");
        content.addView(sectionTitle("信息意图"));
        if (targetResults == null || targetResults.length() == 0) {
            content.addView(cardText("没有 targetResults，可能是旧版 count 评测结果。"));
        } else {
            for (int i = 0; i < targetResults.length(); i++) {
                JSONObject target = targetResults.optJSONObject(i);
                if (target != null) {
                    content.addView(targetCard(target));
                }
            }
            content.addView(sectionTitle("能力分类"));
            content.addView(capabilityCard(targetResults));
        }
    }

    private View artifactRow(JSONObject candidate) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(10));

        LinearLayout top = horizontal();
        TextView id = new TextView(this);
        id.setText(candidate.optString("id", "-"));
        id.setTextColor(getColor(R.color.text_primary));
        id.setTextSize(15);
        top.addView(id, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(statusText(readableArtifactStatus(candidate.optString("schemaStatus", "")),
                "PASS".equals(candidate.optString("schemaStatus", ""))));
        row.addView(top);

        String detail = candidate.optString("plugin", "-") + " · "
                + candidate.optString("contentType", "-") + " · "
                + formatBytes(candidate.optLong("bytes", 0));
        row.addView(infoText(detail));
        return row;
    }

    private View targetCard(JSONObject target) {
        LinearLayout card = card();
        String status = target.optString("status", "UNKNOWN");

        LinearLayout top = horizontal();
        top.addView(statusText(status, "PASS".equals(status)));
        TextView id = new TextView(this);
        id.setText("  " + target.optString("id", "-"));
        id.setTextColor(getColor(R.color.text_primary));
        id.setTextSize(15);
        top.addView(id, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView score = new TextView(this);
        score.setText(String.format(Locale.US, "%.2f", target.optDouble("score", 0)));
        score.setTextColor(COLOR_MUTED);
        score.setTextSize(13);
        top.addView(score);
        card.addView(top);

        TextView description = infoText(target.optString("description", ""));
        description.setPadding(0, dp(6), 0, dp(6));
        card.addView(description);
        card.addView(infoText("evidence "
                + target.optInt("passedEvidence", 0)
                + " / "
                + target.optInt("totalEvidence", 0)));

        JSONArray evidenceResults = target.optJSONArray("evidenceResults");
        if (evidenceResults != null) {
            for (int i = 0; i < evidenceResults.length(); i++) {
                JSONObject evidence = evidenceResults.optJSONObject(i);
                if (evidence != null) {
                    card.addView(evidenceRow(evidence));
                }
            }
        }
        return card;
    }

    private View evidenceRow(JSONObject evidence) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackgroundColor("PASS".equals(evidence.optString("status"))
                ? 0xFFF3FAF5 : 0xFFFFF4F2);

        LinearLayout top = horizontal();
        top.addView(statusText(evidence.optString("status", "UNKNOWN"),
                "PASS".equals(evidence.optString("status"))));
        TextView id = new TextView(this);
        id.setText("  " + evidence.optString("id", "-"));
        id.setTextColor(getColor(R.color.text_primary));
        id.setTextSize(13);
        top.addView(id);
        row.addView(top);

        String capability = evidence.optString("capability", "");
        if (capability.isEmpty()) {
            capability = "default";
        }
        row.addView(infoText(capability
                + " · role=" + evidence.optString("role", "")
                + " · name=" + evidence.optString("name", "")
                + " · actual=" + evidence.optInt("actualCount", 0)
                + " · expected>=" + evidence.optInt("minCount", 1)));
        return row;
    }

    private View capabilityCard(JSONArray targetResults) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (int i = 0; i < targetResults.length(); i++) {
            JSONObject target = targetResults.optJSONObject(i);
            if (target == null) {
                continue;
            }
            JSONArray evidenceResults = target.optJSONArray("evidenceResults");
            if (evidenceResults == null) {
                continue;
            }
            for (int j = 0; j < evidenceResults.length(); j++) {
                JSONObject evidence = evidenceResults.optJSONObject(j);
                if (evidence == null) {
                    continue;
                }
                String capability = evidence.optString("capability", "");
                if (capability.isEmpty()) {
                    capability = "default";
                }
                int[] count = counts.computeIfAbsent(capability, key -> new int[2]);
                count[1]++;
                if ("PASS".equals(evidence.optString("status"))) {
                    count[0]++;
                }
            }
        }

        LinearLayout card = card();
        for (Map.Entry<String, int[]> entry : counts.entrySet()) {
            int[] count = entry.getValue();
            card.addView(capabilityRow(entry.getKey(), count[0] + " / " + count[1]));
        }
        return card;
    }

    private View capabilityRow(String capability, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(8));

        row.addView(metricRow(capability, value));

        TextView description = infoText(capabilityDescription(capability));
        description.setTextSize(12);
        description.setPadding(0, 0, 0, dp(2));
        row.addView(description);
        return row;
    }

    private static String capabilityDescription(String capability) {
        switch (capability) {
            case "textual_content":
                return "页面文字、名称、摘要、日期等可读内容";
            case "structural":
                return "列表、分组、滚动容器等页面结构";
            case "navigational":
                return "页面身份、Tab、底部导航和快捷入口";
            case "actionable":
                return "按钮、列表项等可操作入口";
            case "semantic_label":
                return "图标或入口的业务语义";
            case "visual_state":
                return "红点、选中、角标等视觉状态";
            case "visual_content":
                return "图片、预览图等非文字视觉内容";
            default:
                return "未显式分类的基础证据";
        }
    }

    private File findLatestResultFile(String baselineId) {
        File baseDir = getExternalFilesDir(null);
        if (baseDir == null) {
            return null;
        }
        File capturesDir = new File(baseDir, "captures");
        if (!capturesDir.isDirectory()) {
            return null;
        }

        if (baselineId != null && !baselineId.isEmpty()) {
            return latestForBaseline(new File(capturesDir, baselineId));
        }

        File latest = null;
        File[] baselines = capturesDir.listFiles(File::isDirectory);
        if (baselines == null) {
            return null;
        }
        for (File baseline : baselines) {
            File result = latestForBaseline(baseline);
            if (result != null && (latest == null || result.lastModified() > latest.lastModified())) {
                latest = result;
            }
        }
        return latest;
    }

    private File latestForBaseline(File baselineDir) {
        File runsDir = new File(baselineDir, "runs");
        File[] runs = runsDir.listFiles(File::isDirectory);
        if (runs == null || runs.length == 0) {
            return null;
        }
        File latestRun = null;
        for (File run : runs) {
            if (latestRun == null || run.getName().compareTo(latestRun.getName()) > 0) {
                latestRun = run;
            }
        }
        File result = new File(latestRun, "evaluation/evaluation-result.json");
        return result.isFile() ? result : null;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(COLOR_CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        return card;
    }

    private TextView cardText(String text) {
        TextView view = infoText(text);
        view.setBackgroundColor(COLOR_CARD);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        return view;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private View metricRow(String label, String value) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(5), 0, dp(5));
        TextView left = infoText(label);
        row.addView(left, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView right = new TextView(this);
        right.setText(value);
        right.setTextColor(getColor(R.color.text_primary));
        right.setTextSize(15);
        row.addView(right);
        return row;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setTextColor(getColor(R.color.text_primary));
        view.setGravity(Gravity.START);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(getColor(R.color.text_primary));
        view.setPadding(0, dp(10), 0, dp(8));
        return view;
    }

    private TextView infoText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(COLOR_MUTED);
        return view;
    }

    private TextView statusBadge(String status) {
        TextView view = statusText(status, "PASS".equals(status));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(4), dp(8), dp(4));
        return view;
    }

    private TextView statusText(String text, boolean pass) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTextColor(pass ? COLOR_PASS : COLOR_FAIL);
        return view;
    }

    private View emptyState(String baselineId) {
        String label = baselineId == null || baselineId.isEmpty() ? "当前设备" : baselineId;
        return cardText(label + " 暂无 evaluation-result.json，请先在对应页面点击抓取按钮。");
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String readableArtifactStatus(String schemaStatus) {
        if ("PASS".equals(schemaStatus)) {
            return "可读取";
        }
        if ("FAIL".equals(schemaStatus)) {
            return "不可用";
        }
        return schemaStatus == null || schemaStatus.isEmpty() ? "未知" : schemaStatus;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date(timestamp));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
