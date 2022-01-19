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
// We will reuse the ViewHolder created in the search experience as it can hold the same information we want to show here
public class MenuItemAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<MenuInfo> mMenuInfos;
    private final MapsActivity mMapActivity;

    MenuItemAdapter(List<MenuInfo> menuInfoList, MapsActivity activity) {
        mMenuInfos = menuInfoList;
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

        // When a category is selected, we want to filter the map s.t. it only shows the locations in that
        // category
        holder.itemView.setOnClickListener(view -> {
            // empty query, we do not need to query anything specific
            MPQuery query = new MPQuery.Builder().build();
            // filter created on the selected category key
            MPFilter filter = new MPFilter.Builder().setCategories(Collections.singletonList(mMenuInfos.get(position).getCategoryKey())).build();
            MapsIndoors.getLocationsAsync(query, filter, (locations, error) -> {
                if (error == null && locations != null) {
                    mMapActivity.getMapControl().displaySearchResults(locations);
                }
            });
        });

        // if there exists an icon for this menuItem, then we will use it
        if (mMenuInfos.get(position).getIconUrl() != null) {
            // As we need to download the image, it has to be offloaded from the main thread
            new Thread(() -> {
                Bitmap image;
                try {
                    // Usually we would not want to re-download the image every time, but that is not important for this guide
                    URL url = new URL(mMenuInfos.get(position).getIconUrl());
                    image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch(IOException ignored) {
                    return;
                }
                //Set the image while on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    holder.imageView.setImageBitmap(image);
                });

            }).start();
        }


    }

    @Override
    public int getItemCount() {
        return mMenuInfos.size();
    }

}
