package com.hh.uiperception.baseline.nativepage;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

final class MailAdapter extends RecyclerView.Adapter<MailAdapter.MailViewHolder> {
    private final List<MailItem> items;

    MailAdapter(List<MailItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public MailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(0xFFFFFFFF);
        row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout content = new LinearLayout(parent.getContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.TOP);
        content.setPadding(dp(parent, 16), dp(parent, 12), dp(parent, 16), dp(parent, 12));

        LinearLayout textGroup = new LinearLayout(parent.getContext());
        textGroup.setOrientation(LinearLayout.VERTICAL);

        TextView sender = text(parent, UiKit.TEXT_PRIMARY, 17);
        sender.setSingleLine(true);
        TextView subject = text(parent, UiKit.TEXT_PRIMARY, 15);
        subject.setSingleLine(true);
        subject.setPadding(0, dp(parent, 4), 0, 0);
        TextView preview = text(parent, UiKit.TEXT_SECONDARY, 14);
        preview.setSingleLine(true);
        preview.setPadding(0, dp(parent, 4), 0, 0);

        textGroup.addView(sender);
        textGroup.addView(subject);
        textGroup.addView(preview);
        content.addView(textGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView time = text(parent, UiKit.TEXT_SECONDARY, 13);
        time.setGravity(Gravity.END);
        content.addView(time, new LinearLayout.LayoutParams(dp(parent, 64), ViewGroup.LayoutParams.WRAP_CONTENT));

        View divider = new View(parent.getContext());
        divider.setBackgroundColor(UiKit.DIVIDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        dividerParams.setMargins(dp(parent, 16), 0, dp(parent, 16), 0);

        row.addView(content);
        row.addView(divider, dividerParams);
        return new MailViewHolder(row, sender, subject, preview, time);
    }

    @Override
    public void onBindViewHolder(@NonNull MailViewHolder holder, int position) {
        MailItem item = items.get(position);
        holder.sender.setText(item.sender);
        holder.subject.setText(item.subject);
        holder.preview.setText(item.preview);
        holder.time.setText(item.time);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private TextView text(ViewGroup parent, int color, int sizeSp) {
        TextView view = new TextView(parent.getContext());
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        return view;
    }

    private int dp(ViewGroup parent, int value) {
        return UiKit.dp(parent.getContext(), value);
    }

    static final class MailViewHolder extends RecyclerView.ViewHolder {
        final TextView sender;
        final TextView subject;
        final TextView preview;
        final TextView time;

        MailViewHolder(@NonNull View itemView, TextView sender, TextView subject, TextView preview, TextView time) {
            super(itemView);
            this.sender = sender;
            this.subject = subject;
            this.preview = preview;
            this.time = time;
        }
    }
}
