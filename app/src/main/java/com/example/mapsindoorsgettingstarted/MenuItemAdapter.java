package com.example.mapsindoorsgettingstarted;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapsindoors.mapssdk.MPFilter;
import com.mapsindoors.mapssdk.MPQuery;
import com.mapsindoors.mapssdk.MapsIndoors;
import com.mapsindoors.mapssdk.MenuInfo;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class MenuItemAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<MenuInfo> mMenuInfos;
    private final MapsActivity mMapActivity;

    MenuItemAdapter(List<MenuInfo> locationList, MapsActivity activity) {
        mMenuInfos = locationList;
        mMapActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Setting the the text on the text view to the name of the location
        holder.text.setText(mMenuInfos.get(position).getName());

        holder.itemView.setOnClickListener(view -> {
            MapsIndoors.getLocationsAsync(new MPQuery.Builder().build(),
                    new MPFilter.Builder().setCategories(Collections.singletonList(mMenuInfos.get(position).getCategoryKey())).build(),
                    (locations ,error) -> {
                if (error == null) {
                    mMapActivity.getMapControl().displaySearchResults(locations);
                }
            });
        });

        new Thread(() -> {
            Bitmap image;
            try {
                URL url = new URL(mMenuInfos.get(position).getIconUrl());
                image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch(IOException ignored) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                holder.imageView.setImageBitmap(image);
            });

        }).start();
    }

    @Override
    public int getItemCount() {
        return mMenuInfos.size();
    }

}
