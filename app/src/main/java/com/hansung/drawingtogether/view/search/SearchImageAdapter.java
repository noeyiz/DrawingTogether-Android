package com.hansung.drawingtogether.view.search;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

import com.bumptech.glide.Glide;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.ImageSearchVO;
import com.hansung.drawingtogether.databinding.DialogSearchImageBinding;

public class SearchImageAdapter extends BaseAdapter {

    private ArrayList<ImageSearchVO> imageList = new ArrayList<>();
    private Context context;

    public SearchImageAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return imageList.size();
    }

    @Override
    public Object getItem(int position) {
        return imageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setData(ArrayList<ImageSearchVO> imageList) {
        this.imageList = imageList;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            DialogSearchImageBinding binding = DialogSearchImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(500, 500));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView)convertView;
        }
        Glide.with(context).load(imageList.get(position).thumbnail_url).into(imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogImage = (View) View.inflate(context, R.layout.dialog_search_image, null);
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                ImageView imageUrl = (ImageView) dialogImage.findViewById(R.id.search_image_url);
                Glide.with(context).load(imageList.get(position).image_url).into(imageUrl);
                dialog.setTitle("이미지");
                dialog.setView(dialogImage);
                dialog.setNegativeButton("닫기", null);
                dialog.show();
            }
        });

        return imageView;
    }
}
