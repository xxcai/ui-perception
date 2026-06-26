import { SemanticNode } from './SemanticNode';
import { SemanticRole } from './SemanticRole';
import { Bounds } from './Bounds';

interface WebJsonRoot {
  url?: string;
  title?: string;
  root?: WebJsonNode;
  error?: string;
}

interface WebJsonNode {
  role?: string;
  name?: string;
  states?: Array<string>;
  bounds?: Array<number>;
  __pr_idx?: number;
  children?: Array<WebJsonNode>;
}

export class WebJsonParser {
  static parse(json: string): SemanticNode | null {
    if (json === null || json === undefined) {
      return null;
    }
    const trimmed = json.trim();
    if (trimmed.length === 0) {
      return null;
    }
    let parsed: Object;
    try {
      parsed = JSON.parse(trimmed);
    } catch (e) {
      return null;
    }
    const root = parsed as WebJsonRoot;
    if (root === null || root === undefined) {
      return null;
    }
    if (root.error !== undefined && root.error.length > 0) {
      return null;
    }
    const rootNode = root.root;
    if (rootNode === null || rootNode === undefined) {
      return null;
    }
    return WebJsonParser.parseNode(rootNode);
  }

  private static parseNode(obj: WebJsonNode): SemanticNode {
    const role = WebJsonParser.mapRole(obj.role !== undefined ? obj.role : '');
    const name = obj.name !== undefined ? obj.name : '';
    const bounds = WebJsonParser.parseBounds(obj.bounds);

    const builder = SemanticNode.builder(role)
      .setName(name)
      .setBounds(bounds)
      .setWebElementIdx(obj.__pr_idx !== undefined ? obj.__pr_idx : -1);

    if (obj.states !== undefined && obj.states !== null) {
      for (let i = 0; i < obj.states.length; i++) {
        builder.addState(obj.states[i]);
      }
    }

    if (obj.children !== undefined && obj.children !== null) {
      for (let i = 0; i < obj.children.length; i++) {
        const child = WebJsonParser.parseNode(obj.children[i]);
        builder.addChild(child);
      }
    }

    return builder.build();
  }

  private static mapRole(role: string): SemanticRole {
    if (role.length === 0) {
      return SemanticRole.Generic;
    }
    const mapped = WEB_ROLE_MAP.get(role);
    if (mapped !== undefined) {
      return mapped;
    }
    return SemanticRole.Generic;
  }

  private static parseBounds(arr: Array<number> | undefined): Bounds | null {
    if (arr === undefined || arr === null) {
      return null;
    }
    if (arr.length !== 4) {
      return null;
    }
    return new Bounds(arr[0], arr[1], arr[2], arr[3]);
  }
}

const WEB_ROLE_MAP: Map<string, SemanticRole> = new Map<string, SemanticRole>([
  ['screen', SemanticRole.Screen],
  ['toolbar', SemanticRole.Toolbar],
  ['tabbar', SemanticRole.Tabbar],
  ['tab', SemanticRole.Tab],
  ['list', SemanticRole.List],
  ['grid', SemanticRole.Grid],
  ['listitem', SemanticRole.ListItem],
  ['scroll', SemanticRole.Scroll],
  ['card', SemanticRole.Card],
  ['section', SemanticRole.Section],
  ['dialog', SemanticRole.Dialog],
  ['sheet', SemanticRole.Sheet],
  ['text', SemanticRole.Text],
  ['image', SemanticRole.Image],
  ['button', SemanticRole.Button],
  ['input', SemanticRole.Input],
  ['checkbox', SemanticRole.Checkbox],
  ['radio', SemanticRole.Radio],
  ['switch', SemanticRole.Switch],
  ['slider', SemanticRole.Slider],
  ['picker', SemanticRole.Picker],
  ['progress', SemanticRole.Progress],
  ['generic', SemanticRole.Generic],
  ['webview', SemanticRole.Webview],
  ['link', SemanticRole.Link],
  ['heading', SemanticRole.Heading],
  ['navigation', SemanticRole.Navigation],
  ['textbox', SemanticRole.Textbox],
  ['searchbox', SemanticRole.Searchbox],
  ['spinbutton', SemanticRole.Spinbutton],
  ['combobox', SemanticRole.Combobox],
  ['listbox', SemanticRole.Listbox],
  ['table', SemanticRole.Table],
  ['row', SemanticRole.Row],
  ['cell', SemanticRole.Cell],
  ['columnheader', SemanticRole.ColumnHeader],
  ['rowheader', SemanticRole.RowHeader],
  ['form', SemanticRole.Form],
  ['article', SemanticRole.Article],
  ['complementary', SemanticRole.Complementary],
  ['blockquote', SemanticRole.Blockquote],
  ['caption', SemanticRole.Caption],
  ['group', SemanticRole.Group],
  ['term', SemanticRole.Term],
  ['definition', SemanticRole.Definition],
  ['separator', SemanticRole.Separator],
  ['meter', SemanticRole.Meter],
  ['option', SemanticRole.Option],
  ['status', SemanticRole.Status],
  ['paragraph', SemanticRole.Paragraph],
  ['rowgroup', SemanticRole.RowGroup],
]);
