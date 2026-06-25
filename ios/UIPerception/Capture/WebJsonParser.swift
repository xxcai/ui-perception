import Foundation

/// 把 dom-serializer.js 输出的 JSON 解析为 SemanticNode 树。
/// 对应 Android `WebJsonParser.java`。
///
/// 数据流：WKWebView → evaluateJavaScript(dom-serializer.js) → JSON 字符串
///       → 本 parser → SemanticNode 树 → TreeNormalizer → RefAssigner("w") → YAML
enum WebJsonParser {

    /// 解析顶层 JSON，提取 "root" 节点。
    /// 输入是 dom-serializer.js 直接 return 的 JSON 字符串。
    static func parse(_ json: String?) -> SemanticNode? {
        guard let json, !json.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        guard let data = json.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let rootNode = object["root"] as? [String: Any]
        else { return nil }
        return parseNode(rootNode)
    }

    /// 递归解析 JSON node 为 SemanticNode。
    /// JSON 字段映射：role → SemanticRole、name、bounds、states、__pr_idx → webElementIdx、children → 递归。
    private static func parseNode(_ obj: [String: Any]) -> SemanticNode? {
        guard let roleStr = obj["role"] as? String else { return nil }
        let role = mapRole(roleStr)

        let name = (obj["name"] as? String) ?? ""
        let bounds = parseBounds(obj["bounds"])

        let builder = SemanticNode.builder(role)
            .name(name)
            .bounds(bounds)
            .webElementIdx((obj["__pr_idx"] as? Int) ?? -1)

        if let states = obj["states"] as? [String] {
            for state in states { builder.addState(state) }
        }

        if let children = obj["children"] as? [[String: Any]] {
            for child in children {
                if let childNode = parseNode(child) {
                    builder.addChild(childNode)
                }
            }
        }

        return builder.build()
    }

    /// JSON 字符串 role 映射到 SemanticRole。
    /// 匹配 SemanticRole.snapshotName；未知 role 兜底为 .generic。
    private static func mapRole(_ role: String) -> SemanticRole {
        if role.isEmpty { return .generic }
        return SemanticRole(rawValue: role) ?? .generic
    }

    /// 把 [x1, y1, x2, y2] JSON 数组解析为 Bounds。
    private static func parseBounds(_ any: Any?) -> Bounds? {
        guard let arr = any as? [Int], arr.count == 4 else { return nil }
        return Bounds(left: arr[0], top: arr[1], right: arr[2], bottom: arr[3])
    }
}
