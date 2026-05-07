package com.hh.uiperception.baseline.nativepage;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsHomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        root.addView(createTopArea(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout listFrame = new FrameLayout(getActivity());
        ListView listView = new ListView(getActivity());
        listView.setDivider(null);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setSelector(android.R.color.transparent);
        listView.setAdapter(new ContactsAdapter(createRows()));
        listView.setOnItemClickListener((parent, view, position, id) -> {
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> true);
        listFrame.addView(listView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        listFrame.addView(createLetterIndex(), indexParams());

        root.addView(listFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return root;
    }

    private View createTopArea() {
        LinearLayout top = new LinearLayout(getActivity());
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), 0);
        top.setBackgroundColor(Color.WHITE);

        LinearLayout titleRow = new LinearLayout(getActivity());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        ImageView profile = UiKit.iconImage(getActivity(), R.drawable.ic_profile, R.drawable.bg_avatar_light);
        UiKit.markClickable(profile);
        titleRow.addView(profile, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = UiKit.title(getActivity(), "通讯录");
        title.setPadding(dp(18), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView plus = UiKit.iconImage(getActivity(), R.drawable.ic_plus, 0);
        UiKit.markClickable(plus);
        titleRow.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40)));

        top.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        return top;
    }

    private View createLetterIndex() {
        LinearLayout index = new LinearLayout(getActivity());
        index.setOrientation(LinearLayout.VERTICAL);
        index.setGravity(Gravity.CENTER);
        index.setPadding(0, dp(4), 0, dp(4));
        String[] letters = {"C", "L", "W", "X", "Y", "Z"};
        for (String letter : letters) {
            TextView view = new TextView(getActivity());
            view.setText(letter);
            view.setTextColor(UiKit.TEXT_PRIMARY);
            view.setTextSize(12);
            view.setGravity(Gravity.CENTER);
            UiKit.markClickable(view);
            index.addView(view, new LinearLayout.LayoutParams(dp(24), dp(22)));
        }
        return index;
    }

    private FrameLayout.LayoutParams indexParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        params.setMargins(0, 0, dp(4), 0);
        return params;
    }

    private List<ContactRow> createRows() {
        List<ContactRow> rows = new ArrayList<>();
        rows.add(ContactRow.search());
        rows.add(ContactRow.shortcuts());
        addSection(rows, "C",
                new ContactPerson("程", "程可欣", "EMP-A1001", "", "产品体验部@深圳", R.drawable.bg_avatar_light, UiKit.BLUE, false),
                new ContactPerson("曹", "曹景行", "EXT-B2002", "合作人员", "测试平台项目组", R.drawable.bg_avatar_cyan, 0xFF00A6D6, true)
        );
        addSection(rows, "L",
                new ContactPerson("林", "林知远", "EMP-A1003", "", "研发效能部@上海", R.drawable.bg_avatar_blue, 0xFFFFFFFF, false),
                new ContactPerson("陆", "陆明澈", "EMP-A1004", "", "质量保障中心", R.drawable.bg_avatar_violet, 0xFFFFFFFF, false)
        );
        addSection(rows, "W",
                new ContactPerson("吴", "吴清越", "EMP-A1005", "", "前端基础设施组", R.drawable.bg_avatar_purple, 0xFFFFFFFF, false)
        );
        addSection(rows, "X",
                new ContactPerson("徐", "徐一航", "EXT-B2006", "合作人员", "1231232qqq", R.drawable.bg_avatar_light, UiKit.BLUE, true),
                new ContactPerson("夏", "夏若辰", "EMP-A1007", "", "移动体验实验室", R.drawable.bg_avatar_gray, 0xFFFFFFFF, false)
        );
        addSection(rows, "Y",
                new ContactPerson("姚", "姚明远", "EMP-A1008", "", "测试@林知远", R.drawable.bg_avatar_light, UiKit.BLUE, false)
        );
        rows.add(ContactRow.footer(8));
        return rows;
    }

    private void addSection(List<ContactRow> rows, String section, ContactPerson... people) {
        rows.add(ContactRow.section(section));
        for (ContactPerson person : people) {
            rows.add(ContactRow.contact(person));
        }
    }

    private int dp(int value) {
        return UiKit.dp(getActivity(), value);
    }
}
