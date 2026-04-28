package com.hh.uiperception.baseline.nativepage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class BusinessRow {
    static final int TYPE_SEARCH = 0;
    static final int TYPE_RECENT = 1;
    static final int TYPE_SIMPLE = 2;
    static final int TYPE_EMPTY = 3;
    static final int TYPE_BANNER = 4;
    static final int TYPE_ATTENDANCE = 5;
    static final int TYPE_APPROVAL = 6;
    static final int TYPE_PLINK = 7;
    static final int TYPE_NOTICE = 8;
    static final int TYPE_IMAGE_BULLET = 9;
    static final int TYPE_APP_GRID = 10;

    final int type;
    final String title;
    final String rightText;
    final String subtitle;
    final List<String> items;
    final int iconRes;

    private BusinessRow(int type, String title, String rightText, String subtitle, List<String> items, int iconRes) {
        this.type = type;
        this.title = title;
        this.rightText = rightText;
        this.subtitle = subtitle;
        this.items = items;
        this.iconRes = iconRes;
    }

    static BusinessRow search() {
        return new BusinessRow(TYPE_SEARCH, "", "", "", new ArrayList<>(), 0);
    }

    static BusinessRow recent() {
        return new BusinessRow(TYPE_RECENT, "最近使用", "", "前往HIS MALL发现更多应用",
                Arrays.asList("华为Wi-Fi", "设备管理"), 0);
    }

    static BusinessRow simple(String title, String rightText) {
        return new BusinessRow(TYPE_SIMPLE, title, rightText, "", new ArrayList<>(), 0);
    }

    static BusinessRow empty(String title, String message) {
        return new BusinessRow(TYPE_EMPTY, title, "", message, new ArrayList<>(), 0);
    }

    static BusinessRow banner(String title, int imageRes) {
        return new BusinessRow(TYPE_BANNER, title, "", "", new ArrayList<>(), imageRes);
    }

    static BusinessRow attendance() {
        return new BusinessRow(TYPE_ATTENDANCE, "每日打卡", "", "轻松打卡，快乐工作", new ArrayList<>(), 0);
    }

    static BusinessRow approval() {
        return new BusinessRow(TYPE_APPROVAL, "待办审批", "", "",
                Arrays.asList("iHR_个税（Tax）|2", "PMALL|99+", "iHR_组织氛围调查(HWOCS)|4"), 0);
    }

    static BusinessRow plink() {
        return new BusinessRow(TYPE_PLINK, "P-Link", "", "暂无内容",
                Arrays.asList("项目", "任务", "动态", "更多"), 0);
    }

    static BusinessRow notice(String title, String message) {
        return new BusinessRow(TYPE_NOTICE, title, "", message, new ArrayList<>(), 0);
    }

    static BusinessRow imageBullet() {
        return new BusinessRow(TYPE_IMAGE_BULLET, "CBGIOC-MSS作战数据平台", "", "MSS战区一站式数字化运营平台",
                Arrays.asList("查看每日MSS最新战报", "查看内控管理运作分析", "查看每日中国区最新战报", "查看每日财经最新战报"),
                0);
    }

    static BusinessRow appGrid() {
        return new BusinessRow(TYPE_APP_GRID, "我的应用", "", "",
                Arrays.asList("sitIplatform", "群话题", "h5mo", "我的客户", "常用热线", "更多"), 0);
    }
}
