package com.hh.uiperception.baseline.nativepage;

final class ContactRow {
    static final int TYPE_SEARCH = 0;
    static final int TYPE_SHORTCUTS = 1;
    static final int TYPE_SECTION = 2;
    static final int TYPE_CONTACT = 3;
    static final int TYPE_FOOTER = 4;

    final int type;
    final String section;
    final ContactPerson person;

    private ContactRow(int type, String section, ContactPerson person) {
        this.type = type;
        this.section = section;
        this.person = person;
    }

    static ContactRow search() {
        return new ContactRow(TYPE_SEARCH, "", null);
    }

    static ContactRow shortcuts() {
        return new ContactRow(TYPE_SHORTCUTS, "", null);
    }

    static ContactRow section(String section) {
        return new ContactRow(TYPE_SECTION, section, null);
    }

    static ContactRow contact(ContactPerson person) {
        return new ContactRow(TYPE_CONTACT, "", person);
    }

    static ContactRow footer(int count) {
        return new ContactRow(TYPE_FOOTER, count + " 个联系人", null);
    }
}
