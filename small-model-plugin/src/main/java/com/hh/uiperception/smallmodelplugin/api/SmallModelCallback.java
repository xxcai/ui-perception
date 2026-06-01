package com.hh.uiperception.smallmodelplugin.api;

/**
 * 小模型异步调用回调。
 */
public interface SmallModelCallback<T> {

    void onSuccess(T value);

    void onError(SmallModelError error);
}
