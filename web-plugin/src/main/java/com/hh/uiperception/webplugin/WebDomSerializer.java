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
        // Playwright ref: roleUtils.ts:323 — <option> inside <select> never hidden by CSS
        + "  if(el.tagName==='OPTION'&&el.closest('select'))return false;"
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
        // Playwright ref: roleUtils.ts:112,120 kAncestorPreventingLandmark
        //   HEADER/FOOTER 在 article/aside/main/nav/section 内时不作为 landmark
        + "function inLandmark(el){"
        + "  var LANDMARKS=['ARTICLE','ASIDE','MAIN','NAV','SECTION'];"
        + "  var p=el.parentElement;"
        + "  while(p){"
        + "    if(LANDMARKS.indexOf(p.tagName)>=0)return true;"
        + "    p=p.parentElement;"
        + "  }"
        + "  return false;"
        + "}"
        // ── 3. Role 映射 ────────────────────────────────────
        // Playwright ref: roleUtils.ts:262 validRoles — 合法 ARIA role 列表
        + "var VALID_ROLES='alert,alertdialog,application,article,association,banner,blockquote," +
          "button,caption,cell,checkbox,code,columnheader,combobox,complementary," +
          "contentinfo,definition,deletion,dialog,directory,document,feed,figure," +
          "form,grid,gridcell,group,heading,img,image,insertion,link,list,listbox," +
          "listitem,log,main,marquee,math,menu,menubar,menuitem,menuitemcheckbox," +
          "menuitemradio,meter,navigation,none,note,option,paragraph,presentation," +
          "progressbar,radio,radiogroup,region,row,rowgroup,rowheader,scrollbar," +
          "search,searchbox,section,separator,slider,slot,spinbutton,status," +
          "strong,subscript,suggestion,superswitch,switch,tab,table,tablist," +
          "tabpanel,term,textbox,time,timer,toolbar,tooltip,tree,treegrid," +
          "treeitem'.split(',');"
        // Playwright ref: roleUtils.ts:59 hasGlobalAriaAttribute
        //   检查元素是否有全局 ARIA 属性（用于 presentation 冲突解决）
        + "var GLOBAL_ARIA=['aria-atomic','aria-busy','aria-controls','aria-current',"
        + "  'aria-describedby','aria-details','aria-disabled','aria-dropeffect',"
        + "  'aria-errormessage','aria-flowto','aria-grabbed','aria-haspopup',"
        + "  'aria-hidden','aria-invalid','aria-keyshortcuts','aria-label',"
        + "  'aria-labelledby','aria-live','aria-owns','aria-relevant',"
        + "  'aria-roledescription'];"
        + "function hasGlobalAria(el){"
        + "  for(var i=0;i<GLOBAL_ARIA.length;i++){"
        + "    if(el.hasAttribute(GLOBAL_ARIA[i]))return true;"
        + "  }"
        + "  return false;"
        + "}"
        // Playwright ref: roleUtils.ts:281 getAriaRole
        //   → line 270 getExplicitAriaRole (显式 role 属性)
        //   → line 242 getImplicitAriaRole (kImplicitRoleByTagName 表)
        + "function getImplicitRole(el){"
        + "  var t=el.tagName;"
        + "  if(!t)return 'generic';"
        + "  switch(t){"
        + "    case 'A':return el.hasAttribute('href')?'link':'generic';"
        // Playwright ref: roleUtils.ts:122-141 input type 分支
        //   完整 type→role 映射，对齐 Playwright 的 kImplicitRoleByTagName
        + "    case 'INPUT':"
        + "      var tp=(el.type||'text').toLowerCase();"
        + "      if(tp==='checkbox')return 'checkbox';"
        + "      if(tp==='radio')return 'radio';"
        + "      if(tp==='hidden')return null;"
        + "      if(tp==='submit'||tp==='reset'||tp==='button'||tp==='image')return 'button';"
        + "      if(tp==='file')return 'button';"
        + "      if(tp==='number')return 'spinbutton';"
        + "      if(tp==='range')return 'slider';"
        + "      if(tp==='search'){"
        + "        if(el.list)return 'combobox';"
        + "        return 'searchbox';"
        + "      }"
        + "      if(tp==='email'||tp==='tel'||tp==='url'||tp===''||tp==='text'){"
        + "        if(el.list)return 'combobox';"
        + "        return 'textbox';"
        + "      }"
        + "      return 'textbox';"
        + "    case 'BUTTON':return 'button';"
        // Playwright ref: roleUtils.ts:158 — multiple/size > 1 → listbox, else → combobox
        + "    case 'SELECT':"
        + "      if(el.multiple||el.size>1)return 'listbox';"
        + "      return 'combobox';"
        + "    case 'TEXTAREA':return 'textbox';"
        // Playwright ref: roleUtils.ts:123 IMG — alt="" 无 title/aria/tabindex → presentation
        + "    case 'IMG':"
        + "      var altImg=el.getAttribute('alt');"
        + "      if(altImg===''&&!el.getAttribute('title')&&!el.getAttribute('aria-label')&&!el.getAttribute('tabindex'))return null;"
        + "      return 'image';"
        // Playwright ref: roleUtils.ts:140 H1-H6 → heading
        + "    case 'H1':case 'H2':case 'H3':"
        + "    case 'H4':case 'H5':case 'H6':return 'heading';"
        + "    case 'UL':case 'OL':case 'MENU':return 'list';"
        + "    case 'LI':return 'listitem';"
        + "    case 'DL':return 'list';"
        + "    case 'DT':return 'term';"
        + "    case 'DD':return 'definition';"
        + "    case 'NAV':return 'navigation';"
        // Playwright ref: roleUtils.ts:157 SECTION — 仅在有 accessible name 时 → region
        + "    case 'SECTION':"
        + "      if(el.getAttribute('aria-label')||el.getAttribute('aria-labelledby')||el.getAttribute('title'))return 'section';"
        + "      return 'generic';"
        // Playwright ref: roleUtils.ts:112,120 HEADER/FOOTER — landmark 嵌套检查
        //   在 article/aside/main/nav/section 内 → null
        + "    case 'HEADER':"
        + "      if(inLandmark(el))return 'generic';"
        + "      return 'toolbar';"
        + "    case 'FOOTER':"
        + "      if(inLandmark(el))return 'generic';"
        + "      return 'section';"
        + "    case 'MAIN':return 'screen';"
        + "    case 'ARTICLE':return 'article';"
        + "    case 'ASIDE':return 'complementary';"
        + "    case 'BLOCKQUOTE':return 'blockquote';"
        + "    case 'HR':return 'separator';"
        + "    case 'P':return 'paragraph';"
        + "    case 'DATALIST':return 'listbox';"
        + "    case 'DETAILS':return 'group';"
        + "    case 'DIALOG':return el.hasAttribute('open')?'dialog':null;"
        + "    case 'METER':return 'meter';"
        + "    case 'OPTGROUP':return 'group';"
        + "    case 'OPTION':return 'option';"
        + "    case 'OUTPUT':return 'status';"
        + "    case 'PROGRESS':return 'progress';"
        + "    case 'AREA':return el.hasAttribute('href')?'link':'generic';"
        // Playwright ref: roleUtils.ts:113 FORM — 仅在有 accessible name 时 → form
        + "    case 'FORM':"
        + "      if(el.getAttribute('aria-label')||el.getAttribute('aria-labelledby')||el.getAttribute('title'))return 'form';"
        + "      return 'generic';"
        // Playwright ref: roleUtils.ts:170-175 TABLE/TR/TD/TH
        + "    case 'TABLE':return 'table';"
        + "    case 'CAPTION':return 'caption';"
        + "    case 'TR':return 'row';"
        // Playwright ref: roleUtils.ts TD — 祖先 table role=grid/treegrid → gridcell
        + "    case 'TD':{"
        + "      var tbl=el.closest('table');"
        + "      var tr=tbl?getRole(tbl):'';"
        + "      return(tr==='grid'||tr==='treegrid')?'gridcell':'cell';"
        + "    }"
        // Playwright ref: roleUtils.ts TH — 分析 sibling 判断 columnheader/rowheader
        + "    case 'TH':{"
        + "      var scope=el.getAttribute('scope');"
        + "      if(scope==='row')return 'rowheader';"
        + "      if(scope==='col'||scope==='colgroup')return 'columnheader';"
        + "      var row=el.closest('tr');"
        + "      if(row){"
        + "        if(row.parentNode&&row.parentNode.tagName==='THEAD')return 'columnheader';"
        + "        var cells=row.cells;"
        + "        for(var ci=0;ci<cells.length;ci++){"
        + "          if(cells[ci]===el){"
        + "            return ci===0?'rowheader':'columnheader';"
        + "          }"
        + "        }"
        + "      }"
        + "      return 'columnheader';"
        + "    }"
        + "    case 'THEAD':case 'TBODY':case 'TFOOT':return 'rowgroup';"
        // Playwright ref: roleUtils.ts:166 SVG -> img
        + "    case 'SVG':return 'image';"
        + "    case 'IFRAME':return 'generic';"
        + "    default:return 'generic';"
        + "  }"
        + "}"
        // Playwright ref: roleUtils.ts:281 getAriaRole
        //   显式 role + presentation 冲突解决 + implicit fallback
        + "function getRole(el){"
        + "  var raw=el.getAttribute('role');"
        + "  if(raw){"
        // Playwright ref: roleUtils.ts:270 — 取首个合法 token
        + "    var tokens=raw.trim().split(/\\s+/);"
        + "    var explicit=null;"
        + "    for(var i=0;i<tokens.length;i++){"
        + "      if(VALID_ROLES.indexOf(tokens[i])>=0){explicit=tokens[i];break;}"
        + "    }"
        // Playwright ref: roleUtils.ts:276-279 — presentation/none 冲突解决
        + "    if(explicit==='presentation'||explicit==='none'){"
        + "      if(hasGlobalAria(el)||el.hasAttribute('tabindex'))return getImplicitRole(el);"
        + "      return null;"
        + "    }"
        + "    if(explicit)return explicit;"
        + "  }"
        + "  return getImplicitRole(el);"
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
        // + roleUtils.ts:944-990 innerAccumulatedElementText block spacing
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
        // SVG <title>: display:none, only contributes to SVG's name
        + "      if(tag==='TITLE'&&c.ownerSVGElement)continue;"
        // Block spacing: non-inline 元素的文本贡献前后加空格
        + "      var cs=getComputedStyle(c);"
        + "      var isBlock=cs.display!=='inline';"
        + "      if(isBlock&&parts.length>0&&parts[parts.length-1]!==' ')parts.push(' ');"
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
        + "      if(isBlock)parts.push(' ');"
        + "    }"
        + "  }"
        + "  return parts.join(' ').replace(/\\s+/g,' ').trim();"
        + "}"

        // ── 4. 可访问名称 ──────────────────────────────────
        // Playwright ref: roleUtils.ts:504 getElementAccessibleName
        //   → line 622 getTextAlternativeInternal
        // W3C accname spec 步骤:
        //   Step 2d: aria-label (line 723)
        //   Step 2b: aria-labelledby (line 653) — 只取 textContent，不递归
        //   Step 2e: native HTML naming (line 730-912)
        //     - label association (line 805-817)
        //     - img: alt (line 780)
        //     - input[submit/button/reset]: value (line 732)
        //     - input[text]: placeholder
        //   Step 2f: name from content (line 918) — textFromContent + ::before/::after
        //   Step 2i: title fallback (line 933)
        // 已实现: embedded control substitution (textFromContent)、
        //         CSS ::before/::after (pseudoText)、label association
        // 跳过: aria-labelledby 递归、SVG title
        + "function getName(el){"
        + "  var a=el.getAttribute('aria-label');"
        + "  if(a&&a.trim())return a.trim();"
        // Playwright ref: roleUtils.ts:653 aria-labelledby — 多 ID 空格分隔
        + "  var lb=el.getAttribute('aria-labelledby');"
        + "  if(lb){"
        + "    var ids=lb.trim().split(/\\s+/);"
        + "    var parts=[];"
        + "    for(var li=0;li<ids.length;li++){"
        + "      var ref=document.getElementById(ids[li]);"
        + "      if(ref){var t=ref.textContent.trim();if(t)parts.push(t);}"
        + "    }"
        + "    if(parts.length)return parts.join(' ');"
        + "  }"
        // Label association: <label for="id">
        // Playwright ref: roleUtils.ts:805-817 getTextAlternativeInternal step 2e
        + "  var elId=el.id;"
        + "  if(elId){"
        + "    var lbl=document.querySelector('label[for=\"'+elId+'\"]');"
        + "    if(lbl){var lt=lbl.textContent.trim();if(lt)return lt;}"
        + "  }"
        // Label wrapping: <label>text <input></label>
        // Playwright ref: roleUtils.ts:805-817 — element.labels 或 el.closest('label')
        + "  var parentLabel=el.closest('label');"
        + "  if(parentLabel){"
        + "    var plt=parentLabel.textContent.trim();"
        + "    if(plt)return plt;"
        + "  }"
        // Native HTML naming (Playwright ref: roleUtils.ts:730-912)
        + "  var tag=el.tagName;"
        + "  if(tag==='IMG'){var alt=el.getAttribute('alt');if(alt!=null)return alt;}"
        + "  if(tag==='INPUT'){"
        + "    var tp=(el.type||'text').toLowerCase();"
        + "    if(tp==='submit'||tp==='reset'||tp==='button'||tp==='image'){"
        + "      return el.value||tp;"
        + "    }"
        + "    if(tp==='file'){return el.value||'Choose File';}"
        + "    var ph=el.getAttribute('placeholder');"
        + "    if(ph)return ph;"
        + "  }"
        + "  if(tag==='TEXTAREA'){var ph2=el.getAttribute('placeholder');if(ph2)return ph2;}"
        // TABLE → caption, FIGURE → figcaption, FIELDSET → legend, DETAILS → summary
        // Playwright ref: roleUtils.ts:780-912 native HTML naming
        + "  if(tag==='TABLE'){var cap=el.querySelector('caption');if(cap){var ct=cap.textContent.trim();if(ct)return ct;}}"
        + "  if(tag==='FIGURE'){var fc=el.querySelector('figcaption');if(fc){var ft=fc.textContent.trim();if(ft)return ft;}}"
        + "  if(tag==='FIELDSET'){var lg=el.querySelector('legend');if(lg){var lgt=lg.textContent.trim();if(lgt)return lgt;}}"
        + "  if(tag==='DETAILS'){var sm=el.querySelector('summary');if(sm){var smt=sm.textContent.trim();if(smt)return smt;}}"
        // SVG → first <title> child
        // Playwright ref: roleUtils.ts:893-904 — SVG name from <title> child element
        + "  if(tag==='SVG'){var st=el.querySelector('title');if(st){var stt=st.textContent.trim();if(stt)return stt;}}"
        // Step 2f: name from content — only if role allows it
        // Playwright ref: roleUtils.ts:491-502 allowsNameFromContent
        + "  var nmRole=getRole(el)||'generic';"
        + "  var nameFromContent='heading,listitem,button,link,treeitem,option,tab,menuitem,"
        + "menuitemcheckbox,menuitemradio,cell,gridcell,columnheader,rowheader,tooltip,term,"
        + "definition,group,note,section,caption,paragraph,separator,alert,log,status,marquee,"
        + "timer,alertdialog,dialog,article,navigation,region,application,form,toolbar,search"
        + "'.split(',');"
        + "  var inner='';"
        + "  if(nameFromContent.indexOf(nmRole)>=0){"
        + "    inner=textFromContent(el);"
        + "  }"
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
        // Playwright ref: ariaSnapshot.ts:264-280 toAriaNode
        //   每个状态只在特定 role 上提取（role-gating），对齐 Playwright 行为
        + "function getStates(el){"
        + "  var s=[];"
        + "  var role=getRole(el)||'generic';"
        // checked — role-gated: native + aria-checked
        // Playwright ref: roleUtils.ts:1004-1037
        + "  var checkedRoles='checkbox,radio,menuitemcheckbox,option,switch,menuitemradio,treeitem';"
        + "  if(el.checked&&checkedRoles.indexOf(role)>=0){s.push('checked');}"
        + "  if(el.indeterminate&&checkedRoles.indexOf(role)>=0)s.push('indeterminate');"
        + "  var ac=el.getAttribute('aria-checked');"
        + "  if(ac&&checkedRoles.indexOf(role)>=0){"
        + "    if(ac==='true'&&!el.checked)s.push('checked');"
        + "    else if(ac==='mixed'&&!el.indeterminate)s.push('indeterminate');"
        + "  }"
        // disabled — role-gated + aria-disabled + fieldset 继承
        // Playwright ref: roleUtils.ts:1099-1121
        + "  var disabledRoles='application,button,composite,gridcell,group,input,link,menuitem,"
        + "scrollbar,separator,tab,checkbox,columnheader,combobox,grid,listbox,menu,menubar,"
        + "menuitemcheckbox,menuitemradio,option,radio,radiogroup,row,rowheader,searchbox,"
        + "select,slider,spinbutton,switch,tablist,textbox,toolbar,tree,treegrid,treeitem';"
        + "  if(isDisabled(el)&&disabledRoles.indexOf(role)>=0)s.push('disabled');"
        // expanded — role-gated + DETAILS.open
        // Playwright ref: roleUtils.ts:1066-1080
        + "  var expandedRoles='application,button,checkbox,combobox,gridcell,link,listbox,"
        + "menuitem,row,rowheader,tab,treeitem,columnheader,menuitemcheckbox,menuitemradio,switch';"
        + "  if(expandedRoles.indexOf(role)>=0){"
        + "    var isExp=el.getAttribute('aria-expanded');"
        + "    if(el.tagName==='DETAILS')isExp=el.open?'true':'false';"
        + "    if(isExp!==null&&isExp!==undefined)s.push(isExp==='true'||isExp===true?'expanded':'collapsed');"
        + "  }"
        // pressed — role-gated (only button)
        // Playwright ref: roleUtils.ts:1053
        + "  var pressed=el.getAttribute('aria-pressed');"
        + "  if(pressed&&role==='button'){"
        + "    if(pressed==='true')s.push('pressed');"
        + "    else if(pressed==='mixed')s.push('pressed=mixed');"
        + "  }"
        // selected — role-gated + native OPTION.selected
        // Playwright ref: roleUtils.ts:993-1001
        + "  var selectedRoles='gridcell,option,row,tab,rowheader,columnheader,treeitem';"
        + "  if(selectedRoles.indexOf(role)>=0){"
        + "    var isSel=el.getAttribute('aria-selected');"
        + "    if(el.tagName==='OPTION'&&el.selected)isSel='true';"
        + "    if(isSel==='true')s.push('selected');"
        + "  }"
        // readonly — role-gated
        // Playwright ref: roleUtils.ts:1039-1050
        + "  var readonlyRoles='checkbox,combobox,grid,gridcell,listbox,radiogroup,slider,"
        + "spinbutton,textbox,columnheader,rowheader,searchbox,switch,treegrid';"
        + "  if(readonlyRoles.indexOf(role)>=0){"
        + "    if(el.readOnly||el.getAttribute('aria-readonly')==='true')s.push('readonly');"
        + "  }"
        // invalid — aria-invalid + HTML5 validity
        // Playwright ref: roleUtils.ts:562-580
        + "  var inv=el.getAttribute('aria-invalid');"
        + "  if(inv==='true'){s.push('invalid');}"
        + "  else if(inv==='grammar')s.push('invalid=grammar');"
        + "  else if(inv==='spelling')s.push('invalid=spelling');"
        + "  else if(typeof el.validity==='object'&&el.validity&&!el.validity.valid)s.push('invalid');"
        // focused
        + "  if(document.activeElement===el)s.push('focused');"
        // level — role-gated
        + "  if('heading,listitem,row,treeitem'.indexOf(role)>=0){"
        + "    var lv=el.getAttribute('aria-level');"
        + "    if(lv){s.push('level='+lv);}"
        + "    else{var m=el.tagName&&el.tagName.match(/^H(\\d)$/);if(m)s.push('level='+m[1]);}"
        + "  }"
        + "  return s;"
        + "}"
        // isDisabled: native disabled + aria-disabled + fieldset disabled 继承
        // Playwright ref: roleUtils.ts:1099-1121 getAriaDisabled
        + "function isDisabled(el){"
        + "  if(el.disabled)return true;"
        + "  if(el.getAttribute('aria-disabled')==='true')return true;"
        // fieldset[disabled] 继承（排除 legend 内部）
        // Playwright ref: roleUtils.ts:1115 belongsToDisabledFieldSet
        + "  var p=el.parentElement;"
        + "  while(p){"
        + "    if(p.tagName==='FIELDSET'&&p.disabled){"
        + "      var lg=p.querySelector('legend');"
        + "      if(lg&&!lg.contains(el))return true;"
        + "    }"
        + "    var ad=p.getAttribute('aria-disabled');"
        + "    if(ad==='true')return true;"
        + "    p=p.parentElement;"
        + "  }"
        + "  return false;"
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

        // ── 7a. 元素索引计数器 ────────────────────────────
        // 为每个序列化的 DOM 元素分配递增索引，同时写入 __pr_idx 属性
        // 用于后续操作时通过索引反查 DOM 元素
        + "var prIdxCounter=0;"

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
        //   textbox 内部的文本不作为子节点（已通过 element.value 捕获）
        + "  if(node.nodeType===3){"
        + "    var text=node.textContent.trim();"
        + "    if(!text||text.length===0)return;"
        + "    if(container._isTextbox)return;"
        + "    container.children.push({role:'text',name:text,children:[]});"
        + "    return;"
        + "  }"

        // 非元素节点跳过
        // Playwright ref: ariaSnapshot.ts:112-113
        + "  if(node.nodeType!==1)return;"

        // Hidden tag 直接跳过整棵子树（不做 reattach）
        // Playwright ref: ariaSnapshot.ts:125-126 — aria 模式下 hidden 元素直接 return
        //   isElementIgnoredForAria 返回 true 时，不遍历子节点
        + "  if(isHiddenTag(node.tagName))return;"
        // SVG <title>: display:none, only contributes to SVG's name (Playwright ref: roleUtils.ts:893)
        + "  if(node.tagName==='TITLE'&&node.ownerSVGElement)return;"

        // 可见性判断
        // Playwright ref: ariaSnapshot.ts:123-128
        //   visible → 创建子节点，子节点输出到 childResult
        //   !visible → 不创建子节点，子节点输出到 container（reattach）
        + "  var visible=!isHiddenForAria(node);"

        + "  var result=null;"
        + "  if(visible){"
        // Role 获取，role=null 的元素（如 input[type=hidden]、presentation）跳过
        + "    var role=getRole(node);"
        + "    if(!role)return;"
        // Playwright ref: ariaSnapshot.ts:249 — inline generic with single text child → skip node
        //   文本会在后续子节点遍历中自然挂到 container
        + "    if(role==='generic'){"
        + "      var cs=getComputedStyle(node);"
        + "      if(cs.display==='inline'&&node.childNodes.length===1&&node.childNodes[0].nodeType===3)return;"
        + "    }"
        + "    var prIdx=prIdxCounter++;"
        + "    try{node.setAttribute('__pr_idx',prIdx);}catch(e){}"
        + "    result={"
        + "      role:role,"
        + "      name:getName(node),"
        + "      states:getStates(node),"
        + "      bounds:getBounds(node),"
        + "      __pr_idx:prIdx,"
        + "      children:[]"
        + "    };"
        // clickable 加入 states
        + "    if(isClickable(node))result.states.push('clickable');"
        // Playwright ref: ariaSnapshot.ts:107 — textbox 内文本不作为子节点
        + "    result._isTextbox=(role==='textbox');"
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

        // Playwright ref: ariaSnapshot.ts:158,177 — ::before/::after 伪元素作为子节点
        + "  if(result){"
        + "    var before=pseudoText(getComputedStyle(node,'::before').content);"
        + "    if(before)result.children.push({role:'text',name:before,children:[]});"
        + "  }"

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

        // ── 8b-2. ::after 伪元素 ──────────────────────
        // Playwright ref: ariaSnapshot.ts:177
        + "  if(result){"
        + "    var after=pseudoText(getComputedStyle(node,'::after').content);"
        + "    if(after)result.children.push({role:'text',name:after,children:[]});"
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
