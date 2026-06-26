import { SemanticRole } from './SemanticRole';
import { RoleDecision } from './RoleDecision';

export interface HarmonyInspectorNode {
  readonly type: string;
  readonly id: string;
  readonly rect: string;
  readonly attrs: Map<string, string>;
  readonly children: Array<HarmonyInspectorNode>;
}

export class HarmonyRoleResolver {
  static readonly ROLE_VERSION: string = 'harmony-arkui-role-v1';

  private static readonly INHERENTLY_CLICKABLE: Set<string> = new Set<string>([
    'ListItem', 'GridItem', 'MenuItem',
    'Button', 'Toggle', 'Checkbox', 'CheckboxGroup',
    'Radio', 'RadioButton', 'Stepper', 'Select',
    'Slider', 'Rating', 'Gauge',
    'Swiper', 'Tabs', 'TabContent',
    'DatePicker', 'TimePicker', 'TextPicker', 'CalendarPicker',
  ]);

  private static readonly INHERENTLY_CHECKABLE: Set<string> = new Set<string>([
    'Toggle', 'Checkbox', 'Radio', 'RadioButton',
  ]);

  private static readonly INHERENTLY_SELECTABLE: Set<string> = new Set<string>([
    'ListItem', 'GridItem', 'MenuItem',
  ]);

  static resolve(node: HarmonyInspectorNode): RoleDecision {
    const type = node.type;
    if (type === 'Button') {
      return new RoleDecision(SemanticRole.Button, 'type:button', 0.95);
    }
    if (type === 'Toggle') {
      return new RoleDecision(SemanticRole.Switch, 'type:toggle', 0.95);
    }
    if (type === 'Checkbox' || type === 'CheckboxGroup') {
      return new RoleDecision(SemanticRole.Checkbox, 'type:checkbox', 0.95);
    }
    if (type === 'Radio' || type === 'RadioButton') {
      return new RoleDecision(SemanticRole.Radio, 'type:radio', 0.95);
    }
    if (type === 'Slider' || type === 'Rating' || type === 'Gauge') {
      return new RoleDecision(SemanticRole.Slider, 'type:slider', 0.9);
    }
    if (type === 'Select' || type === 'Stepper') {
      return new RoleDecision(SemanticRole.Picker, 'type:picker', 0.9);
    }
    if (type === 'DatePicker' || type === 'TimePicker'
        || type === 'TextPicker' || type === 'CalendarPicker') {
      return new RoleDecision(SemanticRole.Picker, 'type:date-picker', 0.9);
    }
    if (type === 'TextInput' || type === 'TextArea' || type === 'Search') {
      return new RoleDecision(SemanticRole.Input, 'type:input', 0.95);
    }
    if (type === 'Text' || type === 'Span') {
      return new RoleDecision(SemanticRole.Text, 'type:text', 0.85);
    }
    if (type === 'Image') {
      return new RoleDecision(SemanticRole.Image, 'type:image', 0.85);
    }
    if (type === 'List') {
      return new RoleDecision(SemanticRole.List, 'type:list', 0.9);
    }
    if (type === 'Grid' || type === 'GridRow' || type === 'WaterFlow') {
      return new RoleDecision(SemanticRole.Grid, 'type:grid', 0.9);
    }
    if (type === 'ListItem' || type === 'GridItem' || type === 'FlowItem') {
      return new RoleDecision(SemanticRole.ListItem, 'type:listitem', 0.9);
    }
    if (type === 'Scroll') {
      return new RoleDecision(SemanticRole.Scroll, 'type:scroll', 0.9);
    }
    if (type === 'Tabs' || type === 'TabContent') {
      return new RoleDecision(SemanticRole.Tabbar, 'type:tabbar', 0.85);
    }
    if (type === 'TabBuilder') {
      return new RoleDecision(SemanticRole.Tab, 'type:tab', 0.85);
    }
    if (type === 'Navigation' || type === 'NavBar' || type === 'NavDestination') {
      return new RoleDecision(SemanticRole.Navigation, 'type:navigation', 0.85);
    }
    if (type === 'Progress' || type === 'LoadingProgress') {
      return new RoleDecision(SemanticRole.Progress, 'type:progress', 0.9);
    }
    if (type === 'Web') {
      return new RoleDecision(SemanticRole.Webview, 'type:webview', 0.95);
    }
    if (type === 'Divider') {
      return new RoleDecision(SemanticRole.Separator, 'type:separator', 0.85);
    }
    if (type === 'Panel' || type === 'Sheet' || type === 'BindSheet') {
      return new RoleDecision(SemanticRole.Sheet, 'type:sheet', 0.85);
    }
    if (type === 'Dialog' || type === 'AlertDialog' || type === 'CustomDialog') {
      return new RoleDecision(SemanticRole.Dialog, 'type:dialog', 0.85);
    }

    // a11y explicit mark
    const a11yRole = node.attrs.get('accessibilityRole');
    if (a11yRole !== undefined && a11yRole.length > 0) {
      const mapped = HarmonyRoleResolver.mapA11yRole(a11yRole);
      if (mapped !== null) {
        return new RoleDecision(mapped, 'attr:accessibilityRole', 0.85);
      }
    }

    // fallback: text-or-label
    const content = node.attrs.get('content');
    const a11yText = node.attrs.get('accessibilityText');
    if ((content !== undefined && content.length > 0)
        || (a11yText !== undefined && a11yText.length > 0)) {
      return new RoleDecision(SemanticRole.Text, 'text-or-label-fallback', 0.55);
    }

    // generic container
    return new RoleDecision(SemanticRole.Generic, 'generic-fallback', 0.3);
  }

  static resolveName(node: HarmonyInspectorNode, role: SemanticRole): string {
    if (role === SemanticRole.Input) {
      const placeholder = node.attrs.get('placeholder');
      if (placeholder !== undefined && placeholder.length > 0) {
        return placeholder;
      }
    }
    const content = node.attrs.get('content');
    if (content !== undefined && content.length > 0) {
      return content;
    }
    const a11yText = node.attrs.get('accessibilityText');
    if (a11yText !== undefined && a11yText.length > 0) {
      return a11yText;
    }
    const a11yDesc = node.attrs.get('accessibilityDescription');
    if (a11yDesc !== undefined && a11yDesc.length > 0) {
      return a11yDesc;
    }
    return '';
  }

  static inferClickable(node: HarmonyInspectorNode, role: SemanticRole): boolean {
    // L1: type-based
    if (HarmonyRoleResolver.INHERENTLY_CLICKABLE.has(node.type)) {
      return true;
    }
    // L3: explicit a11y action
    const actions = node.attrs.get('accessibilityActionOptions');
    if (actions !== undefined && actions.indexOf('ACCESSIBILITY_CLICK') >= 0) {
      return true;
    }
    const a11yRole = node.attrs.get('accessibilityRole');
    if (a11yRole !== undefined
        && (a11yRole === 'BUTTON' || a11yRole === 'CHECKBOX' || a11yRole === 'RADIO')) {
      return true;
    }
    // L2: focusable heuristic (medium confidence)
    const focusable = node.attrs.get('focusable');
    if (focusable === 'true'
        && role !== SemanticRole.Text
        && role !== SemanticRole.Image
        && role !== SemanticRole.Generic) {
      return true;
    }
    return false;
  }

  static isCheckable(type: string): boolean {
    return HarmonyRoleResolver.INHERENTLY_CHECKABLE.has(type);
  }

  static isSelectable(type: string): boolean {
    return HarmonyRoleResolver.INHERENTLY_SELECTABLE.has(type);
  }

  static isInherentlyClickable(type: string): boolean {
    return HarmonyRoleResolver.INHERENTLY_CLICKABLE.has(type);
  }

  private static mapA11yRole(value: string): SemanticRole | null {
    switch (value) {
      case 'BUTTON': return SemanticRole.Button;
      case 'CHECKBOX': return SemanticRole.Checkbox;
      case 'RADIO': return SemanticRole.Radio;
      case 'SWITCH': return SemanticRole.Switch;
      case 'SLIDER': return SemanticRole.Slider;
      case 'IMAGE': return SemanticRole.Image;
      case 'TEXT':
      case 'STATIC_TEXT': return SemanticRole.Text;
      case 'EDIT_TEXT':
      case 'SEARCH': return SemanticRole.Input;
      case 'LIST': return SemanticRole.List;
      case 'GRID': return SemanticRole.Grid;
      case 'PAGE': return SemanticRole.Screen;
      case 'TAB_BAR': return SemanticRole.Tabbar;
      case 'NAVIGATION_BAR': return SemanticRole.Toolbar;
      case 'PROGRESS_BAR': return SemanticRole.Progress;
      default: return null;
    }
  }
}
