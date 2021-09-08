package com.example.mapsindoorsgettingstarted.PositionProviders;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.mapsindoors.mapssdk.LocationDisplayRule;
import com.mapsindoors.mapssdk.MapControl;
import com.mapsindoors.mapssdk.MapsIndoors;
import com.mapsindoors.mapssdk.OnPositionUpdateListener;
import com.mapsindoors.mapssdk.PermissionsAndPSListener;
import com.mapsindoors.mapssdk.PositionProvider;
import com.mapsindoors.mapssdk.PositionResult;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PositionProviderService {
    private MapControl mMapControl;
    private Activity mActivity;

    private PositionProvider mCiscoDNAPositionProvider;
    private PositionProvider mIndoorAtlasPositionProvider;
    private PositionProvider mGooglePositioningProvider;

    public PositionProviderService(Activity activity, MapControl mapControl) {
        mMapControl = mapControl;
        mActivity = activity;
    }

    public void setupCiscoPositioning(){
        Map<String, Object> ciscoDnaConfig = MapsIndoors.getSolution().getPositionProviderConfig().get("ciscodna");
        String tenantId = (String) ciscoDnaConfig.get("ciscoDnaSpaceTenantId");

        if(tenantId == null || tenantId.isEmpty()){
            // Cannot setup CiscoDNA positioning in this case
            return;
        }

        mCiscoDNAPositionProvider = new CiscoDNAPositionProvider(mActivity, tenantId);
        MapsIndoors.setPositionProvider(mCiscoDNAPositionProvider);
        MapsIndoors.startPositioning();
        mMapControl.showUserPosition(true);

        mCiscoDNAPositionProvider.addOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositioningStarted(@NonNull @NotNull PositionProvider positionProvider) { }

            @Override
            public void onPositionFailed(@NonNull @NotNull PositionProvider positionProvider) { }

            @Override
            public void onPositionUpdate(@NonNull @NotNull PositionResult positionResult) {
                mActivity.runOnUiThread(() -> {
                    mMapControl.getPositionIndicator().setIconFromDisplayRule( new LocationDisplayRule.Builder( "BlueDotRule" )
                            .setVectorDrawableIcon(android.R.drawable.presence_invisible, 23, 23 )
                            .setTint(Color.BLUE)
                            .setShowLabel(true)
                            .setLabel("You")
                            .setLabel(null)
                            .build());
                });
            }
        });
    }

    public void setupIndoorAtlasPositioning() {
        mActivity.runOnUiThread(()-> {
            Map<String, Object> indoorAtlasConfig = MapsIndoors.getSolution().getPositionProviderConfig().get("indooratlas3");
            mIndoorAtlasPositionProvider = new IndoorAtlasPositionProvider(mActivity, indoorAtlasConfig);
            mIndoorAtlasPositionProvider.checkPermissionsAndPSEnabled(new PermissionsAndPSListener() {
                @Override
                public void onPermissionDenied() { }

                @Override
                public void onPermissionGranted() {
                    onIndoorAtlasPermissionsGiven();
                }

                @Override
                public void onGPSPermissionAndServiceEnabled() { }

                @Override
                public void onPermissionRequestError() { }
            });
        });
    }

    void onIndoorAtlasPermissionsGiven() {
        MapsIndoors.setPositionProvider(mIndoorAtlasPositionProvider);
        MapsIndoors.startPositioning();
        mMapControl.showUserPosition(true);

        mIndoorAtlasPositionProvider.addOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositioningStarted(@NonNull @NotNull PositionProvider positionProvider) { }

            @Override
            public void onPositionFailed(@NonNull @NotNull PositionProvider positionProvider) { }

            @Override
            public void onPositionUpdate(@NonNull @NotNull PositionResult positionResult) {
                mActivity.runOnUiThread(() -> {
                    mMapControl.getPositionIndicator().setIconFromDisplayRule( new LocationDisplayRule.Builder( "BlueDotRule" )
                            .setVectorDrawableIcon(android.R.drawable.presence_invisible, 23, 23 )
                            .setTint(Color.BLUE)
                            .setShowLabel(true)
                            .setLabel("You")
                            .setLabel(null)
                            .build());
                });
            }
        });
    }

    public void setupGooglePositioning() {
        mGooglePositioningProvider = new GPSPositionProvider(mActivity);
        mGooglePositioningProvider.checkPermissionsAndPSEnabled(new PermissionsAndPSListener() {
            @Override
            public void onPermissionDenied() { }

            @Override
            public void onPermissionGranted() { }

            @Override
            public void onGPSPermissionAndServiceEnabled() {
                onGooglePositioningPermissionsGiven();
            }

            @Override
            public void onPermissionRequestError() { }
        });
    }

    void onGooglePositioningPermissionsGiven() {
        MapsIndoors.setPositionProvider(mGooglePositioningProvider);
        MapsIndoors.startPositioning();
        mMapControl.showUserPosition(true);

        mGooglePositioningProvider.addOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositioningStarted(@NonNull @NotNull PositionProvider positionProvider) {

            }

            @Override
            public void onPositionFailed(@NonNull @NotNull PositionProvider positionProvider) {

            }

            @Override
            public void onPositionUpdate(@NonNull @NotNull PositionResult positionResult) {
                mActivity.runOnUiThread(() -> {
                    mMapControl.getPositionIndicator().setIconFromDisplayRule( new LocationDisplayRule.Builder( "BlueDotRule" )
                            .setVectorDrawableIcon(android.R.drawable.presence_invisible, 23, 23 )
                            .setTint(Color.BLUE)
                            .setShowLabel(true)
                            .setLabel("You")
                            .setLabel(null)
                            .build());
                });
            }
        });
    }
}
