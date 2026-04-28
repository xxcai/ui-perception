package com.hh.uiperception.baseline.nativepage;

final class ContactPerson {
    final String avatarText;
    final String name;
    final String workId;
    final String tag;
    final String department;
    final int avatarBgRes;
    final int avatarTextColor;
    final boolean marked;

    ContactPerson(String avatarText, String name, String workId, String tag, String department,
                  int avatarBgRes, int avatarTextColor, boolean marked) {
        this.avatarText = avatarText;
        this.name = name;
        this.workId = workId;
        this.tag = tag;
        this.department = department;
        this.avatarBgRes = avatarBgRes;
        this.avatarTextColor = avatarTextColor;
        this.marked = marked;
    }
}
