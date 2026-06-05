package com.hh.uiperception.webplugin;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.hh.uiperception.core.CaptureRequest;
import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionPlugin;
import com.hh.uiperception.core.TransformRequest;
import com.hh.uiperception.core.TransformResult;
import com.hh.uiperception.core.semantic.RefAssigner;
import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SnapshotRenderer;
import com.hh.uiperception.webplugin.TreeNormalizer;
import com.hh.uiperception.webplugin.WebJsonParser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class WebPerceptionPlugin implements PerceptionPlugin {

    private static final String TAG = "WebPerception";

    @Override
    public String name() {
        return "web";
    }

    @Override
    public boolean canHandle(Activity activity) {
        return WebViewFinder.find(activity).isWeb;
    }

    @Override
    public CaptureResult capture(Activity activity, CaptureRequest request) {
        WebViewFinder.Finding finding = WebViewFinder.find(activity);
        if (!finding.isWeb || finding.dominantWebView == null) {
            return CaptureResult.error(request.channelName(), request.baselineId(),
                    "No dominant WebView found");
        }

        WebView webView = finding.dominantWebView;
        String js = WebDomSerializer.script(activity);

        AtomicReference<String> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            webView.evaluateJavascript(js, value -> {
                if (value != null) {
                    // evaluateJavascript wraps result in quotes; strip them
                    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        // Unescape JSON string escapes from evaluateJavascript
                        value = value.replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\n", "\n")
                                .replace("\\t", "\t");
                    }
                    resultRef.set(value);
                }
                latch.countDown();
            });
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                return CaptureResult.error(request.channelName(), request.baselineId(),
                        "JS evaluation timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CaptureResult.error(request.channelName(), request.baselineId(),
                    "Interrupted");
        }

        String result = resultRef.get();
        if (result == null || result.isEmpty()) {
            return CaptureResult.error(request.channelName(), request.baselineId(),
                    "JS returned empty result");
        }

        return CaptureResult.success(
                request.channelName(),
                request.baselineId(),
                "application/json",
                result,
                activity.getClass().getSimpleName()
        );
    }

    @Override
    public TransformResult transform(TransformRequest request) {
        String json = request.sourceContent();
        try {
            SemanticNode root = WebJsonParser.parse(json);
            if (root == null) {
                return TransformResult.error("web_semantic_snapshot",
                        request.sourceToolName(), request.baselineId(),
                        "JSON parse returned null");
            }
            SemanticNode normalized = TreeNormalizer.normalize(root);
            SemanticNode withRefs = RefAssigner.assign(normalized, "w");
            String yaml = SnapshotRenderer.render(withRefs);
            return TransformResult.success("web_semantic_snapshot",
                    request.sourceToolName(), request.baselineId(),
                    "text/yaml", yaml, request.activityClassName());
        } catch (Exception e) {
            return TransformResult.error("web_semantic_snapshot",
                    request.sourceToolName(), request.baselineId(),
                    e.getMessage());
        }
    }

    @Override
    public SemanticNode buildSemanticTree(CaptureResult captureResult) {
        SemanticNode root = WebJsonParser.parse(captureResult.content());
        return root != null ? TreeNormalizer.normalize(root) : null;
    }
}
