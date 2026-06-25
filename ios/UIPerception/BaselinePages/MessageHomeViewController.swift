import UIKit

/// UIKit 端的「消息首页」baseline 页面，复刻 Android NativeHomeFragment 的消息 tab。
/// 用于验证 UIView 树遍历能否拿到等价于 Android 的 semantic snapshot。
final class MessageHomeViewController: UIViewController {
    private let searchField = UITextField()
    private let addButton = UIButton(type: .system)
    private let callButton = UIButton(type: .system)
    private let tableView = UITableView(frame: .zero, style: .plain)

    struct MessageRow {
        let avatar: String
        let name: String
        let status: String
    }

    private let messages: [MessageRow] = [
        MessageRow(avatar: "辉", name: "梁晓舟", status: "明天上班"),
        MessageRow(avatar: "芸", name: "苏芸", status: "刚到工位"),
        MessageRow(avatar: "霖", name: "周霖", status: "在开会"),
        MessageRow(avatar: "瀚", name: "孙瀚", status: "外出"),
        MessageRow(avatar: "妍", name: "李妍", status: "请假"),
        MessageRow(avatar: "辰", name: "韩辰", status: "在线"),
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        setupSearchBar()
        setupTableView()
        setupConstraints()
    }

    private func setupSearchBar() {
        searchField.placeholder = "搜索"
        searchField.borderStyle = .roundedRect
        searchField.font = .systemFont(ofSize: 16)
        searchField.accessibilityIdentifier = "search_input"
        searchField.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(searchField)

        addButton.setTitle("新增", for: .normal)
        addButton.setImage(UIImage(systemName: "plus.circle"), for: .normal)
        addButton.accessibilityIdentifier = "add_button"
        addButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(addButton)

        callButton.setTitle("客服", for: .normal)
        callButton.setImage(UIImage(systemName: "phone"), for: .normal)
        callButton.accessibilityIdentifier = "call_button"
        callButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(callButton)
    }

    private func setupTableView() {
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(MessageCell.self, forCellReuseIdentifier: MessageCell.reuseId)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
    }

    private func setupConstraints() {
        NSLayoutConstraint.activate([
            searchField.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
            searchField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            searchField.trailingAnchor.constraint(equalTo: addButton.leadingAnchor, constant: -12),
            searchField.heightAnchor.constraint(equalToConstant: 40),

            addButton.centerYAnchor.constraint(equalTo: searchField.centerYAnchor),
            addButton.trailingAnchor.constraint(equalTo: callButton.leadingAnchor, constant: -8),

            callButton.centerYAnchor.constraint(equalTo: searchField.centerYAnchor),
            callButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),

            tableView.topAnchor.constraint(equalTo: searchField.bottomAnchor, constant: 12),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }
}

extension MessageHomeViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        messages.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: MessageCell.reuseId, for: indexPath) as! MessageCell
        cell.configure(with: messages[indexPath.row])
        return cell
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        72
    }
}

private final class MessageCell: UITableViewCell {
    static let reuseId = "MessageCell"

    private let avatarView = UILabel()
    private let nameLabel = UILabel()
    private let statusLabel = UILabel()
    private let avatarContainer = UIView()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private func setupViews() {
        avatarContainer.translatesAutoresizingMaskIntoConstraints = false
        avatarContainer.layer.cornerRadius = 22
        avatarContainer.layer.masksToBounds = true
        avatarContainer.backgroundColor = .systemBlue.withAlphaComponent(0.15)

        avatarView.translatesAutoresizingMaskIntoConstraints = false
        avatarView.font = .systemFont(ofSize: 20, weight: .medium)
        avatarView.textAlignment = .center
        avatarContainer.addSubview(avatarView)

        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.font = .systemFont(ofSize: 16, weight: .semibold)

        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        statusLabel.font = .systemFont(ofSize: 13)
        statusLabel.textColor = .secondaryLabel

        contentView.addSubview(avatarContainer)
        contentView.addSubview(nameLabel)
        contentView.addSubview(statusLabel)

        NSLayoutConstraint.activate([
            avatarContainer.leadingAnchor.constraint(equalTo: contentView.layoutMarginsGuide.leadingAnchor),
            avatarContainer.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            avatarContainer.widthAnchor.constraint(equalToConstant: 44),
            avatarContainer.heightAnchor.constraint(equalToConstant: 44),

            avatarView.centerXAnchor.constraint(equalTo: avatarContainer.centerXAnchor),
            avatarView.centerYAnchor.constraint(equalTo: avatarContainer.centerYAnchor),

            nameLabel.leadingAnchor.constraint(equalTo: avatarContainer.trailingAnchor, constant: 12),
            nameLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),

            statusLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            statusLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            statusLabel.trailingAnchor.constraint(lessThanOrEqualTo: contentView.layoutMarginsGuide.trailingAnchor),
        ])
    }

    func configure(with row: MessageHomeViewController.MessageRow) {
        avatarView.text = row.avatar
        nameLabel.text = row.name
        statusLabel.text = row.status
        accessibilityLabel = "\(row.name)，\(row.status)"
    }
}
