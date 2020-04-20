package com.hansung.drawingtogether.view.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hansung.drawingtogether.data.remote.model.LocationVO;
import com.hansung.drawingtogether.databinding.LayoutSearchMapBinding;

import java.util.ArrayList;

public class SearchMapAdapter extends RecyclerView.Adapter<SearchMapAdapter.ViewHolder> {

    private SearchViewModel searchViewModel;
    private ArrayList<LocationVO> locationList;
    private Context context;

    public SearchMapAdapter(Context context, SearchViewModel searchViewModel) {
        this.context = context;
        this.searchViewModel = searchViewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutSearchMapBinding binding = LayoutSearchMapBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        View view = binding.getRoot();
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.setBinding(binding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(locationList.get(position));
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    public void setData(ArrayList<LocationVO> locationList) {
        this.locationList = locationList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private LayoutSearchMapBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // data 담아 drawingFragment 이동
                    Bundle bundle = new Bundle();
                    bundle.putString("placename", locationList.get(getAdapterPosition()).place_name);
                    bundle.putString("longtitude", locationList.get(getAdapterPosition()).x);
                    bundle.putString("latitude", locationList.get(getAdapterPosition()).y);
                    searchViewModel.callFragment(context, new SearchMapView(), bundle);
                }
            });
        }

        public void setBinding(LayoutSearchMapBinding binding) {
            this.binding = binding;
        }

        public void bind(LocationVO locationVO) {
            binding.setName(locationVO.road_address_name);
            binding.setAddress(locationVO.place_name);
        }
    }
}
