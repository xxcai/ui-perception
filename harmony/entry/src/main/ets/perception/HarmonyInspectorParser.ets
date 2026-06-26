import { HarmonyInspectorNode } from './HarmonyRoleResolver';

type AttrValue = string | boolean | number;
type AttrsMap = Record<string, AttrValue>;

interface JsonNodeShape {
  $type?: string;
  $ID?: number;
  $rect?: string;
  type?: string;
  $attrs?: AttrsMap;
  $children?: Array<JsonNodeShape>;
}

export class HarmonyInspectorParser {
  static parse(json: string): HarmonyInspectorNode | null {
    let parsed: Object;
    try {
      parsed = JSON.parse(json);
    } catch (e) {
      return null;
    }
    const root = parsed as JsonNodeShape;
    if (root === null || root === undefined) {
      return null;
    }
    return HarmonyInspectorParser.convert(root);
  }

  private static convert(node: JsonNodeShape): HarmonyInspectorNode {
    const type = node.$type !== undefined ? node.$type : '';
    const id = node.$ID !== undefined ? node.$ID.toString() : '';

    const rect: string = node.$rect !== undefined ? node.$rect : '';

    const attrs = new Map<string, string>();
    const rawAttrs = node.$attrs;
    if (rawAttrs !== undefined && rawAttrs !== null) {
      const keys = Object.keys(rawAttrs);
      for (let i = 0; i < keys.length; i++) {
        const k = keys[i];
        const v = rawAttrs[k];
        attrs.set(k, v === undefined || v === null ? '' : v.toString());
      }
    }

    const children: Array<HarmonyInspectorNode> = [];
    const rawChildren = node.$children;
    if (rawChildren !== undefined && rawChildren !== null) {
      for (let i = 0; i < rawChildren.length; i++) {
        children.push(HarmonyInspectorParser.convert(rawChildren[i]));
      }
    }

    return {
      type: type,
      id: id,
      rect: rect,
      attrs: attrs,
      children: children,
    };
  }
}
