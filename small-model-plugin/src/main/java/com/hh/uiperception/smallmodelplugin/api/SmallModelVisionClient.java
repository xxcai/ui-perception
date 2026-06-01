package com.hh.uiperception.smallmodelplugin.api;

import android.content.Context;

/**
 * 小模型视觉理解能力接口。
 *
 * 调试界面和后续测评插件都只依赖这个接口，不直接依赖具体模型运行时。
 */
public interface SmallModelVisionClient {

    void initialize(Context context, SmallModelInitConfig config, SmallModelCallback<Void> callback);

    void analyze(SmallModelRequest request, SmallModelCallback<SmallModelResult> callback);

    boolean isInitialized();

    void close();
}
