import Foundation

/// Role 推导结果，对应 Android `RoleDecision.java`。
struct RoleDecision: Equatable {
    let role: SemanticRole
    let source: String
    let confidence: Double

    init(_ role: SemanticRole, _ source: String, _ confidence: Double) {
        self.role = role
        self.source = source
        self.confidence = confidence
    }
}
