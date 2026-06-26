import { SemanticRole } from './SemanticRole';
import { Bounds } from './Bounds';
import { RoleDecision } from './RoleDecision';

export class SemanticNode {
  readonly role: SemanticRole;
  readonly name: string;
  readonly text: string;
  readonly contentDescription: string;
  readonly resourceId: string;
  readonly className: string;
  readonly bounds: Bounds | null;
  readonly ref: string;
  readonly roleDecision: RoleDecision | null;
  readonly states: Array<string>;
  readonly children: Array<SemanticNode>;
  readonly webElementIdx: number;

  static readonly NO_WEB_ELEMENT: number = -1;

  constructor(role: SemanticRole,
              name: string,
              text: string,
              contentDescription: string,
              resourceId: string,
              className: string,
              bounds: Bounds | null,
              ref: string,
              roleDecision: RoleDecision | null,
              states: Array<string>,
              children: Array<SemanticNode>,
              webElementIdx: number) {
    this.role = role;
    this.name = SemanticNode.normalize(name);
    this.text = SemanticNode.normalize(text);
    this.contentDescription = SemanticNode.normalize(contentDescription);
    this.resourceId = SemanticNode.normalize(resourceId);
    this.className = SemanticNode.normalize(className);
    this.bounds = bounds;
    this.ref = SemanticNode.normalize(ref);
    this.roleDecision = roleDecision;
    this.states = states;
    this.children = children;
    this.webElementIdx = webElementIdx;
  }

  get hasRef(): boolean {
    return this.ref.length > 0;
  }

  static builder(role: SemanticRole): SemanticNodeBuilder {
    return new SemanticNodeBuilder(role);
  }

  static normalize(value: string): string {
    if (value === null || value === undefined) {
      return '';
    }
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      return '';
    }
    return trimmed.replace(/\s+/g, ' ');
  }
}

export class SemanticNodeBuilder {
  readonly role: SemanticRole;
  private _name: string = '';
  private _text: string = '';
  private _contentDescription: string = '';
  private _resourceId: string = '';
  private _className: string = '';
  private _bounds: Bounds | null = null;
  private _ref: string = '';
  private _roleDecision: RoleDecision | null = null;
  private _states: Array<string> = [];
  private _children: Array<SemanticNode> = [];
  private _webElementIdx: number = SemanticNode.NO_WEB_ELEMENT;

  constructor(role: SemanticRole) {
    this.role = role;
  }

  get name(): string { return this._name; }
  get text(): string { return this._text; }
  get contentDescription(): string { return this._contentDescription; }
  get resourceId(): string { return this._resourceId; }
  get className(): string { return this._className; }
  get bounds(): Bounds | null { return this._bounds; }
  get ref(): string { return this._ref; }
  get roleDecision(): RoleDecision | null { return this._roleDecision; }
  get states(): Array<string> { return this._states; }
  get children(): Array<SemanticNode> { return this._children; }
  get webElementIdx(): number { return this._webElementIdx; }

  setName(value: string): SemanticNodeBuilder {
    this._name = value;
    return this;
  }

  setText(value: string): SemanticNodeBuilder {
    this._text = value;
    return this;
  }

  setContentDescription(value: string): SemanticNodeBuilder {
    this._contentDescription = value;
    return this;
  }

  setResourceId(value: string): SemanticNodeBuilder {
    this._resourceId = value;
    return this;
  }

  setClassName(value: string): SemanticNodeBuilder {
    this._className = value;
    return this;
  }

  setBounds(value: Bounds | null): SemanticNodeBuilder {
    this._bounds = value;
    return this;
  }

  setRef(value: string): SemanticNodeBuilder {
    this._ref = value;
    return this;
  }

  setRoleDecision(value: RoleDecision | null): SemanticNodeBuilder {
    this._roleDecision = value;
    return this;
  }

  addState(value: string): SemanticNodeBuilder {
    const normalized = SemanticNode.normalize(value);
    if (normalized.length > 0) {
      this._states.push(normalized);
    }
    return this;
  }

  addChild(child: SemanticNode | null): SemanticNodeBuilder {
    if (child !== null) {
      this._children.push(child);
    }
    return this;
  }

  setWebElementIdx(value: number): SemanticNodeBuilder {
    this._webElementIdx = value;
    return this;
  }

  build(): SemanticNode {
    return new SemanticNode(
      this.role,
      this._name,
      this._text,
      this._contentDescription,
      this._resourceId,
      this._className,
      this._bounds,
      this._ref,
      this._roleDecision,
      this._states.slice(),
      this._children.slice(),
      this._webElementIdx,
    );
  }
}
