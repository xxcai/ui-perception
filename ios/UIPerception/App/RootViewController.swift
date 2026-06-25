import UIKit

final class RootViewController: UITableViewController {
    private struct BaselinePage {
        let title: String
        let subtitle: String
        let makeViewController: () -> UIViewController
    }

    private let pages: [BaselinePage] = [
        BaselinePage(
            title: "消息首页",
            subtitle: "Message Home — UITableView + 搜索栏",
            makeViewController: { MessageHomeViewController() }
        ),
        BaselinePage(
            title: "Web 表单页",
            subtitle: "Web Form — WKWebView + 表单/列表/链接",
            makeViewController: { WebBaselineViewController(pageName: "web_form") }
        ),
    ]

    init() {
        super.init(style: .insetGrouped)
        title = "UI Perception"
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        pages.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        let page = pages[indexPath.row]
        var content = cell.defaultContentConfiguration()
        content.text = page.title
        content.secondaryText = page.subtitle
        cell.contentConfiguration = content
        cell.accessoryType = .disclosureIndicator
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let page = pages[indexPath.row]
        let vc = page.makeViewController()
        vc.title = page.title
        navigationController?.pushViewController(vc, animated: true)
    }
}
