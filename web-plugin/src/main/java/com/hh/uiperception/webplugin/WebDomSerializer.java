package com.hh.uiperception.webplugin;

/**
 * 注入到 WebView 的 DOM 序列化脚本。
 * 遍历 DOM 树，提取每个可见元素的语义信息，输出 JSON。
 *
 * 设计参考 Playwright injected ARIA snapshot pipeline:
 * - DOM 遍历: ariaSnapshot.ts generateAriaTree/visit
 * - Role 映射: roleUtils.ts getAriaRole/getImplicitAriaRole
 * - 可见性: roleUtils.ts isElementHiddenForAria
 * - 可访问名称: roleUtils.ts getElementAccessibleName/getTextAlternativeInternal
 * - 状态: roleUtils.ts getAriaChecked/Disabled/Expanded/Level
 * - Bounds: domUtils.ts computeBox
 * - Shadow DOM: ariaSnapshot.ts visit 内 element.shadowRoot 处理
 */
public final class WebDomSerializer {

    private WebDomSerializer() {}

    static String script() {
        return DOM_SERIALIZER_SCRIPT;
    }

    // clang-format off
    private static final String DOM_SERIALIZER_SCRIPT = "(function(){"
        // ── 1. 隐藏标签过滤 ──────────────────────────────────
        // Playwright ref: roleUtils.ts:306-308 isElementHiddenForAria
        // 直接跳过 STYLE/SCRIPT/NOSCRIPT/TEMPLATE，不做后续处理
        + "var HIDDEN_TAGS=['SCRIPT','STYLE','HEAD','META','LINK','NOSCRIPT','TEMPLATE'];"
        + "function isHiddenTag(t){return HIDDEN_TAGS.indexOf(t)>=0;}"

        // ── 2. 可见性检测 ────────────────────────────────────
        // Playwright ref: roleUtils.ts:305 isElementHiddenForAria
        //   + roleUtils.ts:328 belongsToDisplayNoneOrAriaHiddenOrNonSlotted
        // 不检查 bounding rect 尺寸（Playwright 不检查，inline 元素 rect 可为零但有可见子节点）
        // display:contents 元素自身不可见，但子节点可见时视为可见
        // 祖先链同时检查 display:none 和 aria-hidden=true
        + "function isHiddenForAria(el){"
        + "  if(isHiddenTag(el.tagName))return true;"
        + "  var s=getComputedStyle(el);"
        // display:contents — 透明处理，递归检查子节点
        // Playwright ref: roleUtils.ts:309-318
        + "  if(s.display==='contents'){"
        + "    for(var i=0;i<el.childNodes.length;i++){"
        + "      var c=el.childNodes[i];"
        + "      if(c.nodeType===1&&!isHiddenForAria(c))return false;"
        + "      if(c.nodeType===3){var t=c.textContent.trim();if(t)return false;}"
        + "    }"
        + "    return true;"
        + "  }"
        // CSS 可见性检查
        + "  if(typeof el.checkVisibility==='function'){"
        + "    if(!el.checkVisibility({checkOpacity:true,checkVisibilityCSS:true}))return true;"
        + "  }else{"
        + "    if(s.display==='none')return true;"
        + "    if(s.visibility==='hidden')return true;"
        + "    if(parseFloat(s.opacity)===0)return true;"
        + "  }"
        // 祖先链检查: display:none + aria-hidden=true
        // Playwright ref: roleUtils.ts:328 belongsToDisplayNoneOrAriaHiddenOrNonSlotted
        + "  var p=el.parentElement;"
        + "  while(p){"
        + "    var ps=getComputedStyle(p);"
        + "    if(ps.display==='none')return true;"
        + "    if(p.getAttribute('aria-hidden')==='true')return true;"
        + "    p=p.parentElement;"
        + "  }"
        + "  return false;"
        + "}"

        // ── 3. Role 映射 ────────────────────────────────────
        // Playwright ref: roleUtils.ts:281 getAriaRole
        //   → line 270 getExplicitAriaRole (显式 role 属性)
        //   → line 242 getImplicitAriaRole (kImplicitRoleByTagName 表)
        // 简化: 保留 ~20 个常见 tag 映射，跳过 presentation 继承、
        //       landmark 嵌套检查、TH scope、select multiple/size 分支
        + "function getRole(el){"
        + "  var explicit=el.getAttribute('role');"
        + "  if(explicit&&explicit!=='presentation'&&explicit!=='none')return explicit;"
        + "  var t=el.tagName;"
        + "  if(!t)return 'generic';"
        + "  switch(t){"
        + "    case 'A':return el.hasAttribute('href')?'link':'generic';"
        // Playwright ref: roleUtils.ts:122 input type 分支
        //   简化: checkbox/radio 保持，其他统一为 input
        //   跳过: datalist→combobox, range→slider, hidden→不输出
        + "    case 'INPUT':"
        + "      var tp=(el.type||'text').toLowerCase();"
        + "      if(tp==='checkbox')return 'checkbox';"
        + "      if(tp==='radio')return 'radio';"
        + "      if(tp==='hidden')return null;"
        + "      if(tp==='submit'||tp==='reset'||tp==='button')return 'button';"
        + "      return 'input';"
        + "    case 'BUTTON':return 'button';"
        + "    case 'SELECT':return 'input';"
        + "    case 'TEXTAREA':return 'input';"
        + "    case 'IMG':return 'image';"
        // Playwright ref: roleUtils.ts:140 H1-H6 → heading
        + "    case 'H1':case 'H2':case 'H3':"
        + "    case 'H4':case 'H5':case 'H6':return 'heading';"
        + "    case 'UL':case 'OL':return 'list';"
        + "    case 'LI':return 'listitem';"
        + "    case 'NAV':return 'navigation';"
        + "    case 'SECTION':return 'section';"
        + "    case 'HEADER':return 'toolbar';"
        + "    case 'FOOTER':return 'section';"
        + "    case 'MAIN':return 'screen';"
        + "    case 'FORM':return 'generic';"
        + "    case 'TABLE':return 'list';"
        + "    case 'TR':return 'listitem';"
        + "    case 'IFRAME':return 'generic';"
        + "    default:return 'generic';"
        + "  }"
        + "}"

        // ── 4a. 辅助函数 ────────────────────────────────
        // CSS ::before/::after 伪元素文本提取
        // Playwright ref: roleUtils.ts:944 innerAccumulatedElementText
        + "function pseudoText(s){"
        + "  if(!s||s==='none'||s==='normal')return '';"
        + "  var c=s.charCodeAt(0);"
        + "  if((c===34||c===39)&&s.charCodeAt(s.length-1)===c)return s.substring(1,s.length-1);"
        + "  return '';"
        + "}"
        // embedded control substitution: 表单控件值替代文本
        // Playwright ref: roleUtils.ts:677-719 getTextAlternativeInternal step 2c
        + "function textFromContent(el){"
        + "  var parts=[];"
        + "  for(var i=0;i<el.childNodes.length;i++){"
        + "    var c=el.childNodes[i];"
        + "    if(c.nodeType===3){"
        + "      var t=c.textContent.trim();"
        + "      if(t)parts.push(t);"
        + "    }else if(c.nodeType===1){"
        + "      var tag=c.tagName;"
        + "      if(isHiddenTag(tag))continue;"
        + "      if(tag==='INPUT'){"
        + "        var tp=(c.type||'text').toLowerCase();"
        + "        if(tp==='submit'||tp==='reset'||tp==='button'){parts.push(c.value||tp);}"
        + "        else if(tp!=='hidden'&&tp!=='checkbox'&&tp!=='radio'){parts.push(c.value||'');}"
        + "      }else if(tag==='SELECT'){"
        + "        if(c.selectedIndex>=0&&c.options[c.selectedIndex])parts.push(c.options[c.selectedIndex].text);"
        + "      }else if(tag==='TEXTAREA'){"
        + "        parts.push(c.value||'');"
        + "      }else{"
        + "        var ct=textFromContent(c);"
        + "        if(ct)parts.push(ct);"
        + "      }"
        + "    }"
        + "  }"
        + "  return parts.join(' ');"
        + "}"

        // ── 4. 可访问名称 ──────────────────────────────────
        // Playwright ref: roleUtils.ts:504 getElementAccessibleName
        //   → line 622 getTextAlternativeInternal
        // W3C accname spec 步骤:
        //   Step 2d: aria-label (line 723)
        //   Step 2b: aria-labelledby (line 653) — 只取 textContent，不递归
        //   Step 2e: native HTML naming (line 730-912)
        //     - img: alt (line 780)
        //     - input[submit/button/reset]: value (line 732)
        //     - input[text]: placeholder
        //   Step 2f: name from content (line 918) — textFromContent + ::before/::after
        //   Step 2i: title fallback (line 933)
        // 已实现: embedded control substitution (textFromContent)、
        //         CSS ::before/::after (pseudoText)
        // 跳过: aria-labelledby 递归、label association、SVG title
        + "function getName(el){"
        + "  var a=el.getAttribute('aria-label');"
        + "  if(a&&a.trim())return a.trim();"
        + "  var lb=el.getAttribute('aria-labelledby');"
        + "  if(lb){"
        + "    var ref=document.getElementById(lb);"
        + "    if(ref){var t=ref.textContent.trim();if(t)return t;}"
        + "  }"
        + "  var tag=el.tagName;"
        + "  if(tag==='IMG'){var alt=el.getAttribute('alt');if(alt!=null)return alt;}"
        + "  if(tag==='INPUT'){"
        + "    var tp=(el.type||'text').toLowerCase();"
        + "    if(tp==='submit'||tp==='reset'||tp==='button'){"
        + "      return el.value||tp;"
        + "    }"
        + "    var ph=el.getAttribute('placeholder');"
        + "    if(ph)return ph;"
        + "  }"
        + "  if(tag==='TEXTAREA'){var ph2=el.getAttribute('placeholder');if(ph2)return ph2;}"
        + "  var inner=textFromContent(el);"
        + "  var before=pseudoText(getComputedStyle(el,'::before').content);"
        + "  var after=pseudoText(getComputedStyle(el,'::after').content);"
        + "  var combined=((before?before+' ':'')+inner+(after?' '+after:'')).trim();"
        + "  if(combined){"
        + "    return combined.length>200?combined.substring(0,200)+'...':combined;"
        + "  }"
        + "  var ttl=el.getAttribute('title');"
        + "  if(ttl)return ttl;"
        + "  return '';"
        + "}"

        // ── 5. Bounds 提取 ─────────────────────────────────
        // Playwright ref: domUtils.ts computeBox (line 129)
        //   简化: 直接用 getBoundingClientRect，不做 scroll offset 转换
        + "function getBounds(el){"
        + "  var r=el.getBoundingClientRect();"
        + "  return[Math.round(r.left),Math.round(r.top),Math.round(r.right),Math.round(r.bottom)];"
        + "}"

        // ── 6. 状态提取 ──────────────────────────────────
        // Playwright ref:
        //   getAriaChecked: roleUtils.ts:1004 — checked 属性
        //   getAriaDisabled: roleUtils.ts:1099 — disabled 属性 + fieldset 祖先
        //   getAriaLevel: roleUtils.ts:1083 — H1-H6 level
        // 简化: 直接读 DOM 属性 + aria-pressed，跳过 aria-checked/disabled attribute、
        //       fieldset 祖先链
        + "function getStates(el){"
        + "  var s=[];"
        + "  if(el.disabled)s.push('disabled');"
        + "  if(el.checked)s.push('checked');"
        + "  if(el.indeterminate)s.push('indeterminate');"
        + "  var pressed=el.getAttribute('aria-pressed');"
        + "  if(pressed==='true')s.push('pressed');"
        + "  else if(pressed==='mixed')s.push('pressed=mixed');"
        + "  if(document.activeElement===el)s.push('focused');"
        + "  var m=el.tagName&&el.tagName.match(/^H(\\d)$/);"
        + "  if(m)s.push('level='+m[1]);"
        + "  return s;"
        + "}"

        // ── 7. Clickable 推断 ────────────────────────────
        // Playwright ref: 无直接对应。Playwright 的 ref 分配基于
        //   receivesPointerEvents (roleUtils.ts:1149) 和 interactability。
        //   我们在 capture 阶段做简单推断，transform 阶段再做精确判断。
        + "function isClickable(el){"
        + "  var tag=el.tagName;"
        + "  if(tag==='BUTTON'||tag==='A'||tag==='SELECT')return true;"
        + "  if(tag==='INPUT'){"
        + "    var tp=(el.type||'text').toLowerCase();"
        + "    if(tp!=='hidden')return true;"
        + "  }"
        + "  if(el.getAttribute('role')==='button')return true;"
        + "  if(el.onclick!=null)return true;"
        + "  return false;"
        + "}"

        // ── 8. 主遍历逻辑 ──────────────────────────────
        // Playwright ref: ariaSnapshot.ts:84 generateAriaTree
        //   → line 96 visit(ariaNode, node, parentElementVisible)
        // 关键: 不可见元素的可见子节点 reattach 到父容器（不丢弃子树）
        //   Playwright: processElement(childAriaNode || ariaNode, ...)
        //   我们: serialize(container, node, depth) — container 是输出目标
        + "function serialize(container,node,depth){"
        + "  if(depth>50)return;"

        // 文本节点
        // Playwright ref: ariaSnapshot.ts:108 — 直接收集为字符串
        + "  if(node.nodeType===3){"
        + "    var text=node.textContent.trim();"
        + "    if(!text||text.length===0)return;"
        + "    container.children.push({role:'text',name:text,children:[]});"
        + "    return;"
        + "  }"

        // 非元素节点跳过
        // Playwright ref: ariaSnapshot.ts:112-113
        + "  if(node.nodeType!==1)return;"

        // 可见性判断
        // Playwright ref: ariaSnapshot.ts:123-128
        //   visible → 创建子节点，子节点输出到 childResult
        //   !visible → 不创建子节点，子节点输出到 container（reattach）
        + "  var visible=!isHiddenForAria(node);"

        + "  var result=null;"
        + "  if(visible){"
        // Role 获取，role=null 的元素（如 input[type=hidden]）跳过
        + "    var role=getRole(node);"
        + "    if(!role)return;"
        + "    result={"
        + "      role:role,"
        + "      name:getName(node),"
        + "      states:getStates(node),"
        + "      bounds:getBounds(node),"
        + "      children:[]"
        + "    };"
        // clickable 加入 states
        + "    if(isClickable(node))result.states.push('clickable');"
        // input/textarea 的 value
        // Playwright ref: ariaSnapshot.ts:282-285 — input value 作为子节点
        //   简化: 直接放 states 中
        + "    if(typeof node.value==='string'&&node.value){"
        + "      result.states.push('value='+node.value);"
        + "    }"
        + "    container.children.push(result);"
        + "  }"

        // 子节点输出到 result（可见时）或 container（不可见时，reattach 到父容器）
        // Playwright ref: ariaSnapshot.ts:148 processElement(childAriaNode || ariaNode, ...)
        + "  var target=result||container;"

        // ── 8a. Shadow DOM ───────────────────────────────
        // Playwright ref: ariaSnapshot.ts:168-171
        //   在 light DOM 子节点之后遍历 shadowRoot.firstChild
        //   跳过 slot assignedNodes 处理
        + "  var shadow=node.shadowRoot;"
        + "  if(shadow){"
        + "    var sc=shadow.firstChild;"
        + "    while(sc){"
        + "      serialize(target,sc,depth+1);"
        + "      sc=sc.nextSibling;"
        + "    }"
        + "  }"

        // ── 8b. Light DOM 子节点 ────────────────────────
        // Playwright ref: ariaSnapshot.ts:164 childNodes 遍历
        //   跳过: hasAssignedSlot 检查、aria-owns 补充
        + "  var children=node.childNodes;"
        + "  for(var i=0;i<children.length;i++){"
        + "    serialize(target,children[i],depth+1);"
        + "  }"

        // ── 8c. Same-origin iframe ──────────────────────
        // Playwright 不在 JS 内递归 iframe（由外部 orchestration 处理）
        // 我们通过 contentDocument 在 JS 内穿透同源 iframe
        + "  if(node.tagName==='IFRAME'){"
        + "    try{"
        + "      var doc=node.contentDocument;"
        + "      if(doc&&doc.body){"
        + "        serialize(target,doc.body,depth+1);"
        + "      }"
        + "    }catch(e){}"
        + "  }"
        + "}"

        // ── 9. 入口 ──────────────────────────────────────
        // Playwright ref: injectedScript.ts:313 incrementalAriaSnapshot
        //   → generateAriaTree → renderAriaTree
        //   我们直接从 document.body 开始序列化，不需要 render 阶段（Java 侧做）
        + "var body=document.body;"
        + "if(!body)return JSON.stringify({error:'no body'});"
        + "var root={role:'screen',name:'',states:[],bounds:[],children:[]};"
        + "serialize(root,body,0);"
        + "if(root.children.length===0)return JSON.stringify({error:'empty result'});"
        + "return JSON.stringify({"
        + "  url:location.href,"
        + "  title:document.title||'',"
        + "  root:root"
        + "});"

        + "})()";
    // clang-format on
}
