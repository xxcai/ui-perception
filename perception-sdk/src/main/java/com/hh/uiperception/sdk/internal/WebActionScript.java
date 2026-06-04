package com.hh.uiperception.sdk.internal;

/**
 * 注入到 WebView 执行操作的 JS 脚本。
 * 使用 __pr_idx 属性定位元素（序列化时由 WebDomSerializer 设置）。
 */
final class WebActionScript {

    private WebActionScript() {}

    static String typeText(int prIdx, String text, boolean clear) {
        String escaped = escapeJs(text);
        return "(function(){"
            + "var el=document.querySelector('[__pr_idx=\"" + prIdx + "\"]');"
            + "if(!el)return JSON.stringify({status:'error',error:'Element not found'});"
            + "el.focus();"
            + (clear
                ? "if(el.setSelectionRange){el.setSelectionRange(0,el.value.length);}"
                  + "else if(el.createTextRange){var r=el.createTextRange();r.select();}"
                : "")
            // Strategy W1: execCommand('insertText') — closest to CDP Input.insertText
            + "if(document.execCommand){"
            + (clear ? "document.execCommand('selectAll',false,null);" : "")
            + "  if(document.execCommand('insertText',false,\"" + escaped + "\")){"
            + "    return JSON.stringify({status:'success',result:{method:'execCommand'}});"
            + "  }"
            + "}"
            // Strategy W2: direct value assignment + synthetic events
            + (clear ? "el.value='';" : "")
            + "el.value=el.value+\"" + escaped + "\";"
            + "el.dispatchEvent(new Event('input',{bubbles:true}));"
            + "el.dispatchEvent(new Event('change',{bubbles:true}));"
            + "return JSON.stringify({status:'success',result:{method:'value'}});"
            + "})()";
    }

    static String selectOption(int prIdx, String value) {
        String escaped = escapeJs(value);
        return "(function(){"
            + "var el=document.querySelector('[__pr_idx=\"" + prIdx + "\"]');"
            + "if(!el)return JSON.stringify({status:'error',error:'Element not found'});"
            + "if(el.tagName!=='SELECT')return JSON.stringify({status:'error',error:'Not a select element'});"
            + "var found=false;"
            + "for(var i=0;i<el.options.length;i++){"
            + "  if(el.options[i].value===\"" + escaped + "\"||el.options[i].text===\"" + escaped + "\"){"
            + "    el.selectedIndex=i;"
            + "    el.options[i].selected=true;"
            + "    found=true;"
            + "    break;"
            + "  }"
            + "}"
            + "if(!found)return JSON.stringify({status:'error',error:'Option not found: " + escaped + "'});"
            + "el.dispatchEvent(new Event('input',{bubbles:true}));"
            + "el.dispatchEvent(new Event('change',{bubbles:true}));"
            + "return JSON.stringify({status:'success',result:{value:el.value}});"
            + "})()";
    }

    static String getCheckedState(int prIdx) {
        return "(function(){"
            + "var el=document.querySelector('[__pr_idx=\"" + prIdx + "\"]');"
            + "if(!el)return JSON.stringify({status:'error',error:'Element not found'});"
            + "var checked=el.checked===true;"
            + "var ariaChecked=el.getAttribute('aria-checked');"
            + "return JSON.stringify({status:'success',result:{checked:checked,ariaChecked:ariaChecked}});"
            + "})()";
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
