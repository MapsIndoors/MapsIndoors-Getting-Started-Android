package com.example.mapsindoorsgettingstarted;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.mapsindoors.mapssdk.MPRoute;

import java.util.concurrent.TimeUnit;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     NavigationFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class NavigationFragment extends Fragment {
    private MPRoute mRoute;
    private MapsActivity mMapsActivity;

    public static NavigationFragment newInstance(MPRoute route, MapsActivity mapsActivity) {
        final NavigationFragment fragment = new NavigationFragment();
        fragment.mRoute = route;
        fragment.mMapsActivity = mapsActivity;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RouteCollectionAdapter routeCollectionAdapter = new RouteCollectionAdapter(this);
        ViewPager2 mViewPager = view.findViewById(R.id.view_pager);
        mViewPager.setAdapter(routeCollectionAdapter);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //When a page is selected call the renderer with the index
                mMapsActivity.getMpDirectionsRenderer().setRouteLegIndex(position);
                //Update the floor on MapControl if the floor might have changed for the routing
                mMapsActivity.getMapControl().selectFloor(mMapsActivity.getMpDirectionsRenderer().getLegFloorIndex());
            }
        });

        //Assigning views
        TextView distanceTxtView = view.findViewById(R.id.distanceTxt);
        TextView infoTxtView = view.findViewById(R.id.infoTxt);
        ImageButton closeBtn = view.findViewById(R.id.closeBtn);
        ImageButton nextBtn = view.findViewById(R.id.arrow_next);
        ImageButton backBtn = view.findViewById(R.id.arrow_back);

        //Button for closing the bottom sheet. Clears the route through MPDirectionsRenderer as well, and changes map padding.
        closeBtn.setOnClickListener(v -> {
            mMapsActivity.removeFragmentFromBottomSheet(this);
            mMapsActivity.getMpDirectionsRenderer().clear();
        });

        //Next button for going through the legs of the route.
        nextBtn.setOnClickListener(v -> {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
        });

        //Back button for going through the legs of the route.
        backBtn.setOnClickListener(v -> {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
        });

        //Describing the distance in meters
        distanceTxtView.setText("Distance: " + mRoute.getDistance() + " m");
        //Describing the time it takes for the route in minutes
        infoTxtView.setText("Time for route: " + TimeUnit.MINUTES.convert(mRoute.getDuration(), TimeUnit.SECONDS) + " minutes");
    }

    class RouteCollectionAdapter extends FragmentStateAdapter {

        public RouteCollectionAdapter(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return RouteLegFragment.newInstance(mRoute.getLegs().get(position));
        }

        @Override
        public int getItemCount() {
            return mRoute.getLegs().size();
        }
    }
}