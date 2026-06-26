import { SemanticRole } from './SemanticRole';

export class RoleDecision {
  readonly role: SemanticRole;
  readonly source: string;
  readonly confidence: number;

  constructor(role: SemanticRole, source: string, confidence: number) {
    this.role = role;
    this.source = source;
    this.confidence = confidence;
  }
}
