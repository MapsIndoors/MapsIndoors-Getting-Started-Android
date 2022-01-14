package com.example.mapsindoorsgettingstarted;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapsindoors.mapssdk.MapsIndoors;
import com.mapsindoors.mapssdk.MenuInfo;

import java.util.List;

public class MenuFragment extends Fragment {


    private List<MenuInfo> mMenuInfos = null;
    private MapsActivity mMapActivity = null;

    public static MenuFragment newInstance(List<MenuInfo>  menuInfos, MapsActivity mapsActivity) {
        final MenuFragment fragment = new MenuFragment();
        fragment.mMenuInfos = menuInfos;
        fragment.mMapActivity = mapsActivity;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // For the brevity of this guide, we will reuse the bottom sheet used in the searchFragment
        return inflater.inflate(R.layout.fragment_search_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new MenuItemAdapter(mMenuInfos, mMapActivity));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        // When we close the menu fragment we want to display all locations again, not just whichever were selected last
        mMapActivity.getMapControl().clearMap();
        super.onDestroyView();
    }
}
