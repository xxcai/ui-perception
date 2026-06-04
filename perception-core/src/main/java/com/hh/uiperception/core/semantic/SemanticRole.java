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
    WEBVIEW("webview"),
    LINK("link"),
    HEADING("heading"),
    NAVIGATION("navigation"),
    TEXTBOX("textbox"),
    SEARCHBOX("searchbox"),
    SPINBUTTON("spinbutton"),
    COMBOBOX("combobox"),
    LISTBOX("listbox"),
    TABLE_ROLE("table"),
    ROW("row"),
    CELL("cell"),
    COLUMNHEADER("columnheader"),
    ROWHEADER("rowheader"),
    FORM("form"),
    ARTICLE("article"),
    COMPLEMENTARY("complementary"),
    BLOCKQUOTE("blockquote"),
    CAPTION("caption"),
    GROUP("group"),
    TERM("term"),
    DEFINITION("definition"),
    SEPARATOR("separator"),
    METER("meter"),
    OPTION_ROLE("option"),
    STATUS("status"),
    PARAGRAPH("paragraph"),
    ROWGROUP("rowgroup");

    private final String snapshotName;

    SemanticRole(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String snapshotName() {
        return snapshotName;
    }
}
