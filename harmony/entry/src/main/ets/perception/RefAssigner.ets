import { SemanticNode } from './SemanticNode';
import { SemanticRole } from './SemanticRole';
import { SemanticStates } from './SemanticStates';

export class RefAssigner {
  static assign(root: SemanticNode | null,
                prefix: string = 'n',
                webMode: boolean = false): SemanticNode | null {
    if (root === null) {
      return null;
    }
    const state = new RefAssignerState(prefix, webMode);
    return state.assignNode(root);
  }

  static shouldAssignRef(node: SemanticNode, webMode: boolean): boolean {
    if (node.bounds === null || !node.bounds.isValid) {
      return false;
    }
    if (webMode) {
      return true;
    }
    if (node.role === SemanticRole.ListItem) {
      return RefAssigner.shouldAssignRefToListItem(node);
    }
    return RefAssigner.isExecutableRole(node.role);
  }

  private static isExecutableRole(role: SemanticRole): boolean {
    switch (role) {
      case SemanticRole.Button:
      case SemanticRole.Input:
      case SemanticRole.Checkbox:
      case SemanticRole.Radio:
      case SemanticRole.Switch:
      case SemanticRole.Slider:
      case SemanticRole.Picker:
      case SemanticRole.List:
      case SemanticRole.Grid:
      case SemanticRole.Scroll:
      case SemanticRole.Webview:
        return true;
      default:
        return false;
    }
  }

  private static shouldAssignRefToListItem(node: SemanticNode): boolean {
    if (node.states.indexOf(SemanticStates.CLICKABLE) >= 0
        || node.states.indexOf(SemanticStates.CLICKABLE_INFERRED) >= 0) {
      return true;
    }
    if (node.states.indexOf(SemanticStates.CLICKABLE_GUESSED) >= 0) {
      return !RefAssigner.hasExecutableDescendant(node);
    }
    return false;
  }

  private static hasExecutableDescendant(node: SemanticNode): boolean {
    for (let i = 0; i < node.children.length; i++) {
      const child = node.children[i];
      if (RefAssigner.shouldAssignRef(child, false)
          || RefAssigner.hasExecutableDescendant(child)) {
        return true;
      }
    }
    return false;
  }
}

class RefAssignerState {
  readonly prefix: string;
  readonly webMode: boolean;
  private _nextRef: number = 1;

  constructor(prefix: string, webMode: boolean) {
    this.prefix = prefix;
    this.webMode = webMode;
  }

  assignNode(node: SemanticNode): SemanticNode {
    const builder = SemanticNode.builder(node.role)
      .setName(node.name)
      .setText(node.text)
      .setContentDescription(node.contentDescription)
      .setResourceId(node.resourceId)
      .setClassName(node.className)
      .setBounds(node.bounds)
      .setRoleDecision(node.roleDecision)
      .setWebElementIdx(node.webElementIdx);
    for (let i = 0; i < node.states.length; i++) {
      builder.addState(node.states[i]);
    }
    if (RefAssigner.shouldAssignRef(node, this.webMode)) {
      builder.setRef(this.prefix + this._nextRef.toString());
      this._nextRef += 1;
    } else if (node.hasRef) {
      builder.setRef(node.ref);
    }
    for (let i = 0; i < node.children.length; i++) {
      builder.addChild(this.assignNode(node.children[i]));
    }
    return builder.build();
  }
}
