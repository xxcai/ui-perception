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
            + "h2{font-size:20px;color:#333;margin:16px 0 8px;border-bottom:1px solid #eee;padding-bottom:4px}"
            + "p{color:#555;line-height:1.5;margin:0 0 12px}"
            + "a{color:#1593FF}"
            + "button{padding:12px 24px;background:#1593FF;color:#fff;border:none;"
            + "border-radius:8px;font-size:16px;margin:4px 0}"
            + "input[type=text],input[type=email],input[type=password],input[type=number],"
            + "input[type=search],input[type=tel],input[type=url]{"
            + "padding:12px;border:1px solid #ddd;border-radius:4px;"
            + "width:100%;box-sizing:border-box;margin:4px 0;font-size:16px}"
            + "textarea{padding:12px;border:1px solid #ddd;border-radius:4px;"
            + "width:100%;box-sizing:border-box;margin:4px 0;font-size:16px;height:80px}"
            + "select{padding:12px;border:1px solid #ddd;border-radius:4px;"
            + "width:100%;box-sizing:border-box;margin:4px 0;font-size:16px}"
            + "ul{padding-left:20px;margin:8px 0}li{margin:8px 0}"
            + "label{margin-left:4px;line-height:2}"
            + ".output{min-height:24px;color:#666;margin:4px 0;padding:8px;"
            + "background:#f5f5f5;border-radius:4px;font-family:monospace}"
            + ".row{margin:8px 0}"
            + "</style></head><body>"

            // ── Section 1: Click ──
            + "<h2>Click</h2>"
            + "<button id='btn1' onclick=\"document.getElementById('click-output').textContent='Clicked!'\">Click Me</button>"
            + "<div id='click-output' class='output'>-</div>"

            // ── Section 2: Type Text ──
            + "<h2>Type Text</h2>"
            + "<input type='text' id='input-text' placeholder='Type something...'>"
            + "<div id='type-output' class='output'>-</div>"
            + "<input type='email' id='input-email' placeholder='Email address'>"
            + "<input type='password' id='input-password' placeholder='Password'>"
            + "<input type='number' id='input-number' placeholder='Number'>"
            + "<textarea id='input-textarea' placeholder='Multiline text'></textarea>"

            // ── Section 3: Checkbox ──
            + "<h2>Checkbox</h2>"
            + "<div class='row'>"
            + "<input type='checkbox' id='cb1' onchange=\"document.getElementById('cb-output').textContent='checked='+this.checked\">"
            + "<label for='cb1'>Check this</label>"
            + "</div>"
            + "<div id='cb-output' class='output'>-</div>"

            // ── Section 4: Radio ──
            + "<h2>Radio</h2>"
            + "<div class='row'>"
            + "<input type='radio' name='choice' id='r1' value='A' onchange=\"document.getElementById('radio-output').textContent='selected='+this.value\">"
            + "<label for='r1'>Option A</label><br>"
            + "<input type='radio' name='choice' id='r2' value='B' onchange=\"document.getElementById('radio-output').textContent='selected='+this.value\">"
            + "<label for='r2'>Option B</label>"
            + "</div>"
            + "<div id='radio-output' class='output'>-</div>"

            // ── Section 5: Select ──
            + "<h2>Select</h2>"
            + "<select id='select1' onchange=\"document.getElementById('select-output').textContent='value='+this.value\">"
            + "<option value=''>Choose...</option>"
            + "<option value='apple'>Apple</option>"
            + "<option value='banana'>Banana</option>"
            + "<option value='cherry'>Cherry</option>"
            + "</select>"
            + "<div id='select-output' class='output'>-</div>"

            // ── Section 6: Long Press ──
            + "<h2>Long Press</h2>"
            + "<div id='longpress-area' style='padding:24px;background:#e8f4ff;border-radius:8px;text-align:center'"
            + " onmousedown=\"lpTimer=setTimeout(function(){document.getElementById('lp-output').textContent='Long pressed!'},500)\""
            + " onmouseup=\"clearTimeout(lpTimer)\""
            + " ontouchstart=\"lpTimer=setTimeout(function(){document.getElementById('lp-output').textContent='Long pressed!'},500)\""
            + " ontouchend=\"clearTimeout(lpTimer)\">"
            + "Press and hold here</div>"
            + "<div id='lp-output' class='output'>-</div>"

            // ── Section 7: Scroll ──
            + "<h2>Scroll</h2>"
            + "<div id='scroll-container' style='height:120px;overflow-y:auto;border:1px solid #ddd;border-radius:4px;padding:8px'>"
            + "<div style='height:400px'>"
            + "<p>Scroll content line 1</p>"
            + "<p>Scroll content line 2</p>"
            + "<p>Scroll content line 3</p>"
            + "<p>Scroll content line 4</p>"
            + "<p>Scroll content line 5</p>"
            + "<p id='scroll-target'>Scroll target line</p>"
            + "<p>Scroll content line 7</p>"
            + "<p>Scroll content line 8</p>"
            + "</div></div>"

            // ── Section 8: Links ──
            + "<h2>Links</h2>"
            + "<ul>"
            + "<li><a href='#item1'>Item 1</a></li>"
            + "<li><a href='#item2'>Item 2</a></li>"
            + "<li><a href='#item3'>Item 3</a></li>"
            + "</ul>"

            // ── Section 9: Table ──
            + "<h2>Table</h2>"
            + "<table style='width:100%;border-collapse:collapse'>"
            + "<tr><th style='border:1px solid #ddd;padding:8px;text-align:left'>Name</th>"
            + "<th style='border:1px solid #ddd;padding:8px;text-align:left'>Value</th></tr>"
            + "<tr><td style='border:1px solid #ddd;padding:8px'>Row 1</td>"
            + "<td style='border:1px solid #ddd;padding:8px'>100</td></tr>"
            + "<tr><td style='border:1px solid #ddd;padding:8px'>Row 2</td>"
            + "<td style='border:1px solid #ddd;padding:8px'>200</td></tr>"
            + "</table>"

            + "</body></html>";

        webView.loadDataWithBaseURL("https://baseline.test", html, "text/html", "UTF-8", null);
    }
}
