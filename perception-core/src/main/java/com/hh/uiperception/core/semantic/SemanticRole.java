package com.hh.uiperception.core.semantic;

/**
 * Android native semantic snapshot 第一版 role 集合。
 */
public enum SemanticRole {
    SCREEN("screen"),
    TOOLBAR("toolbar"),
    TABBAR("tabbar"),
    TAB("tab"),
    LIST("list"),
    GRID("grid"),
    LIST_ITEM("listitem"),
    SCROLL("scroll"),
    CARD("card"),
    SECTION("section"),
    DIALOG("dialog"),
    SHEET("sheet"),
    TEXT("text"),
    IMAGE("image"),
    BUTTON("button"),
    INPUT("input"),
    CHECKBOX("checkbox"),
    RADIO("radio"),
    SWITCH("switch"),
    SLIDER("slider"),
    PICKER("picker"),
    PROGRESS("progress"),
    GENERIC("generic"),
    LINK("link"),
    HEADING("heading"),
    NAVIGATION("navigation");

    private final String snapshotName;

    SemanticRole(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String snapshotName() {
        return snapshotName;
    }
}
