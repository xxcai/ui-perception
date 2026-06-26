import { FrameNode } from '@ohos.arkui.node';
import fs from '@ohos.file.fs';
import common from '@ohos.app.ability.common';
import { HarmonyInspectorParser } from '../perception/HarmonyInspectorParser';
import { HarmonySemanticTreeBuilder } from '../perception/HarmonySemanticTreeBuilder';
import { RefAssigner } from '../perception/RefAssigner';
import { SnapshotRenderer } from '../perception/SnapshotRenderer';
import { SemanticNode } from '../perception/SemanticNode';

interface LayoutNode {
  type: string;
  id: string;
  uniqueId: number;
  x: number;
  y: number;
  width: number;
  height: number;
  hasInspectorInfo: boolean;
  children: LayoutNode[];
}

@Entry
@Component
struct Index {
  @State clickCount: number = 0;
  @State switchOn: boolean = false;
  @State inputText: string = '';
  @State items: Array<string> = ['梁晓舟', '苏清沅', '顾衍', '林晚秋'];
  @State captureStatus: string = '';
  @State captureAPath: string = '';
  @State captureBPath: string = '';

  private walkFrameNode(node: FrameNode): LayoutNode {
    const pos = node.getPositionToWindow();
    const size = node.getMeasuredSize();
    const count: number = node.getChildrenCount();
    const children: LayoutNode[] = [];
    for (let i = 0; i < count; i++) {
      const child = node.getChild(i);
      if (child !== null) {
        children.push(this.walkFrameNode(child));
      }
    }
    let hasInfo = false;
    try {
      const info = node.getInspectorInfo();
      hasInfo = info !== null && info !== undefined;
    } catch (e) {
      hasInfo = false;
    }
    return {
      type: node.getNodeType(),
      id: node.getId(),
      uniqueId: node.getUniqueId(),
      x: pos.x,
      y: pos.y,
      width: size.width,
      height: size.height,
      hasInspectorInfo: hasInfo,
      children: children,
    };
  }

  private captureA(): string {
    const ctx = this.getUIContext();
    const root = ctx.getFrameNodeById('app_root');
    if (root === null) {
      return JSON.stringify({ error: 'root FrameNode not found' });
    }
    const tree: LayoutNode = this.walkFrameNode(root);
    return JSON.stringify(tree, null, 2);
  }

  private captureB(): string {
    const ctx = this.getUIContext();
    return ctx.getFilteredInspectorTree();
  }

  private captureBFiltered(filters: Array<string>): string {
    const ctx = this.getUIContext();
    return ctx.getFilteredInspectorTree(filters);
  }

  private writeToFile(path: string, content: string): void {
    const file = fs.openSync(path, fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
    fs.writeSync(file.fd, content);
    fs.closeSync(file);
  }

  private captureBoth(): void {
    const ctx = getContext(this) as common.Context;
    const filesDir = ctx.filesDir;
    console.info(`[UIPerception] filesDir=${filesDir}`);
    let jsonA: string = '';
    let jsonB: string = '';
    let errA: string = '';
    let errB: string = '';
    let writeErrA: string = '';
    let writeErrB: string = '';

    try {
      jsonA = this.captureA();
    } catch (e) {
      errA = (e as Error).message ?? String(e);
    }
    try {
      jsonB = this.captureB();
    } catch (e) {
      errB = (e as Error).message ?? String(e);
    }

    const pathA = `${filesDir}/capture_A_framenode.json`;
    const pathB = `${filesDir}/capture_B_inspector.json`;
    if (jsonA.length > 0) {
      try {
        this.writeToFile(pathA, jsonA);
        this.captureAPath = pathA;
      } catch (e) {
        writeErrA = (e as Error).message ?? String(e);
      }
    }
    if (jsonB.length > 0) {
      try {
        this.writeToFile(pathB, jsonB);
        this.captureBPath = pathB;
      } catch (e) {
        writeErrB = (e as Error).message ?? String(e);
      }
    }

    this.captureStatus =
      `A(FrameNode): ${errA.length > 0 ? 'CAP_ERR ' + errA : jsonA.length + ' bytes'}${writeErrA.length > 0 ? ' WRITE_ERR ' + writeErrA : ''}\n` +
      `B(InspectorTree): ${errB.length > 0 ? 'CAP_ERR ' + errB : jsonB.length + ' bytes'}${writeErrB.length > 0 ? ' WRITE_ERR ' + writeErrB : ''}`;
    console.info(`[UIPerception] captureStatus=${this.captureStatus}`);
  }

  private testFilters(): void {
    const ctx = getContext(this) as common.Context;
    const filesDir = ctx.filesDir;
    const configs: Array<Array<string>> = [
      ['content'],
      ['content', 'enabled', 'label', 'accessibilityText', 'accessibilityDescription'],
      ['content', 'enabled', 'clickable', 'checked', 'selected', 'scrollable', 'accessibilityText'],
    ];
    const sizes: Array<string> = [];
    configs.forEach((filters, idx) => {
      let json = '';
      let err = '';
      try {
        json = this.captureBFiltered(filters);
      } catch (e) {
        err = (e as Error).message ?? String(e);
      }
      const path = `${filesDir}/capture_B_filter${idx}.json`;
      if (json.length > 0) {
        try {
          this.writeToFile(path, json);
          sizes.push(`F${idx}[${filters.length}]: ${json.length}b`);
        } catch (e) {
          sizes.push(`F${idx}: WRITE_ERR`);
        }
      } else {
        sizes.push(`F${idx}: ${err}`);
      }
    });
    this.captureStatus = sizes.join(' | ');
    console.info(`[UIPerception] filter test: ${this.captureStatus}`);
  }

  private testV5Algorithm(): void {
    const ctx = getContext(this) as common.Context;
    const filesDir = ctx.filesDir;
    const filters: Array<string> = [
      'content', 'label', 'placeholder', 'enabled', 'focusable',
      'selected', 'selectable', 'scrollable',
      'accessibilityText', 'accessibilityDescription',
      'accessibilityRole', 'accessibilityActionOptions',
    ];

    let inspectorJson = '';
    let captureErr = '';
    try {
      inspectorJson = this.captureBFiltered(filters);
    } catch (e) {
      captureErr = (e as Error).message ?? String(e);
    }

    if (inspectorJson.length === 0) {
      this.captureStatus = `V5 CAP_ERR: ${captureErr}`;
      return;
    }

    const inspectorRoot = HarmonyInspectorParser.parse(inspectorJson);
    if (inspectorRoot === null) {
      this.captureStatus = `V5 PARSE_ERR (inspector json ${inspectorJson.length}b)`;
      return;
    }

    const semanticRoot = HarmonySemanticTreeBuilder.build(inspectorRoot);
    if (semanticRoot === null) {
      this.captureStatus = 'V5 BUILD_ERR';
      return;
    }

    const assigned = RefAssigner.assign(semanticRoot, 'n', false);
    const yaml = SnapshotRenderer.render(assigned);

    const ts = new Date().getTime();
    const yamlPath = `${filesDir}/v5_snapshot_${ts}.yml`;
    const rawPath = `${filesDir}/v5_inspector_${ts}.json`;
    let writeErr = '';
    try {
      this.writeToFile(yamlPath, yaml);
      this.writeToFile(rawPath, inspectorJson);
    } catch (e) {
      writeErr = (e as Error).message ?? String(e);
    }

    const yamlBytes = yaml.length;
    const nodeCount = this.countSemanticNodes(assigned);
    this.captureStatus =
      `V5: ${nodeCount} nodes → ${yamlBytes}b yaml${writeErr.length > 0 ? ' WRITE_ERR ' + writeErr : ''}`;
    this.captureAPath = yamlPath;
    this.captureBPath = rawPath;
    console.info(`[UIPerception] V5 yaml path=${yamlPath} raw=${rawPath} status=${this.captureStatus}`);
    console.info(`[UIPerception] V5 yaml:\n${yaml}`);
  }

  private countSemanticNodes(node: SemanticNode | null): number {
    if (node === null) {
      return 0;
    }
    let count = 1;
    for (let i = 0; i < node.children.length; i++) {
      count += this.countSemanticNodes(node.children[i]);
    }
    return count;
  }

  build() {
    Column() {
      Text('UIPerception 鸿蒙 PoC')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .margin({ top: 32, bottom: 24 })

      Button(this.clickCount === 0 ? '点击我' : `已点击 ${this.clickCount} 次`)
        .fontSize(18)
        .margin({ bottom: 16 })
        .onClick(() => {
          this.clickCount++;
        })

      Row() {
        Text('启用通知')
          .fontSize(16)
        Blank()
        Toggle({ type: ToggleType.Switch, isOn: this.switchOn })
          .onChange((on: boolean) => {
            this.switchOn = on;
          })
      }
      .width('85%')
      .margin({ bottom: 16 })

      TextInput({ placeholder: '请输入搜索关键词', text: this.inputText })
        .width('85%')
        .onChange((value: string) => {
          this.inputText = value;
        })
        .margin({ bottom: 16 })

      List({ space: 12 }) {
        ForEach(this.items, (item: string, idx: number) => {
          ListItem() {
            Row() {
              Text(item)
                .fontSize(16)
              Blank()
              Text(`#${idx + 1}`)
                .fontSize(14)
                .fontColor('#888888')
            }
            .width('100%')
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#F5F5F5')
          }
        }, (item: string) => item)
      }
      .width('90%')
      .layoutWeight(1)

      Divider()
        .margin({ top: 8, bottom: 8 })

      Button('抓取布局 (A+B 对比)')
        .fontSize(16)
        .backgroundColor('#4CAF50')
        .margin({ bottom: 8 })
        .onClick(() => {
          this.captureBoth();
        })

      Button('测试 B filters')
        .fontSize(16)
        .backgroundColor('#2196F3')
        .margin({ bottom: 8 })
        .onClick(() => {
          this.testFilters();
        })

      Button('V5 算法 (parse→semantic→ref→yaml)')
        .fontSize(16)
        .backgroundColor('#FF9800')
        .margin({ bottom: 8 })
        .onClick(() => {
          this.testV5Algorithm();
        })

      if (this.captureStatus.length > 0) {
        Text(this.captureStatus)
          .fontSize(12)
          .fontColor('#666666')
          .width('90%')
          .margin({ bottom: 4 })
      }
      if (this.captureAPath.length > 0) {
        Text(`A: ${this.captureAPath}`)
          .fontSize(10)
          .fontColor('#999999')
          .width('90%')
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .margin({ bottom: 2 })
      }
      if (this.captureBPath.length > 0) {
        Text(`B: ${this.captureBPath}`)
          .fontSize(10)
          .fontColor('#999999')
          .width('90%')
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
    }
    .id('app_root')
    .width('100%')
    .height('100%')
    .alignItems(HorizontalAlign.Center)
  }
}
