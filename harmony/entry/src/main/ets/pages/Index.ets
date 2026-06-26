@Entry
@Component
struct Index {
  @State clickCount: number = 0;
  @State switchOn: boolean = false;
  @State inputText: string = '';
  @State items: Array<string> = ['梁晓舟', '苏清沅', '顾衍', '林晚秋'];

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
    }
    .width('100%')
    .height('100%')
    .alignItems(HorizontalAlign.Center)
  }
}
