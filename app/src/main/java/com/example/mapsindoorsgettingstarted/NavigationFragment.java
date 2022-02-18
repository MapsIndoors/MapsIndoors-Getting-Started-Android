package com.example.mapsindoorsgettingstarted;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.mapsindoors.mapssdk.Building;
import com.mapsindoors.mapssdk.Highway;
import com.mapsindoors.mapssdk.MPLocation;
import com.mapsindoors.mapssdk.MapsIndoors;
import com.mapsindoors.mapssdk.Route;
import com.mapsindoors.mapssdk.RouteLeg;
import com.mapsindoors.mapssdk.RouteStep;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.text.format.DateUtils;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     NavigationFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class NavigationFragment extends Fragment {
    private Route mRoute;
    private MapsActivity mMapsActivity;
    private MPLocation mLocation;

    public static NavigationFragment newInstance(Route route, MapsActivity mapsActivity, MPLocation location) {
        final NavigationFragment fragment = new NavigationFragment();
        fragment.mRoute = route;
        fragment.mMapsActivity = mapsActivity;
        fragment.mLocation = location;
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
        TextView locationNameTxtView = view.findViewById(R.id.location_name);
        locationNameTxtView.setText("To " + mLocation.getName());

        RouteCollectionAdapter routeCollectionAdapter = new RouteCollectionAdapter(this);
        ViewPager2 mViewPager = view.findViewById(R.id.stepViewPager);
        mViewPager.setAdapter(routeCollectionAdapter);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //When a page is selected call the renderer with the index
                mMapsActivity.getMpDirectionsRenderer().setRouteLegIndex(position);
                //Update the floor on mapcontrol if the floor might have changed for the routing
                mMapsActivity.getMapControl().selectFloor(mMapsActivity.getMpDirectionsRenderer().getCurrentFloor());
            }
        });

        ImageView closeBtn = view.findViewById(R.id.close_btn);
        //Button for closing the bottom sheet. Clears the route through directionsRenderer as well, and changes map padding.
        closeBtn.setOnClickListener(v -> {
            mMapsActivity.removeFragmentFromBottomSheet(this);
            mMapsActivity.getMpDirectionsRenderer().clear();
        });
    }

    String getStepName(RouteStep startStep, RouteStep endStep) {
        double startStepZindex = startStep.getStartLocation().getZIndex();
        String startStepFloorName = startStep.getStartLocation().getFloorName();
        String highway = null;

        for (String actionName : getActionNames()) {
            if (startStep.getHighway().equals(actionName)) {
                if (actionName.equals(Highway.STEPS)) {
                    highway = "stairs";
                }else {
                    highway = actionName;
                }
            }
        }

        if (highway != null) {
            return String.format("Take %s to %s %s", highway, "level", endStep.getEndLocation().getFloorName().isEmpty() ? endStep.getEndLocation().getZIndex(): endStep.getEndLocation().getFloorName());
        }

        if (startStepFloorName.equals(endStep.getEndLocation().getFloorName())) {
            return "Walk to next step";
        }

        String endStepFloorName = endStep.getEndLocation().getFloorName();

        if (endStepFloorName.isEmpty()) {
            return String.format("Level %s to %s", startStepFloorName.isEmpty() ? startStepZindex: startStepFloorName, endStep.getEndPoint().getZIndex());
        }else {
            return String.format("Level %s to %s", startStepFloorName.isEmpty() ? startStepZindex: startStepFloorName, endStepFloorName);
        }
    }

    ArrayList<String> getActionNames() {
        ArrayList<String> actionNames = new ArrayList<>();
        actionNames.add(Highway.ELEVATOR);
        actionNames.add(Highway.ESCALATOR);
        actionNames.add(Highway.STEPS);
        actionNames.add(Highway.TRAVELATOR);
        actionNames.add(Highway.RAMP);
        actionNames.add(Highway.WHEELCHAIRLIFT);
        actionNames.add(Highway.WHEELCHAIRRAMP);
        actionNames.add(Highway.LADDER);
        return actionNames;
    }

    class RouteCollectionAdapter extends FragmentStateAdapter {

        public RouteCollectionAdapter(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == mRoute.getLegs().size() - 1) {
                return RouteLegFragment.newInstance("Walk to " + mLocation.getName(), (int) mRoute.getLegs().get(position).getDistance(), (int) mRoute.getLegs().get(position).getDuration());
            }else {
                RouteLeg leg = mRoute.getLegs().get(position);
                RouteStep firstStep = leg.getSteps().get(0);
                RouteStep lastFirstStep = mRoute.getLegs().get(position+1).getSteps().get(0);
                RouteStep lastStep = mRoute.getLegs().get(position+1).getSteps().get(mRoute.getLegs().get(position+1).getSteps().size()-1);

                Building firstBuilding = MapsIndoors.getBuildings().getBuilding(firstStep.getStartPoint().getLatLng());
                Building lastBuilding = MapsIndoors.getBuildings().getBuilding(lastStep.getStartPoint().getLatLng());

                if (firstBuilding != null && lastBuilding != null) {
                    return RouteLegFragment.newInstance(getStepName(lastFirstStep, lastStep), (int) leg.getDistance(), (int) leg.getDuration());
                }else if (firstBuilding != null) {
                    return RouteLegFragment.newInstance("Exit: " + firstBuilding.getName(), (int) leg.getDistance(), (int) leg.getDuration());
                }else {
                    return RouteLegFragment.newInstance("Enter: " + lastBuilding.getName(), (int) leg.getDistance(), (int) leg.getDuration());
                }
            }
        }

        @Override
        public int getItemCount() {
            return mRoute.getLegs().size();
        }
    }
}