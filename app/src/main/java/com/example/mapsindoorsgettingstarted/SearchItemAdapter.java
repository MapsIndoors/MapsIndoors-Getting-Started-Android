package com.example.mapsindoorsgettingstarted;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapsindoors.mapssdk.LocationDisplayRule;
import com.mapsindoors.mapssdk.MPLocation;

import java.util.List;

class SearchItemAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<MPLocation> mLocations;
    private final MapsActivity mMapActivity;

    SearchItemAdapter(List<MPLocation> locationList, MapsActivity activity) {
        mLocations = locationList;
        mMapActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.text.setText(mLocations.get(position).getName());

        holder.itemView.setOnClickListener(view -> {
            mMapActivity.createRoute(mLocations.get(position));
        });

        if (mMapActivity != null) {
            LocationDisplayRule locationDisplayRule = mMapActivity.getMapControl().getDisplayRule(mLocations.get(position));
            if (locationDisplayRule != null && locationDisplayRule.getIcon() != null) {
                mMapActivity.runOnUiThread(()-> {
                    holder.imageView.setImageBitmap(locationDisplayRule.getIcon());
                });
            }else {
                //Location does not have a special displayRule using type Display rule
                LocationDisplayRule typeDisplayRule = mMapActivity.getMapControl().getDisplayRule(mLocations.get(position).getType());
                if (typeDisplayRule != null) {
                    mMapActivity.runOnUiThread(()-> {
                        holder.imageView.setImageBitmap(typeDisplayRule.getIcon());
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mLocations.size();
    }

}

class ViewHolder extends RecyclerView.ViewHolder {

    final TextView text;
    final ImageView imageView;

    ViewHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.fragment_search__list_item, parent, false));
        text = itemView.findViewById(R.id.text);
        imageView = itemView.findViewById(R.id.location_image);
    }
}