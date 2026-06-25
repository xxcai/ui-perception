import Foundation

/// iOS 屏幕坐标系下的节点矩形（int），对应 Android `Bounds.java`。
/// `[l,t][r,b]` 格式与 Android 完全一致，方便跨端 diff。
struct Bounds: Equatable {
    let left: Int
    let top: Int
    let right: Int
    let bottom: Int

    init(left: Int, top: Int, right: Int, bottom: Int) {
        self.left = left
        self.top = top
        self.right = right
        self.bottom = bottom
    }

    init(_ rect: CGRect) {
        self.left = Int(rect.minX.rounded())
        self.top = Int(rect.minY.rounded())
        self.right = Int(rect.maxX.rounded())
        self.bottom = Int(rect.maxY.rounded())
    }

    var width: Int { right - left }
    var height: Int { bottom - top }
    var centerX: Int { (left + right) / 2 }
    var centerY: Int { (top + bottom) / 2 }

    var isValid: Bool { right > left && bottom > top }

    var snapshotValue: String { "\(left),\(top),\(right),\(bottom)" }

    static func parse(_ value: String?) -> Bounds? {
        guard let value, !value.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespaces)
        let pattern = #"^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$"#
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: trimmed, range: NSRange(trimmed.startIndex..., in: trimmed)),
              match.numberOfRanges == 5,
              let r1 = Range(match.range(at: 1), in: trimmed),
              let r2 = Range(match.range(at: 2), in: trimmed),
              let r3 = Range(match.range(at: 3), in: trimmed),
              let r4 = Range(match.range(at: 4), in: trimmed),
              let l = Int(trimmed[r1]),
              let t = Int(trimmed[r2]),
              let r = Int(trimmed[r3]),
              let b = Int(trimmed[r4])
        else { return nil }
        return Bounds(left: l, top: t, right: r, bottom: b)
    }
}
