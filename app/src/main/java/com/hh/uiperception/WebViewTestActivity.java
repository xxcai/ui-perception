package com.hh.uiperception;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebViewTestActivity extends Activity {

    private static final String TAG = "WebViewTest";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new AgentBridge(), "AgentBridge");

        setContentView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        webView.loadDataWithBaseURL(null, buildHtml(), "text/html", "utf-8", null);
    }

    private static String buildHtml() {
        return "<!DOCTYPE html>"
                + "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{font-family:sans-serif;padding:24px;background:#f5f5f5}"
                + "h2{color:#333}"
                + "input{width:100%;padding:12px;font-size:16px;border:1px solid #ddd;border-radius:8px;box-sizing:border-box}"
                + "button{width:100%;padding:14px;font-size:16px;background:#1a73e8;color:#fff;border:none;border-radius:8px;margin-top:12px}"
                + "#result{margin-top:16px;padding:16px;background:#fff;border-radius:8px;white-space:pre-wrap;font-size:14px;min-height:60px;border:1px solid #e0e0e0}"
                + "</style></head><body>"
                + "<h2>Agent Bridge Test</h2>"
                + "<p>WebView → JS Bridge → Native HTTP → Agent (PC)</p>"
                + "<input id='taskInput' type='text' placeholder='Enter task...' value='hello from webview'/>"
                + "<button onclick='callAgent()'>Call Agent</button>"
                + "<div id='result'>Result will appear here...</div>"
                + "<script>"
                + "function callAgent() {"
                + "  var task = document.getElementById('taskInput').value;"
                + "  document.getElementById('result').textContent = 'Calling agent...';"
                + "  var result = window.AgentBridge.callAgent(task);"
                + "  document.getElementById('result').textContent = result;"
                + "}"
                + "</script>"
                + "</body></html>";
    }

    private class AgentBridge {
        @JavascriptInterface
        public String callAgent(String task) {
            try {
                URL url = new URL("http://localhost:9800/task");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000);

                String body = "{\"task\":" + escapeJson(task) + "}";
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bodyBytes.length);

                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(bodyBytes);
                }

                int code = conn.getResponseCode();
                BufferedReader reader;
                if (code == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                }
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code != 200) return "HTTP " + code + ": " + sb;
                return sb.toString();
            } catch (Exception e) {
                Log.e(TAG, "callAgent failed", e);
                return "Error: " + e.getMessage();
            }
        }
    }

    private static String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
