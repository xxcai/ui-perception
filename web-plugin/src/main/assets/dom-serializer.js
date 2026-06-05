/**
 * DOM Serializer — injected into WebView as IIFE.
 * Traverses the DOM tree, extracts semantic info for each visible element, outputs JSON.
 *
 * Design reference: Playwright injected ARIA snapshot pipeline
 *   - DOM traversal:  ariaSnapshot.ts generateAriaTree / visit
 *   - Role mapping:   roleUtils.ts getAriaRole / getImplicitAriaRole
 *   - Visibility:     roleUtils.ts isElementHiddenForAria
 *   - Accessible name: roleUtils.ts getElementAccessibleName / getTextAlternativeInternal
 *   - States:         roleUtils.ts getAriaChecked / Disabled / Expanded / Level
 *   - Bounds:         domUtils.ts computeBox
 *   - Shadow DOM:     ariaSnapshot.ts visit — element.shadowRoot handling
 */
(function () {

// ═══════════════════════════════════════════════════════════════════
// 1. Hidden tag filter & visibility detection
//    Playwright ref: roleUtils.ts isElementHiddenForAria
// ═══════════════════════════════════════════════════════════════════

var HIDDEN_TAGS = ['SCRIPT', 'STYLE', 'HEAD', 'META', 'LINK', 'NOSCRIPT', 'TEMPLATE'];

function isHiddenTag(t) {
  return HIDDEN_TAGS.indexOf(t) >= 0;
}

/**
 * Check if an element is hidden from the ARIA accessibility tree.
 * Playwright ref: roleUtils.ts:305 isElementHiddenForAria
 *   + roleUtils.ts:328 belongsToDisplayNoneOrAriaHiddenOrNonSlotted
 *
 * - Does NOT check bounding rect size (inline elements can have zero rect but visible children)
 * - display:contents elements: transparent, recurse into children
 * - Ancestor chain: checks both display:none and aria-hidden=true
 */
function isHiddenForAria(el) {
  if (isHiddenTag(el.tagName)) return true;
  // <option> inside <select> is never hidden by CSS
  // Playwright ref: roleUtils.ts:323
  if (el.tagName === 'OPTION' && el.closest('select')) return false;

  var s = getComputedStyle(el);

  // display:contents — transparent container, check if any child is visible
  // Playwright ref: roleUtils.ts:309-318
  if (s.display === 'contents') {
    for (var i = 0; i < el.childNodes.length; i++) {
      var c = el.childNodes[i];
      if (c.nodeType === 1 && !isHiddenForAria(c)) return false;
      if (c.nodeType === 3) { var t = c.textContent.trim(); if (t) return false; }
    }
    return true;
  }

  // CSS visibility check
  if (typeof el.checkVisibility === 'function') {
    if (!el.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true })) return true;
  } else {
    if (s.display === 'none') return true;
    if (s.visibility === 'hidden') return true;
    if (parseFloat(s.opacity) === 0) return true;
  }

  // Ancestor chain: display:none + aria-hidden=true
  // Playwright ref: roleUtils.ts:328 belongsToDisplayNoneOrAriaHiddenOrNonSlotted
  var p = el.parentElement;
  while (p) {
    var ps = getComputedStyle(p);
    if (ps.display === 'none') return true;
    if (p.getAttribute('aria-hidden') === 'true') return true;
    p = p.parentElement;
  }
  return false;
}

// ═══════════════════════════════════════════════════════════════════
// 2. Role mapping (explicit + implicit)
//    Playwright ref: roleUtils.ts getAriaRole / getImplicitAriaRole
// ═══════════════════════════════════════════════════════════════════

/**
 * Check if an ancestor is a landmark element (affects HEADER/FOOTER role).
 * Playwright ref: roleUtils.ts:112,120 kAncestorPreventingLandmark
 */
function inLandmark(el) {
  var LANDMARKS = ['ARTICLE', 'ASIDE', 'MAIN', 'NAV', 'SECTION'];
  var p = el.parentElement;
  while (p) {
    if (LANDMARKS.indexOf(p.tagName) >= 0) return true;
    p = p.parentElement;
  }
  return false;
}

// Valid ARIA roles — Playwright ref: roleUtils.ts:262 validRoles
var VALID_ROLES = 'alert,alertdialog,application,article,association,banner,blockquote,' +
  'button,caption,cell,checkbox,code,columnheader,combobox,complementary,' +
  'contentinfo,definition,deletion,dialog,directory,document,feed,figure,' +
  'form,grid,gridcell,group,heading,img,image,insertion,link,list,listbox,' +
  'listitem,log,main,marquee,math,menu,menubar,menuitem,menuitemcheckbox,' +
  'menuitemradio,meter,navigation,none,note,option,paragraph,presentation,' +
  'progressbar,radio,radiogroup,region,row,rowgroup,rowheader,scrollbar,' +
  'search,searchbox,section,separator,slider,slot,spinbutton,status,' +
  'strong,subscript,suggestion,superswitch,switch,tab,table,tablist,' +
  'tabpanel,term,textbox,time,timer,toolbar,tooltip,tree,treegrid,' +
  'treeitem'.split(',');

// Global ARIA attributes — for presentation/none conflict resolution
// Playwright ref: roleUtils.ts:59 hasGlobalAriaAttribute
var GLOBAL_ARIA = ['aria-atomic', 'aria-busy', 'aria-controls', 'aria-current',
  'aria-describedby', 'aria-details', 'aria-disabled', 'aria-dropeffect',
  'aria-errormessage', 'aria-flowto', 'aria-grabbed', 'aria-haspopup',
  'aria-hidden', 'aria-invalid', 'aria-keyshortcuts', 'aria-label',
  'aria-labelledby', 'aria-live', 'aria-owns', 'aria-relevant',
  'aria-roledescription'];

function hasGlobalAria(el) {
  for (var i = 0; i < GLOBAL_ARIA.length; i++) {
    if (el.hasAttribute(GLOBAL_ARIA[i])) return true;
  }
  return false;
}

/**
 * Get implicit ARIA role from tag name.
 * Playwright ref: roleUtils.ts kImplicitRoleByTagName (line 100-175)
 *   Returns null for elements that should be excluded (e.g., input[hidden], img[alt=""]).
 *   Returns 'generic' for tags with no implicit role.
 */
function getImplicitRole(el) {
  var t = el.tagName;
  if (!t) return 'generic';

  switch (t) {
    case 'A': return el.hasAttribute('href') ? 'link' : 'generic';

    // Input type → role mapping (Playwright ref: roleUtils.ts:122-141)
    case 'INPUT':
      var tp = (el.type || 'text').toLowerCase();
      if (tp === 'checkbox') return 'checkbox';
      if (tp === 'radio') return 'radio';
      if (tp === 'hidden') return null;
      if (tp === 'submit' || tp === 'reset' || tp === 'button' || tp === 'image') return 'button';
      if (tp === 'file') return 'button';
      if (tp === 'number') return 'spinbutton';
      if (tp === 'range') return 'slider';
      if (tp === 'search') {
        if (el.list) return 'combobox';
        return 'searchbox';
      }
      if (tp === 'email' || tp === 'tel' || tp === 'url' || tp === '' || tp === 'text') {
        if (el.list) return 'combobox';
        return 'textbox';
      }
      return 'textbox';

    case 'BUTTON': return 'button';
    // Playwright ref: roleUtils.ts:158 — multiple/size > 1 → listbox
    case 'SELECT':
      if (el.multiple || el.size > 1) return 'listbox';
      return 'combobox';
    case 'TEXTAREA': return 'textbox';

    // IMG with alt="" and no other accessible attributes → excluded (Playwright ref: roleUtils.ts:123)
    case 'IMG':
      var altImg = el.getAttribute('alt');
      if (altImg === '' && !el.getAttribute('title') && !el.getAttribute('aria-label') && !el.getAttribute('tabindex')) return null;
      return 'image';

    // Headings (Playwright ref: roleUtils.ts:140)
    case 'H1': case 'H2': case 'H3':
    case 'H4': case 'H5': case 'H6': return 'heading';

    // Lists
    case 'UL': case 'OL': case 'MENU': return 'list';
    case 'LI': return 'listitem';
    case 'DL': return 'list';
    case 'DT': return 'term';
    case 'DD': return 'definition';
    case 'NAV': return 'navigation';

    // SECTION with accessible name → section, otherwise generic (Playwright ref: roleUtils.ts:157)
    case 'SECTION':
      if (el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.getAttribute('title')) return 'section';
      return 'generic';

    // HEADER/FOOTER: landmark nesting check (Playwright ref: roleUtils.ts:112,120)
    case 'HEADER':
      if (inLandmark(el)) return 'generic';
      return 'toolbar';
    case 'FOOTER':
      if (inLandmark(el)) return 'generic';
      return 'section';

    case 'MAIN': return 'screen';

    // Semantic HTML5 elements
    case 'ARTICLE': return 'article';
    case 'ASIDE': return 'complementary';
    case 'BLOCKQUOTE': return 'blockquote';
    case 'HR': return 'separator';
    case 'P': return 'paragraph';

    // Form-related
    case 'DATALIST': return 'listbox';
    case 'DETAILS': return 'group';
    case 'DIALOG': return el.hasAttribute('open') ? 'dialog' : null;
    case 'METER': return 'meter';
    case 'OPTGROUP': return 'group';
    case 'OPTION': return 'option';
    case 'OUTPUT': return 'status';
    case 'PROGRESS': return 'progress';
    case 'AREA': return el.hasAttribute('href') ? 'link' : 'generic';
    // FORM with accessible name → form (Playwright ref: roleUtils.ts:113)
    case 'FORM':
      if (el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.getAttribute('title')) return 'form';
      return 'generic';

    // Table structure (Playwright ref: roleUtils.ts:170-175)
    case 'TABLE': return 'table';
    case 'CAPTION': return 'caption';
    case 'TR': return 'row';
    // TD: ancestor table role=grid/treegrid → gridcell (Playwright ref: roleUtils.ts TD)
    case 'TD': {
      var tbl = el.closest('table');
      var tr = tbl ? getRole(tbl) : '';
      return (tr === 'grid' || tr === 'treegrid') ? 'gridcell' : 'cell';
    }
    // TH: sibling analysis for columnheader/rowheader (Playwright ref: roleUtils.ts TH)
    case 'TH': {
      var scope = el.getAttribute('scope');
      if (scope === 'row') return 'rowheader';
      if (scope === 'col' || scope === 'colgroup') return 'columnheader';
      var row = el.closest('tr');
      if (row) {
        if (row.parentNode && row.parentNode.tagName === 'THEAD') return 'columnheader';
        var cells = row.cells;
        for (var ci = 0; ci < cells.length; ci++) {
          if (cells[ci] === el) {
            return ci === 0 ? 'rowheader' : 'columnheader';
          }
        }
      }
      return 'columnheader';
    }
    case 'THEAD': case 'TBODY': case 'TFOOT': return 'rowgroup';

    // SVG → img role (Playwright ref: roleUtils.ts:166)
    case 'SVG': return 'image';

    case 'IFRAME': return 'generic';
    default: return 'generic';
  }
}

/**
 * Get resolved ARIA role: explicit (role attribute) → implicit (tag-based).
 * Playwright ref: roleUtils.ts:281 getAriaRole
 *   - Validates role attribute tokens, takes first valid one
 *   - Handles presentation/none conflict resolution
 */
function getRole(el) {
  var raw = el.getAttribute('role');
  if (raw) {
    // Take first valid token (Playwright ref: roleUtils.ts:270)
    var tokens = raw.trim().split(/\s+/);
    var explicit = null;
    for (var i = 0; i < tokens.length; i++) {
      if (VALID_ROLES.indexOf(tokens[i]) >= 0) { explicit = tokens[i]; break; }
    }
    // presentation/none conflict resolution (Playwright ref: roleUtils.ts:276-279)
    if (explicit === 'presentation' || explicit === 'none') {
      if (hasGlobalAria(el) || el.hasAttribute('tabindex')) return getImplicitRole(el);
      return null;
    }
    if (explicit) return explicit;
  }
  return getImplicitRole(el);
}

// ═══════════════════════════════════════════════════════════════════
// 3. Accessible name computation
//    Playwright ref: roleUtils.ts getTextAlternativeInternal
// ═══════════════════════════════════════════════════════════════════

/**
 * Extract text from CSS ::before/::after pseudo-element content.
 * Playwright ref: roleUtils.ts:944 innerAccumulatedElementText
 */
function pseudoText(s) {
  if (!s || s === 'none' || s === 'normal') return '';
  var c = s.charCodeAt(0);
  if ((c === 34 || c === 39) && s.charCodeAt(s.length - 1) === c) return s.substring(1, s.length - 1);
  return '';
}

/**
 * Recursively collect text content from child nodes, with embedded control substitution.
 * Playwright ref: roleUtils.ts:677-719 step 2c + roleUtils.ts:944-990 block spacing
 */
function textFromContent(el) {
  var parts = [];
  for (var i = 0; i < el.childNodes.length; i++) {
    var c = el.childNodes[i];
    if (c.nodeType === 3) {
      var t = c.textContent.trim();
      if (t) parts.push(t);
    } else if (c.nodeType === 1) {
      var tag = c.tagName;
      if (isHiddenTag(tag)) continue;
      // SVG <title> is display:none — only contributes to SVG's own name, not to parent content
      if (tag === 'TITLE' && c.ownerSVGElement) continue;

      var cs = getComputedStyle(c);
      var isBlock = cs.display !== 'inline';
      if (isBlock && parts.length > 0 && parts[parts.length - 1] !== ' ') parts.push(' ');

      // Embedded control substitution: use form control values instead of text content
      if (tag === 'INPUT') {
        var tp = (c.type || 'text').toLowerCase();
        if (tp === 'submit' || tp === 'reset' || tp === 'button') { parts.push(c.value || tp); }
        else if (tp !== 'hidden' && tp !== 'checkbox' && tp !== 'radio') { parts.push(c.value || ''); }
      } else if (tag === 'SELECT') {
        if (c.selectedIndex >= 0 && c.options[c.selectedIndex]) parts.push(c.options[c.selectedIndex].text);
      } else if (tag === 'TEXTAREA') {
        parts.push(c.value || '');
      } else {
        var ct = textFromContent(c);
        if (ct) parts.push(ct);
      }
      if (isBlock) parts.push(' ');
    }
  }
  return parts.join(' ').replace(/\s+/g, ' ').trim();
}

/**
 * Compute accessible name for an element following W3C accname spec steps.
 * Playwright ref: roleUtils.ts:504 getElementAccessibleName → line 622 getTextAlternativeInternal
 *
 * Steps implemented:
 *   Step 2d: aria-label (line 723)
 *   Step 2b: aria-labelledby — multi-ID, space-separated (line 653)
 *   Step 2e: native HTML naming (line 730-912)
 *     - label[for] association (line 805-817)
 *     - label wrapping: <label>text <input></label>
 *     - img: alt (line 780), input submit/button: value (line 732), input[text]: placeholder
 *     - TABLE→caption, FIGURE→figcaption, FIELDSET→legend, DETAILS→summary
 *     - SVG→first <title> child (line 893-904)
 *   Step 2f: name from content — role-gated (line 918) + ::before/::after
 *   Step 2i: title fallback (line 933)
 */
function getName(el) {
  // Step 2d: aria-label
  var a = el.getAttribute('aria-label');
  if (a && a.trim()) return a.trim();

  // Step 2b: aria-labelledby — multi ID (Playwright ref: roleUtils.ts:653)
  var lb = el.getAttribute('aria-labelledby');
  if (lb) {
    var ids = lb.trim().split(/\s+/);
    var parts = [];
    for (var li = 0; li < ids.length; li++) {
      var ref = document.getElementById(ids[li]);
      if (ref) { var t = ref.textContent.trim(); if (t) parts.push(t); }
    }
    if (parts.length) return parts.join(' ');
  }

  // Step 2e: label[for] association (Playwright ref: roleUtils.ts:805-817)
  var elId = el.id;
  if (elId) {
    var lbl = document.querySelector('label[for="' + elId + '"]');
    if (lbl) { var lt = lbl.textContent.trim(); if (lt) return lt; }
  }

  // Label wrapping: <label>text <input></label>
  var parentLabel = el.closest('label');
  if (parentLabel) {
    var plt = parentLabel.textContent.trim();
    if (plt) return plt;
  }

  // Native HTML naming (Playwright ref: roleUtils.ts:730-912)
  var tag = el.tagName;
  if (tag === 'IMG') { var alt = el.getAttribute('alt'); if (alt != null) return alt; }
  if (tag === 'INPUT') {
    var tp = (el.type || 'text').toLowerCase();
    if (tp === 'submit' || tp === 'reset' || tp === 'button' || tp === 'image') {
      return el.value || tp;
    }
    if (tp === 'file') { return el.value || 'Choose File'; }
    var ph = el.getAttribute('placeholder');
    if (ph) return ph;
  }
  if (tag === 'TEXTAREA') { var ph2 = el.getAttribute('placeholder'); if (ph2) return ph2; }

  // Container → child element naming
  if (tag === 'TABLE') { var cap = el.querySelector('caption'); if (cap) { var ct = cap.textContent.trim(); if (ct) return ct; } }
  if (tag === 'FIGURE') { var fc = el.querySelector('figcaption'); if (fc) { var ft = fc.textContent.trim(); if (ft) return ft; } }
  if (tag === 'FIELDSET') { var lg = el.querySelector('legend'); if (lg) { var lgt = lg.textContent.trim(); if (lgt) return lgt; } }
  if (tag === 'DETAILS') { var sm = el.querySelector('summary'); if (sm) { var smt = sm.textContent.trim(); if (smt) return smt; } }
  // SVG → first <title> child (Playwright ref: roleUtils.ts:893-904)
  if (tag === 'SVG') { var st = el.querySelector('title'); if (st) { var stt = st.textContent.trim(); if (stt) return stt; } }

  // Step 2f: name from content — only if role allows it
  // Playwright ref: roleUtils.ts:491-502 allowsNameFromContent
  var nmRole = getRole(el) || 'generic';
  var nameFromContent = 'heading,listitem,button,link,treeitem,option,tab,menuitem,' +
    'menuitemcheckbox,menuitemradio,cell,gridcell,columnheader,rowheader,tooltip,term,' +
    'definition,group,note,section,caption,paragraph,separator,alert,log,status,marquee,' +
    'timer,alertdialog,dialog,article,navigation,region,application,form,toolbar,search'
    .split(',');
  var inner = '';
  if (nameFromContent.indexOf(nmRole) >= 0) {
    inner = textFromContent(el);
  }

  var before = pseudoText(getComputedStyle(el, '::before').content);
  var after = pseudoText(getComputedStyle(el, '::after').content);
  var combined = ((before ? before + ' ' : '') + inner + (after ? ' ' + after : '')).trim();
  if (combined) {
    return combined.length > 200 ? combined.substring(0, 200) + '...' : combined;
  }

  // Step 2i: title fallback
  var ttl = el.getAttribute('title');
  if (ttl) return ttl;
  return '';
}

// ═══════════════════════════════════════════════════════════════════
// 4. Bounds extraction
//    Playwright ref: domUtils.ts computeBox (line 129)
// ═══════════════════════════════════════════════════════════════════

function getBounds(el) {
  var r = el.getBoundingClientRect();
  return [Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom)];
}

// ═══════════════════════════════════════════════════════════════════
// 5. State extraction
//    Playwright ref: ariaSnapshot.ts:264-280 toAriaNode
//    Each state is extracted only on specific roles (role-gating).
// ═══════════════════════════════════════════════════════════════════

function getStates(el) {
  var s = [];
  var role = getRole(el) || 'generic';

  // checked — native + aria-checked (Playwright ref: roleUtils.ts:1004-1037)
  var checkedRoles = 'checkbox,radio,menuitemcheckbox,option,switch,menuitemradio,treeitem';
  if (el.checked && checkedRoles.indexOf(role) >= 0) { s.push('checked'); }
  if (el.indeterminate && checkedRoles.indexOf(role) >= 0) s.push('indeterminate');
  var ac = el.getAttribute('aria-checked');
  if (ac && checkedRoles.indexOf(role) >= 0) {
    if (ac === 'true' && !el.checked) s.push('checked');
    else if (ac === 'mixed' && !el.indeterminate) s.push('indeterminate');
  }

  // disabled — role-gated + aria-disabled + fieldset inheritance
  // Playwright ref: roleUtils.ts:1099-1121
  var disabledRoles = 'application,button,composite,gridcell,group,input,link,menuitem,' +
    'scrollbar,separator,tab,checkbox,columnheader,combobox,grid,listbox,menu,menubar,' +
    'menuitemcheckbox,menuitemradio,option,radio,radiogroup,row,rowheader,searchbox,' +
    'select,slider,spinbutton,switch,tablist,textbox,toolbar,tree,treegrid,treeitem';
  if (isDisabled(el) && disabledRoles.indexOf(role) >= 0) s.push('disabled');

  // expanded — role-gated + DETAILS.open (Playwright ref: roleUtils.ts:1066-1080)
  var expandedRoles = 'application,button,checkbox,combobox,gridcell,link,listbox,' +
    'menuitem,row,rowheader,tab,treeitem,columnheader,menuitemcheckbox,menuitemradio,switch';
  if (expandedRoles.indexOf(role) >= 0) {
    var isExp = el.getAttribute('aria-expanded');
    if (el.tagName === 'DETAILS') isExp = el.open ? 'true' : 'false';
    if (isExp !== null && isExp !== undefined) s.push(isExp === 'true' || isExp === true ? 'expanded' : 'collapsed');
  }

  // pressed — role-gated: button only (Playwright ref: roleUtils.ts:1052-1063)
  var pressed = el.getAttribute('aria-pressed');
  if (pressed && role === 'button') {
    if (pressed === 'true') s.push('pressed');
    else if (pressed === 'mixed') s.push('pressed=mixed');
  }

  // selected — role-gated + native OPTION.selected (Playwright ref: roleUtils.ts:993-1001)
  var selectedRoles = 'gridcell,option,row,tab,rowheader,columnheader,treeitem';
  if (selectedRoles.indexOf(role) >= 0) {
    var isSel = el.getAttribute('aria-selected');
    if (el.tagName === 'OPTION' && el.selected) isSel = 'true';
    if (isSel === 'true') s.push('selected');
  }

  // readonly — role-gated (Playwright ref: roleUtils.ts:1039-1050)
  var readonlyRoles = 'checkbox,combobox,grid,gridcell,listbox,radiogroup,slider,' +
    'spinbutton,textbox,columnheader,rowheader,searchbox,switch,treegrid';
  if (readonlyRoles.indexOf(role) >= 0) {
    if (el.readOnly || el.getAttribute('aria-readonly') === 'true') s.push('readonly');
  }

  // invalid — aria-invalid + HTML5 validity (Playwright ref: roleUtils.ts:562-580)
  var inv = el.getAttribute('aria-invalid');
  if (inv === 'true') { s.push('invalid'); }
  else if (inv === 'grammar') s.push('invalid=grammar');
  else if (inv === 'spelling') s.push('invalid=spelling');
  else if (typeof el.validity === 'object' && el.validity && !el.validity.valid) s.push('invalid');

  // focused
  if (document.activeElement === el) s.push('focused');

  // level — role-gated
  if ('heading,listitem,row,treeitem'.indexOf(role) >= 0) {
    var lv = el.getAttribute('aria-level');
    if (lv) { s.push('level=' + lv); }
    else { var m = el.tagName && el.tagName.match(/^H(\d)$/); if (m) s.push('level=' + m[1]); }
  }

  return s;
}

/**
 * Check if element is disabled: native disabled + aria-disabled + fieldset inheritance.
 * Playwright ref: roleUtils.ts:1099-1121 getAriaDisabled
 */
function isDisabled(el) {
  if (el.disabled) return true;
  if (el.getAttribute('aria-disabled') === 'true') return true;
  // fieldset[disabled] inheritance (excluding legend internals)
  // Playwright ref: roleUtils.ts:1115 belongsToDisabledFieldSet
  var p = el.parentElement;
  while (p) {
    if (p.tagName === 'FIELDSET' && p.disabled) {
      var lg = p.querySelector('legend');
      if (lg && !lg.contains(el)) return true;
    }
    var ad = p.getAttribute('aria-disabled');
    if (ad === 'true') return true;
    p = p.parentElement;
  }
  return false;
}

// ═══════════════════════════════════════════════════════════════════
// 6. Clickable inference
//    No direct Playwright equivalent. Playwright uses receivesPointerEvents
//    (roleUtils.ts:1149) and interactability checks.
//    We do simple inference at capture time; precise ref assignment happens in Java.
// ═══════════════════════════════════════════════════════════════════

function isClickable(el) {
  var tag = el.tagName;
  if (tag === 'BUTTON' || tag === 'A' || tag === 'SELECT') return true;
  if (tag === 'INPUT') {
    var tp = (el.type || 'text').toLowerCase();
    if (tp !== 'hidden') return true;
  }
  if (el.getAttribute('role') === 'button') return true;
  if (el.onclick != null) return true;
  return false;
}

// ═══════════════════════════════════════════════════════════════════
// 7. Element index counter
//    Assigns incremental __pr_idx attribute to each serialized DOM element
//    for later operation targeting (click, type, etc.).
// ═══════════════════════════════════════════════════════════════════

var prIdxCounter = 0;

// ═══════════════════════════════════════════════════════════════════
// 8. Main DOM traversal (serialization)
//    Playwright ref: ariaSnapshot.ts:84 generateAriaTree → visit(ariaNode, node, parentElementVisible)
//
//    Key: invisible elements' visible children are reattached to the parent container (not discarded).
//      Playwright: processElement(childAriaNode || ariaNode, ...)
//      Here: serialize(container, node, depth) — container is the output target
// ═══════════════════════════════════════════════════════════════════

function serialize(container, node, depth) {
  if (depth > 50) return;

  // --- Text nodes ---
  // Playwright ref: ariaSnapshot.ts:108 — collected as string children
  // Text inside textbox is excluded (captured via element.value instead)
  if (node.nodeType === 3) {
    var text = node.textContent.trim();
    if (!text || text.length === 0) return;
    if (container._isTextbox) return;
    container.children.push({ role: 'text', name: text, children: [] });
    return;
  }

  // Non-element nodes
  if (node.nodeType !== 1) return;

  // Hidden tags: skip entire subtree (no reattach)
  if (isHiddenTag(node.tagName)) return;

  // SVG <title>: display:none, only contributes to SVG's own name
  if (node.tagName === 'TITLE' && node.ownerSVGElement) return;

  // Visibility check
  // Playwright ref: ariaSnapshot.ts:123-128
  //   visible → create child node, children output to childResult
  //   !visible → no child node, children output to container (reattach)
  var visible = !isHiddenForAria(node);

  var result = null;
  if (visible) {
    // Get role; null means excluded (e.g., input[hidden], role=presentation without conflict)
    var role = getRole(node);
    if (!role) return;

    // Inline generic with single text child → skip node, text flows to container naturally
    // Playwright ref: ariaSnapshot.ts:249
    if (role === 'generic') {
      var cs = getComputedStyle(node);
      if (cs.display === 'inline' && node.childNodes.length === 1 && node.childNodes[0].nodeType === 3) return;
    }

    var prIdx = prIdxCounter++;
    try { node.setAttribute('__pr_idx', prIdx); } catch (e) {}

    result = {
      role: role,
      name: getName(node),
      states: getStates(node),
      bounds: getBounds(node),
      __pr_idx: prIdx,
      children: []
    };

    if (isClickable(node)) result.states.push('clickable');
    result._isTextbox = (role === 'textbox');

    // Input/textarea value as state (Playwright ref: ariaSnapshot.ts:282-285)
    if (typeof node.value === 'string' && node.value) {
      result.states.push('value=' + node.value);
    }

    container.children.push(result);
  }

  // Children output to result (visible) or container (invisible → reattach)
  var target = result || container;

  // ::before pseudo-element as text child (Playwright ref: ariaSnapshot.ts:158)
  if (result) {
    var before = pseudoText(getComputedStyle(node, '::before').content);
    if (before) result.children.push({ role: 'text', name: before, children: [] });
  }

  // --- Shadow DOM ---
  // Playwright ref: ariaSnapshot.ts:168-171 — traverse shadowRoot after light DOM
  var shadow = node.shadowRoot;
  if (shadow) {
    var sc = shadow.firstChild;
    while (sc) {
      serialize(target, sc, depth + 1);
      sc = sc.nextSibling;
    }
  }

  // --- Light DOM children ---
  // Playwright ref: ariaSnapshot.ts:164 — childNodes traversal
  var children = node.childNodes;
  for (var i = 0; i < children.length; i++) {
    serialize(target, children[i], depth + 1);
  }

  // ::after pseudo-element as text child (Playwright ref: ariaSnapshot.ts:177)
  if (result) {
    var after = pseudoText(getComputedStyle(node, '::after').content);
    if (after) result.children.push({ role: 'text', name: after, children: [] });
  }

  // --- Same-origin iframe ---
  // Playwright doesn't recurse iframes in JS (handled by external orchestration).
  // We traverse same-origin iframes via contentDocument.
  if (node.tagName === 'IFRAME') {
    try {
      var doc = node.contentDocument;
      if (doc && doc.body) {
        serialize(target, doc.body, depth + 1);
      }
    } catch (e) {}
  }
}

// ═══════════════════════════════════════════════════════════════════
// 9. Entry point
//    Playwright ref: injectedScript.ts:313 incrementalAriaSnapshot
//      → generateAriaTree → renderAriaTree
//    We serialize from document.body directly; rendering happens in Java.
// ═══════════════════════════════════════════════════════════════════

var body = document.body;
if (!body) return JSON.stringify({ error: 'no body' });

var root = { role: 'screen', name: '', states: [], bounds: [], children: [] };
serialize(root, body, 0);

if (root.children.length === 0) return JSON.stringify({ error: 'empty result' });

return JSON.stringify({
  url: location.href,
  title: document.title || '',
  root: root
});

})();
