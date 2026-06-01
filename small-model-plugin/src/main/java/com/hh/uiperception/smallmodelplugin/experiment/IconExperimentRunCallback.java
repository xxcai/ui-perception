package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 图标识别实验运行回调。成功和失败都会返回结构化 run result。
 */
public interface IconExperimentRunCallback {

    void onComplete(IconExperimentRunResult result);
}
