import Foundation

/// 语义角色枚举，对应 Android `SemanticRole.java`。
/// snapshotName 是 YAML 输出和 JSON wire 格式中使用的字符串。
enum SemanticRole: String {
    // native + web 共享
    case screen
    case toolbar
    case tabbar
    case tab
    case list
    case grid
    case listItem = "listitem"
    case scroll
    case card
    case section
    case dialog
    case sheet
    case text
    case image
    case button
    case input
    case checkbox
    case radio
    case switchRole = "switch"
    case slider
    case picker
    case progress
    case generic
    case webview
    case link
    case heading
    case navigation

    // web 专用（HTML ARIA），native 不产出
    case textbox
    case searchbox
    case spinbutton
    case combobox
    case listbox
    case tableRole = "table"
    case row
    case cell
    case columnheader
    case rowheader
    case form
    case article
    case complementary
    case blockquote
    case caption
    case group
    case term
    case definition
    case separator
    case meter
    case optionRole = "option"
    case status
    case paragraph
    case rowgroup

    var snapshotName: String { rawValue }
}
