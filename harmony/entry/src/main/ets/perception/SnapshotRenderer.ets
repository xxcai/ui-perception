import { SemanticNode } from './SemanticNode';
import { snapshotName } from './SemanticRole';
import { Bounds } from './Bounds';

export class SnapshotRenderOptions {
  readonly boxes: boolean;
  readonly maxDepth: number;

  constructor(boxes: boolean, maxDepth: number) {
    this.boxes = boxes;
    this.maxDepth = maxDepth;
  }

  static readonly DEFAULTS: SnapshotRenderOptions = new SnapshotRenderOptions(false, Number.MAX_SAFE_INTEGER);

  get hasDepthLimit(): boolean {
    return this.maxDepth !== Number.MAX_SAFE_INTEGER;
  }
}

export class SnapshotRenderer {
  static render(root: SemanticNode | null,
                options: SnapshotRenderOptions = SnapshotRenderOptions.DEFAULTS): string {
    if (root === null) {
      return '';
    }
    let output: string = '';
    output = SnapshotRenderer.appendNode(root, 0, options, output);
    return output;
  }

  private static appendNode(node: SemanticNode,
                            depth: number,
                            options: SnapshotRenderOptions,
                            output: string): string {
    let next = output;
    if (options.hasDepthLimit && depth >= options.maxDepth - 1) {
      next = SnapshotRenderer.appendLine(SnapshotRenderer.keyFor(node, options), depth, next);
      return next;
    }
    const key = SnapshotRenderer.keyFor(node, options);
    if (node.children.length === 0) {
      next = SnapshotRenderer.appendLine(key, depth, next);
      return next;
    }
    next = SnapshotRenderer.appendLine(key + ':', depth, next);
    for (let i = 0; i < node.children.length; i++) {
      next = SnapshotRenderer.appendNode(node.children[i], depth + 1, options, next);
    }
    return next;
  }

  private static keyFor(node: SemanticNode, options: SnapshotRenderOptions): string {
    let key = snapshotName(node.role);
    if (node.name.length > 0) {
      key += ' "' + SnapshotRenderer.escape(node.name) + '"';
    }
    for (let i = 0; i < node.states.length; i++) {
      key += ' [' + node.states[i] + ']';
    }
    if (node.hasRef) {
      key += ' [ref=' + node.ref + ']';
    }
    if (SnapshotRenderer.shouldRenderBounds(node, options)) {
      key += ' [bounds=' + (node.bounds !== null ? node.bounds.snapshotValue : '') + ']';
    }
    return key;
  }

  private static shouldRenderBounds(node: SemanticNode, options: SnapshotRenderOptions): boolean {
    if (node.bounds === null) {
      return false;
    }
    return node.hasRef || options.boxes;
  }

  private static appendLine(text: string, depth: number, output: string): string {
    let next = output;
    if (next.length > 0) {
      next += '\n';
    }
    next += '  '.repeat(depth) + '- ' + text;
    return next;
  }

  private static escape(value: string): string {
    return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  }
}
