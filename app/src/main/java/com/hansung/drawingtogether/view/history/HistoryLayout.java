package com.hansung.drawingtogether.view.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.hansung.drawingtogether.R;

public class HistoryLayout extends LinearLayout {

    private ImageView image;
    private TextView topic;
    private TextView time;
    private ImageView more;

    public HistoryLayout(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_history, this, true);
        image = (ImageView)findViewById(R.id.history_thumbnail);
        topic = (TextView)findViewById(R.id.history_topic);
        time = (TextView)findViewById(R.id.history_time);
        more = (ImageView)findViewById(R.id.history_more);
        more.setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v) {
                PopupMenu pop = new PopupMenu(getContext(), v);
                pop.getMenuInflater().inflate(R.menu.history_menu, pop.getMenu());
                pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getTitle().toString()) {
                            case "드로잉":
                                break;
                            case "삭제":
                                break;
                        }
                        return true;
                    }
                });
                pop.show();
            }
        });
    }

    public void setData(HistoryData data) {
        image.setImageResource(data.getImage());
        topic.setText(data.getTopic());
        time.setText(data.getTime());
    }
}
