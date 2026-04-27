package com.hh.uiperception.capture;

import android.app.Activity;

/**
 * 抓取通道接口：定义一次页面抓取的执行协议。
 *
 * 每个通道实现此接口，并在 CaptureChannelRegistry 中注册。
 * 后续新增通道只需实现此接口并注册。
 */
public interface CaptureChannel {

    /**
     * 通道唯一名称，如 "native_xml"、"web_dom"、"screenshot_ocr"。
     */
    String name();

    /**
     * 对通道能力的一句话描述。
     */
    String description();

    /**
     * 在给定 Activity 上执行抓取。
     *
     * @param activity 前台 Activity，不为 null
     * @param request  抓取请求
     * @return 抓取结果，不为 null
     */
    CaptureResult capture(Activity activity, CaptureRequest request);
}
