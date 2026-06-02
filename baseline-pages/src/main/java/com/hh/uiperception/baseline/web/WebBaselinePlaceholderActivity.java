package com.hh.uiperception.baseline.web;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebBaselinePlaceholderActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        setContentView(webView);

        String html = "<!DOCTYPE html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "body{font-family:sans-serif;padding:16px;margin:0}"
            + "h1{font-size:24px;color:#333;margin:0 0 16px}"
            + "p{color:#555;line-height:1.5;margin:0 0 12px}"
            + "a{color:#1593FF}"
            + "button{padding:12px 24px;background:#1593FF;color:#fff;border:none;"
            + "border-radius:8px;font-size:16px;margin:4px 0}"
            + "input[type=text],input[type=email]{padding:12px;border:1px solid #ddd;"
            + "border-radius:4px;width:100%;box-sizing:border-box;margin:4px 0;font-size:16px}"
            + "ul{padding-left:20px;margin:8px 0}li{margin:8px 0}"
            + "label{margin-left:4px}"
            + "</style></head><body>"
            + "<h1>Web Baseline Test Page</h1>"
            + "<p>This is a paragraph with <a href='#'>a link</a> inside.</p>"
            + "<button id='btn1' onclick=\"document.getElementById('output').textContent='Clicked!'\">Click Me</button>"
            + "<div id='output' style='min-height:24px;color:#666;margin:4px 0'></div>"
            + "<input type='text' id='input1' placeholder='Type something...'>"
            + "<input type='email' id='input2' placeholder='Email address'>"
            + "<div style='margin:8px 0'>"
            + "<input type='checkbox' id='cb1'><label for='cb1'>Check this</label>"
            + "</div>"
            + "<ul>"
            + "<li><a href='#item1'>Item 1</a></li>"
            + "<li><a href='#item2'>Item 2</a></li>"
            + "<li><a href='#item3'>Item 3</a></li>"
            + "</ul>"
            + "<form><fieldset><legend>Form Section</legend>"
            + "<input type='radio' name='choice' id='r1' value='A'><label for='r1'>Option A</label><br>"
            + "<input type='radio' name='choice' id='r2' value='B'><label for='r2'>Option B</label>"
            + "</fieldset></form>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://baseline.test", html, "text/html", "UTF-8", null);
    }
}
