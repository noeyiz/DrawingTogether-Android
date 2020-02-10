package com.hansung.drawingtogether.view.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.databinding.LayoutHistoryBinding;

import java.util.ArrayList;

public class HistoryAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HistoryData> historyData = new ArrayList<>();
    private HistoryViewModel historyViewModel;

    public HistoryAdapter(Context context, HistoryViewModel historyViewModel) {
        this.context = context;
        this.historyViewModel = historyViewModel;
    }

    @Override
    public int getCount() {
        return historyData.size();
    }

    @Override
    public Object getItem(int position) {
        return historyData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setData(ArrayList<HistoryData> historyData) {
        this.historyData = historyData;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        HistoryViewHolder holder;

        if (view == null) {
            LayoutHistoryBinding binding = LayoutHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            view = binding.getRoot();
            holder = new HistoryViewHolder(binding);
            view.setTag(holder);
        }
        else {
            holder = (HistoryViewHolder) view.getTag();
        }

        holder.bind(historyData.get(position));

        return view;
    }

    class HistoryViewHolder {
        private LayoutHistoryBinding binding;

        public HistoryViewHolder(LayoutHistoryBinding binding) {
            this.binding = binding;
        }

        public void bind(HistoryData data) {
            binding.setData(data);
            binding.setVm(historyViewModel);
            binding.historyMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    PopupMenu pop = new PopupMenu(context, v);
                    pop.getMenuInflater().inflate(R.menu.history_menu, pop.getMenu());
                    pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId()) {
                                case R.id.drawing:
                                    historyViewModel.clickDrawing(v);
                                    break;
                                case R.id.delete:
                                    //
                                    break;
                            }
                            return true;
                        }
                    });
                    pop.show();
                }
            });
        }
    }
}
