package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hansung.drawingtogether.databinding.ViewSearchMapBinding;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

public class SearchMapView extends Fragment {

    private String placename;
    private double longtitude;
    private double latitude;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewSearchMapBinding binding = ViewSearchMapBinding.inflate(inflater, container, false);

        placename = getArguments().getString("placename");
        longtitude = Double.parseDouble(getArguments().getString("longtitude"));
        latitude = Double.parseDouble(getArguments().getString("latitude"));

        // MapView 띄우기
        final MapView mapView = new MapView(getContext());
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longtitude), true);

        MapPOIItem marker = new MapPOIItem();
        marker.setItemName(placename);
        marker.setTag(0);
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longtitude));
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);

        mapView.addPOIItem(marker);
        binding.searchMapView.addView(mapView);

        return binding.getRoot();
    }
}
