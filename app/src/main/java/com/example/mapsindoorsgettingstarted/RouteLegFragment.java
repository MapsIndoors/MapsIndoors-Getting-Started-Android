package com.example.mapsindoorsgettingstarted;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapsindoors.mapssdk.RouteLeg;
import com.mapsindoors.mapssdk.RouteStep;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RouteLegFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RouteLegFragment extends Fragment {
    private String mStep = null;
    private int mDuration = 0;
    private int mDistance = 0;

    public static RouteLegFragment newInstance(String step, int distance, int duration) {
        RouteLegFragment fragment = new RouteLegFragment();
        fragment.mStep = step;
        fragment.mDistance = distance;
        fragment.mDuration = duration;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_leg, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView stepTextView = view.findViewById(R.id.stepTextView);
        TextView distanceTextView = view.findViewById(R.id.distanceTextView);
        TextView durationTextView = view.findViewById(R.id.durationTextView);

        stepTextView.setText(mStep);

        if (Locale.getDefault().getCountry().equals("US")) {
            distanceTextView.setText((int) Math.round(mDistance * 3.281) + " feet");
        }else {
            distanceTextView.setText(mDistance + " m");
        }

        if (mDuration < 60) {
            durationTextView.setText(mDuration + " sec");
        }else {
            durationTextView.setText(TimeUnit.MINUTES.convert(new Long(mDuration), TimeUnit.SECONDS) + " min");
        }
    }
}