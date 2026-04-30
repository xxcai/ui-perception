package com.hh.uiperception.baseline.nativepage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

import java.util.List;

final class ContactsAdapter extends BaseAdapter {
    private final List<ContactRow> rows;

    ContactsAdapter(List<ContactRow> rows) {
        this.rows = rows;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public ContactRow getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ContactRow row = rows.get(position);
        if (row.type == ContactRow.TYPE_SEARCH) {
            return searchRow(parent);
        } else if (row.type == ContactRow.TYPE_SHORTCUTS) {
            return shortcutsRow(parent);
        } else if (row.type == ContactRow.TYPE_SECTION) {
            return sectionRow(parent, row.section);
        } else if (row.type == ContactRow.TYPE_FOOTER) {
            return footerRow(parent, row.section);
        }
        return contactRow(parent, row.person);
    }

    @Override
    public boolean isEnabled(int position) {
        int type = rows.get(position).type;
        return type == ContactRow.TYPE_SHORTCUTS || type == ContactRow.TYPE_CONTACT;
    }

    private View searchRow(ViewGroup parent) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(parent, 16), dp(parent, 8), dp(parent, 16), dp(parent, 8));
        row.setBackgroundColor(Color.WHITE);

        TextView search = new TextView(parent.getContext());
        search.setText("搜索");
        search.setTextSize(16);
        search.setTextColor(0xFFC9CDD2);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(parent, 16), 0, dp(parent, 12), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(parent, 8));
        search.setBackgroundResource(R.drawable.bg_search);
        UiKit.markClickable(search);
        row.addView(search, new LinearLayout.LayoutParams(0, dp(parent, 36), 1f));

        LinearLayout service = new LinearLayout(parent.getContext());
        service.setOrientation(LinearLayout.VERTICAL);
        service.setGravity(Gravity.CENTER);
        service.setPadding(dp(parent, 12), 0, 0, 0);
        UiKit.markClickable(service);
        service.addView(UiKit.iconImage(parent.getContext(), R.drawable.ic_headset, 0),
                new LinearLayout.LayoutParams(dp(parent, 22), dp(parent, 22)));
        TextView label = text(parent, "客服", UiKit.TEXT_PRIMARY, 12);
        label.setGravity(Gravity.CENTER);
        service.addView(label);
        row.addView(service, new LinearLayout.LayoutParams(dp(parent, 64), dp(parent, 42)));
        return row;
    }

    private View shortcutsRow(ViewGroup parent) {
        HorizontalScrollView scrollView = new HorizontalScrollView(parent.getContext());
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(parent, 18), 0, dp(parent, 18));
        row.addView(shortcut(parent, R.drawable.ic_contact_ai, "小微"));
        row.addView(shortcut(parent, R.drawable.ic_contact_org, "组织"));
        row.addView(shortcut(parent, R.drawable.ic_contact_team, "团队"));
        row.addView(shortcut(parent, R.drawable.ic_star_filled, "关注"));
        row.addView(shortcut(parent, R.drawable.ic_contact_external, "外部"));
        row.addView(shortcut(parent, R.drawable.ic_contact_public, "公众号"));

        scrollView.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View shortcut(ViewGroup parent, int iconRes, String label) {
        LinearLayout item = new LinearLayout(parent.getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(parent, 8), 0, dp(parent, 8), 0);
        UiKit.markClickable(item);

        ImageView icon = UiKit.iconImage(parent.getContext(), iconRes, R.drawable.bg_avatar_light);
        item.addView(icon, new LinearLayout.LayoutParams(dp(parent, 52), dp(parent, 52)));

        TextView text = text(parent, label, UiKit.TEXT_PRIMARY, 15);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(parent, 8), 0, 0);
        item.addView(text);
        item.setLayoutParams(new LinearLayout.LayoutParams(dp(parent, 96), ViewGroup.LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View sectionRow(ViewGroup parent, String section) {
        TextView view = text(parent, section, 0xFF8A8D91, 14);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(parent, 16), 0, 0, 0);
        view.setBackgroundColor(0xFFF6F7F9);
        view.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(parent, 38)
        ));
        return view;
    }

    private View contactRow(ViewGroup parent, ContactPerson person) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(dp(parent, 16), dp(parent, 12), dp(parent, 16), dp(parent, 10));
        row.setMinimumHeight(dp(parent, 86));
        row.setBackgroundColor(Color.WHITE);

        android.widget.FrameLayout avatarFrame = new android.widget.FrameLayout(parent.getContext());
        TextView avatar = avatar(parent, person);
        avatarFrame.addView(avatar, new android.widget.FrameLayout.LayoutParams(dp(parent, 48), dp(parent, 48)));
        if (person.marked) {
            ImageView mark = UiKit.iconImage(parent.getContext(), R.drawable.ic_contact_mark, 0);
            android.widget.FrameLayout.LayoutParams markParams =
                    new android.widget.FrameLayout.LayoutParams(dp(parent, 18), dp(parent, 18));
            markParams.gravity = Gravity.END | Gravity.BOTTOM;
            avatarFrame.addView(mark, markParams);
        }
        row.addView(avatarFrame, new LinearLayout.LayoutParams(dp(parent, 56), dp(parent, 56)));

        LinearLayout body = new LinearLayout(parent.getContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(parent, 12), 0, 0, 0);

        LinearLayout firstLine = new LinearLayout(parent.getContext());
        firstLine.setOrientation(LinearLayout.HORIZONTAL);
        firstLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(parent, person.name, UiKit.TEXT_PRIMARY, 18);
        name.setSingleLine(true);
        firstLine.addView(name);

        TextView workId = text(parent, person.workId, UiKit.TEXT_SECONDARY, 16);
        workId.setSingleLine(true);
        workId.setPadding(dp(parent, 8), 0, 0, 0);
        firstLine.addView(workId);

        if (!person.tag.isEmpty()) {
            TextView tag = text(parent, person.tag, UiKit.BLUE, 12);
            tag.setPadding(dp(parent, 6), dp(parent, 1), dp(parent, 6), dp(parent, 1));
            tag.setBackgroundResource(R.drawable.bg_tag);
            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tagParams.setMargins(dp(parent, 8), 0, 0, 0);
            firstLine.addView(tag, tagParams);
        }
        body.addView(firstLine);

        TextView department = text(parent, person.department, UiKit.TEXT_SECONDARY, 15);
        department.setSingleLine(true);
        department.setPadding(0, dp(parent, 5), 0, 0);
        body.addView(department);

        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View footerRow(ViewGroup parent, String text) {
        TextView view = text(parent, text, UiKit.TEXT_SECONDARY, 15);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xFFF6F7F9);
        view.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(parent, 72)
        ));
        return view;
    }

    private TextView avatar(ViewGroup parent, ContactPerson person) {
        TextView view = text(parent, person.avatarText, person.avatarTextColor, 22);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(person.avatarBgRes);
        return view;
    }

    private TextView text(ViewGroup parent, String value, int color, int sizeSp) {
        TextView view = new TextView(parent.getContext());
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        return view;
    }

    private int dp(ViewGroup parent, int value) {
        return UiKit.dp(parent.getContext(), value);
    }
}
