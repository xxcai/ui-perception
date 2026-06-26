import UIAbility from '@ohos.app.ability.UIAbility';
import type Want from '@ohos.app.ability.Want';
import type AbilityConstant from '@ohos.app.ability.AbilityConstant';
import { BusinessError } from '@ohos.base';
import window from '@ohos.window';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    console.info('[UIPerception] EntryAbility onCreate');
  }

  onDestroy(): void {
    console.info('[UIPerception] EntryAbility onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    console.info('[UIPerception] EntryAbility onWindowStageCreate');
    windowStage.loadContent('pages/Index', (err: BusinessError) => {
      if (err.code) {
        console.error('[UIPerception] Failed to load content: ' + JSON.stringify(err));
        return;
      }
      console.info('[UIPerception] Loaded content pages/Index');
    });
  }

  onWindowStageDestroy(): void {
    console.info('[UIPerception] EntryAbility onWindowStageDestroy');
  }

  onForeground(): void {
    console.info('[UIPerception] EntryAbility onForeground');
  }

  onBackground(): void {
    console.info('[UIPerception] EntryAbility onBackground');
  }
}
