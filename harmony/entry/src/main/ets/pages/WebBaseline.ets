import web_webview from '@ohos.web.webview';
import router from '@ohos.router';
import fs from '@ohos.file.fs';
import common from '@ohos.app.ability.common';
import util from '@ohos.util';
import { WebJsonParser } from '../perception/WebJsonParser';
import { RefAssigner } from '../perception/RefAssigner';
import { SnapshotRenderer } from '../perception/SnapshotRenderer';
import { SemanticNode } from '../perception/SemanticNode';

class WebCaptureProxy {
  onResultCb: ((json: string) => void) | null = null;

  setResultCallback(cb: (json: string) => void): void {
    this.onResultCb = cb;
  }

  onResult(json: string): void {
    const safe: string = json === null || json === undefined ? '' : json;
    console.info(`[UIPerception] Web proxy.onResult called, len=${safe.length}`);
    if (this.onResultCb !== null) {
      this.onResultCb(safe);
    }
  }
}

@Entry
@Component
struct WebBaseline {
  @State controller: web_webview.WebviewController = new web_webview.WebviewController();
  @State captureStatus: string = '点按钮抓取当前 H5';
  @State yamlPath: string = '';
  @State rawPath: string = '';
  @State isCapturing: boolean = false;
  @State pageReady: boolean = false;
  @State controllerAttached: boolean = false;

  private proxy: WebCaptureProxy = new WebCaptureProxy();

  private loadSerializerJs(): string {
    const ctx = getContext(this) as common.UIAbilityContext;
    const rm = ctx.resourceManager;
    const bytes = rm.getRawFileContentSync('dom-serializer.js');
    const decoder = util.TextDecoder.create('utf-8');
    const original = decoder.decodeWithStream(bytes);
    // Convert each `return JSON.stringify(...)` inside the original IIFE into
    // `return window.__webCaptureResult = JSON.stringify(...)` so the auto-invoked
    // IIFE still returns AND we can read the result via the global var afterwards.
    const patched = original.replace(
      /return\s+JSON\.stringify\(/g,
      'return window.__webCaptureResult = JSON.stringify(',
    );
    return '(function(){ try { '
      + 'window.__webCaptureResult = ""; '
      + patched + ' '
      + '} catch(e) { '
      + '  window.__webCaptureResult = JSON.stringify({ error: "inject_err", message: e.message }); '
      + '} '
      + 'if (window.__host && typeof window.__host.onResult === "function") { '
      + '  window.__host.onResult(window.__webCaptureResult || ""); '
      + '} else { console.error("[WebCap] __host proxy missing"); } '
      + '})();';
  }

  private captureWeb(): void {
    if (this.isCapturing) {
      return;
    }
    if (!this.controllerAttached) {
      this.captureStatus = 'controller 还没绑定，等页面加载完';
      return;
    }
    if (!this.pageReady) {
      this.captureStatus = '页面还没加载完，等 onPageEnd';
      return;
    }
    this.isCapturing = true;
    this.captureStatus = '注入中…';
    console.info('[UIPerception] Web captureWeb start, pageReady=true');

    let timeoutFired = false;
    const timeoutId = setTimeout(() => {
      timeoutFired = true;
      this.isCapturing = false;
      this.captureStatus = 'JS 注入超时（10s）';
      console.error('[UIPerception] Web capture timeout');
    }, 10000);

    this.proxy.setResultCallback((json: string) => {
      console.info(`[UIPerception] Web proxy cb fired, len=${json.length}, timeoutFired=${timeoutFired}`);
      if (timeoutFired) {
        return;
      }
      clearTimeout(timeoutId);
      this.isCapturing = false;
      this.handleCaptureResult(json);
    });

    try {
      const js = this.loadSerializerJs();
      console.info(`[UIPerception] Web injecting JS, total len=${js.length}`);
      this.controller.runJavaScript(js, (err: Error, result: string) => {
        console.info(`[UIPerception] Web runJavaScript cb: err=${err ? err.message : 'null'} result=${result}`);
        if (err) {
          clearTimeout(timeoutId);
          this.isCapturing = false;
          this.captureStatus = `RUNJS_ERR: ${err.message}`;
        }
      });
    } catch (e) {
      clearTimeout(timeoutId);
      this.isCapturing = false;
      this.captureStatus = `INJECT_ERR: ${(e as Error).message ?? String(e)}`;
      console.error(`[UIPerception] Web capture inject throw: ${(e as Error).message ?? String(e)}`);
    }
  }

  private handleCaptureResult(json: string): void {
    if (json === null || json === undefined || json.length === 0) {
      this.captureStatus = 'JS 返回空结果';
      return;
    }
    const tree = WebJsonParser.parse(json);
    if (tree === null) {
      this.captureStatus = `PARSE_FAIL (${json.length}b)`;
      return;
    }
    const withRefs = RefAssigner.assign(tree, 'w', true);
    if (withRefs === null) {
      this.captureStatus = 'REF_FAIL';
      return;
    }
    const yaml = SnapshotRenderer.render(withRefs);

    const ctx = getContext(this) as common.UIAbilityContext;
    const filesDir = ctx.filesDir;
    const ts = new Date().getTime();
    const yamlPath = `${filesDir}/web_snapshot_${ts}.yml`;
    const rawPath = `${filesDir}/web_raw_${ts}.json`;
    let writeErr = '';
    try {
      const f1 = fs.openSync(yamlPath, fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
      fs.writeSync(f1.fd, yaml);
      fs.closeSync(f1);
      const f2 = fs.openSync(rawPath, fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
      fs.writeSync(f2.fd, json);
      fs.closeSync(f2);
    } catch (e) {
      writeErr = (e as Error).message ?? String(e);
    }

    const nodeCount = this.countNodes(withRefs);
    this.captureStatus = `Web: ${nodeCount} nodes → ${yaml.length}b yaml${writeErr.length > 0 ? ' WRITE_ERR ' + writeErr : ''}`;
    this.yamlPath = yamlPath;
    this.rawPath = rawPath;
    console.info(`[UIPerception] Web yaml path=${yamlPath} raw=${rawPath} status=${this.captureStatus}`);
    console.info(`[UIPerception] Web yaml:\n${yaml}`);
  }

  private countNodes(node: SemanticNode | null): number {
    if (node === null) {
      return 0;
    }
    let count = 1;
    for (let i = 0; i < node.children.length; i++) {
      count += this.countNodes(node.children[i]);
    }
    return count;
  }

  build() {
    Column() {
      Row() {
        Button('返回')
          .fontSize(14)
          .onClick(() => {
            router.back();
          })
        Blank()
        Text('Web Baseline')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
        Blank()
        Button(this.isCapturing ? '抓取中…' : (this.pageReady ? '抓取 H5' : '加载中…'))
          .fontSize(14)
          .backgroundColor('#FF9800')
          .enabled(!this.isCapturing && this.pageReady)
          .onClick(() => {
            this.captureWeb();
          })
      }
      .width('100%')
      .padding(8)

      Web({ src: 'resource://rawfile/web_form.html', controller: this.controller })
        .domStorageAccess(true)
        .fileAccess(true)
        .javaScriptAccess(true)
        .javaScriptProxy({
          object: this.proxy,
          name: '__host',
          methodList: ['onResult'],
          controller: this.controller,
        })
        .onControllerAttached(() => {
          this.controllerAttached = true;
          console.info('[UIPerception] Web controller attached');
        })
        .onPageEnd(() => {
          this.pageReady = true;
          this.captureStatus = '页面就绪，可以抓取';
          console.info('[UIPerception] Web onPageEnd');
        })
        .onConsole((e: OnConsoleEvent): boolean => {
          console.info(`[UIPerception] Web CONSOLE: ${JSON.stringify(e)}`);
          return false;
        })
        .width('100%')
        .layoutWeight(1)

      if (this.captureStatus.length > 0) {
        Text(this.captureStatus)
          .fontSize(12)
          .fontColor('#666666')
          .width('95%')
          .margin({ top: 4, bottom: 2 })
      }
      if (this.yamlPath.length > 0) {
        Text(`YAML: ${this.yamlPath}`)
          .fontSize(10)
          .fontColor('#999999')
          .width('95%')
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      if (this.rawPath.length > 0) {
        Text(`RAW: ${this.rawPath}`)
          .fontSize(10)
          .fontColor('#999999')
          .width('95%')
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .margin({ bottom: 4 })
      }
    }
    .width('100%')
    .height('100%')
  }
}
