## Phone Testing

This project includes an Android phone testing system. Use the `@phone-test` subagent to execute structured test cases.

### Usage

```
@phone-test 执行 testcases/contact-picker.md
```

### Writing Test Cases

Test cases go in `testcases/` as Markdown files with 4 sections:

- **前置条件** (Preconditions): Must be true before starting
- **测试步骤** (Steps): Execute sequentially
- **断言与验证** (Assertions): Verify after all steps
- **后置清理** (Cleanup): Run regardless of result

See `testcases/contact-picker.md` for an example.
