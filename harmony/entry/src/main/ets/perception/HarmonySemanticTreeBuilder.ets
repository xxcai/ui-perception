import { HarmonyInspectorNode, HarmonyRoleResolver } from './HarmonyRoleResolver';
import { SemanticNode } from './SemanticNode';
import { Bounds } from './Bounds';
import { SemanticStates } from './SemanticStates';

export class HarmonySemanticTreeBuilder {
  static build(root: HarmonyInspectorNode | null): SemanticNode | null {
    if (root === null) {
      return null;
    }
    return HarmonySemanticTreeBuilder.buildNode(root);
  }

  private static buildNode(node: HarmonyInspectorNode): SemanticNode {
    const decision = HarmonyRoleResolver.resolve(node);
    const role = decision.role;
    const name = HarmonyRoleResolver.resolveName(node, role);
    const bounds = Bounds.parse(node.rect);

    const builder = SemanticNode.builder(role)
      .setName(name)
      .setClassName(node.type)
      .setResourceId(node.id)
      .setBounds(bounds)
      .setRoleDecision(decision);

    const enabled = node.attrs.get('enabled');
    if (enabled === 'false') {
      builder.addState(SemanticStates.DISABLED);
    }

    if (HarmonyRoleResolver.inferClickable(node, role)) {
      if (HarmonyRoleResolver.isInherentlyClickable(node.type)) {
        builder.addState(SemanticStates.CLICKABLE);
      } else {
        builder.addState(SemanticStates.CLICKABLE_INFERRED);
      }
    }

    if (HarmonyRoleResolver.isCheckable(node.type)) {
      const checked = node.attrs.get('selected');
      if (checked === 'true') {
        builder.addState(SemanticStates.CHECKED);
      }
    }

    if (HarmonyRoleResolver.isSelectable(node.type)) {
      const selected = node.attrs.get('selected');
      if (selected === 'true') {
        builder.addState(SemanticStates.SELECTED);
      }
    }

    const scrollable = node.attrs.get('scrollable');
    if (scrollable === 'true') {
      builder.addState(SemanticStates.SCROLLABLE);
    }

    for (let i = 0; i < node.children.length; i++) {
      const child = HarmonySemanticTreeBuilder.buildNode(node.children[i]);
      builder.addChild(child);
    }

    return builder.build();
  }
}
