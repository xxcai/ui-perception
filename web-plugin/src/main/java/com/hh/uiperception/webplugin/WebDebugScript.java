package com.hh.uiperception.webplugin;

/**
 * 调试用 DOM 诊断脚本。
 * 不修改已有序列化逻辑，独立注入以抓取原始 DOM 信息，帮助排查节点遗漏问题。
 *
 * 输出内容:
 * - body 的 outerHTML 长度
 * - 所有 display:contents 元素（可能被子节点穿透但被 isVisible 误判为隐藏）
 * - 零尺寸但有子节点的元素（子节点可能可见但被父节点零尺寸过滤掉）
 * - textContent 很长但 childNodes 很少的元素（文本可能通过 shadow DOM 或其他方式渲染）
 * - Shadow DOM 宿主元素
 */
public final class WebDebugScript {

    private WebDebugScript() {}

    public static String script() {
        return DEBUG_SCRIPT;
    }

    // clang-format off
    private static final String DEBUG_SCRIPT = "(function(){"
        + "var result={};"

        // 1. body outerHTML 长度
        + "result.bodyHtmlLength=document.body?document.body.outerHTML.length:0;"

        // 2. 收集所有 display:contents 元素
        + "var contentsEls=[];"
        + "var all=document.body.getElementsByTagName('*');"
        + "for(var i=0;i<all.length;i++){"
        + "  var el=all[i];"
        + "  var cs=getComputedStyle(el);"
        + "  if(cs.display==='contents'){"
        + "    contentsEls.push({"
        + "      tag:el.tagName,"
        + "      id:el.id||'',"
        + "      cls:(el.className&&typeof el.className==='string')?el.className.substring(0,100):'',"
        + "      childCount:el.childNodes.length,"
        + "      textLength:(el.textContent||'').length"
        + "    });"
        + "  }"
        + "}"
        + "result.displayContentsElements=contentsEls;"

        // 3. 零尺寸但有子节点的元素（非 display:none）
        + "var zeroSizeEls=[];"
        + "for(var i=0;i<all.length;i++){"
        + "  var el=all[i];"
        + "  var cs=getComputedStyle(el);"
        + "  if(cs.display==='none')continue;"
        + "  var r=el.getBoundingClientRect();"
        + "  if(r.width===0&&r.height===0&&el.childNodes.length>0){"
        + "    var textLen=(el.textContent||'').length;"
        + "    zeroSizeEls.push({"
        + "      tag:el.tagName,"
        + "      id:el.id||'',"
        + "      cls:(el.className&&typeof el.className==='string')?el.className.substring(0,100):'',"
        + "      display:cs.display,"
        + "      visibility:cs.visibility,"
        + "      opacity:cs.opacity,"
        + "      childCount:el.childNodes.length,"
        + "      textLength:textLen,"
        + "      textSnippet:textLen>0?(el.textContent||'').substring(0,80):''"
        + "    });"
        + "  }"
        + "}"
        + "result.zeroSizeElementsWithChildren=zeroSizeEls;"

        // 4. textContent 很长但子元素很少的容器（文字可能不在 DOM 子节点中）
        + "var sparseContainers=[];"
        + "for(var i=0;i<all.length;i++){"
        + "  var el=all[i];"
        + "  var textLen=(el.textContent||'').length;"
        + "  if(textLen>200){"
        + "    var childEls=0;"
        + "    for(var j=0;j<el.childNodes.length;j++){"
        + "      if(el.childNodes[j].nodeType===1)childEls++;"
        + "    }"
        + "    if(childEls<=5){"
        + "      sparseContainers.push({"
        + "        tag:el.tagName,"
        + "        id:el.id||'',"
        + "        cls:(el.className&&typeof el.className==='string')?el.className.substring(0,100):'',"
        + "        childElementCount:childEls,"
        + "        textLength:textLen,"
        + "        textSnippet:(el.textContent||'').substring(0,150),"
        + "        innerHTMLLength:(el.innerHTML||'').length,"
        + "        innerHTMLSnippet:(el.innerHTML||'').substring(0,200)"
        + "      });"
        + "    }"
        + "  }"
        + "}"
        + "result.sparseContainers=sparseContainers;"

        // 5. Shadow DOM 宿主
        + "var shadowHosts=[];"
        + "for(var i=0;i<all.length;i++){"
        + "  var el=all[i];"
        + "  if(el.shadowRoot){"
        + "    shadowHosts.push({"
        + "      tag:el.tagName,"
        + "      id:el.id||'',"
        + "      cls:(el.className&&typeof el.className==='string')?el.className.substring(0,100):'',"
        + "      childCount:el.childNodes.length,"
        + "      shadowChildCount:el.shadowRoot.childNodes.length,"
        + "      shadowTextLength:(el.shadowRoot.textContent||'').length"
        + "    });"
        + "  }"
        + "}"
        + "result.shadowHosts=shadowHosts;"

        // 6. body 的 innerHTML 前 2000 字符（用于看 DOM 结构骨架）
        + "result.bodyInnerHtmlSnippet=document.body?(document.body.innerHTML||'').substring(0,2000):'';"

        // 7. 统计：总元素数、各 tag 计数（top 20）
        + "var tagCounts={};"
        + "var totalElements=0;"
        + "for(var i=0;i<all.length;i++){"
        + "  totalElements++;"
        + "  var t=all[i].tagName;"
        + "  tagCounts[t]=(tagCounts[t]||0)+1;"
        + "}"
        + "result.totalElements=totalElements;"
        + "var sorted=Object.keys(tagCounts).sort(function(a,b){return tagCounts[b]-tagCounts[a];});"
        + "var topTags={};"
        + "for(var i=0;i<Math.min(20,sorted.length);i++){topTags[sorted[i]]=tagCounts[sorted[i]];}"
        + "result.tagCounts=topTags;"

        + "return JSON.stringify(result);"
        + "})()";
    // clang-format on
}
