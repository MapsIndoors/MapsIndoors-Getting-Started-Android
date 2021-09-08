package com.example.mapsindoorsgettingstarted.PositionProviders;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.internal.LinkedTreeMap;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.mapsindoors.mapssdk.Floor;
import com.mapsindoors.mapssdk.MPPositionResult;
import com.mapsindoors.mapssdk.OnPositionUpdateListener;
import com.mapsindoors.mapssdk.OnStateChangedListener;
import com.mapsindoors.mapssdk.PermissionsAndPSListener;
import com.mapsindoors.mapssdk.Point;
import com.mapsindoors.mapssdk.PositionProvider;
import com.mapsindoors.mapssdk.PositionResult;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndoorAtlasPositionProvider implements PositionProvider {

    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH"
    };

    private boolean mIsIPSEnabled = false;
    private boolean mIsRunning = false;
    protected final List<OnStateChangedListener> onStateChangedListenersList = new ArrayList<>();
    protected final List<OnPositionUpdateListener> onPositionUpdateListeners = new ArrayList<>();
    protected String mProviderId;
    protected Context mContext;
    protected PositionResult mLatestPosition;

    protected boolean mCanDeliver;

    private static final long MIN_TIME_BETWEEN_UPDATES_IN_MS = 100;

    private IALocationManager mIndoorAtlasClient;

    private long mLastHeadingUpdateTime;
    private long mLastOrientationUpdateTime;
    private float mLatestBearing;

    private Map<Integer, Integer> mFloorMapping;

    private Map<String, Object> mConfig;

    public IndoorAtlasPositionProvider(@NonNull Context context, Map<String, Object> config) {
        mContext = context;
        mConfig = config;
    }

    private void initClient(){
        String apiKey = (String) mConfig.get("key");
        String secret = (String) mConfig.get("secret");

        mFloorMapping = constructFloorMapping(mConfig);

        if(apiKey == null || TextUtils.isEmpty(apiKey)|| secret == null || TextUtils.isEmpty(secret) || mFloorMapping.isEmpty()){
            Log.e(this.getClass().getSimpleName(), "IndoorAtlas API key/secret is either null or empty string, or floor mapping is missing!");
            mCanDeliver = false;
        } else {
            mCanDeliver = true;
        }

        Bundle extras = new Bundle(2);
        extras.putString(IALocationManager.EXTRA_API_KEY, apiKey);
        extras.putString(IALocationManager.EXTRA_API_SECRET, secret);

        mIndoorAtlasClient = IALocationManager.create(mContext, extras);
        mIndoorAtlasClient.registerOrientationListener(new IAOrientationRequest( 1, 0 ), mOrientationListener);

        // Enable switching to GPS when outside, in the IndoorAtlas SDK
        mIndoorAtlasClient.lockIndoors(false);
    }

    /**
     * Creates an int:int map, from the floor mapping from the backend
     * @param config
     * @return
     */
    private Map<Integer, Integer> constructFloorMapping(Map<String, Object> config){
        Map<Integer, Integer> floorMapping = new HashMap<>();

        Object mappingObject = config.get("floorMapping");
        if(mappingObject != null){
            LinkedTreeMap<String, Double> map = (LinkedTreeMap<String, Double>) mappingObject;

            // Convert to int:int map
            for(Map.Entry<String, Double> entry : map.entrySet()){
                int key = Integer.parseInt(entry.getKey());
                double val = entry.getValue();
                floorMapping.put(key, (int)val);
            }
        }

        return floorMapping;
    }

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    @Override
    public boolean isPSEnabled() {
        return mIsIPSEnabled;
    }

    @Override
    public void startPositioning(@Nullable String s) {
        if(!mIsRunning){
            initClient();
            mIndoorAtlasClient.requestLocationUpdates( IALocationRequest.create(), locationListener );
            mIsRunning = true;
        }
    }

    @Override
    public void stopPositioning(@Nullable String s) {
        if(mIsRunning && mIndoorAtlasClient != null) {
            mIndoorAtlasClient.removeLocationUpdates(locationListener);
            mIsRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void addOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if( onPositionUpdateListener != null ) {
            onPositionUpdateListeners.remove( onPositionUpdateListener );
            onPositionUpdateListeners.add( onPositionUpdateListener );
        }
    }

    @Override
    public void removeOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if( onPositionUpdateListener != null ) {
            onPositionUpdateListeners.remove( onPositionUpdateListener );
        }
    }

    @Override
    public void setProviderId(@Nullable String id) {
        mProviderId = id;
    }

    @Override
    public void addOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if( onStateChangedListener != null ) {
            onStateChangedListenersList.remove( onStateChangedListener );
            onStateChangedListenersList.add( onStateChangedListener );
        }
    }

    @Override
    public void removeOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if( onStateChangedListener != null ) {
            onStateChangedListenersList.remove( onStateChangedListener );
        }
    }

    @Override
    public void checkPermissionsAndPSEnabled(@Nullable PermissionsAndPSListener permissionsAndPSListener) {
        PSUtils.checkLocationPermissionAndServicesEnabled( getRequiredPermissions(), mContext, permissionsAndPSListener );
    }

    @Nullable
    @Override
    public String getProviderId() {
        return mProviderId;
    }

    @Nullable
    @Override
    public PositionResult getLatestPosition() {
        return mLatestPosition;
    }

    @Override
    public void startPositioningAfter(int i, @Nullable String s) {
        //Not used
    }

    @Override
    public void terminate() {

    }

    private IAOrientationListener mOrientationListener = new IAOrientationListener() {
        @Override
        public void onHeadingChanged( long timestamp, double heading ) {
            if( mLatestPosition != null ) {
                final long dt = timestamp - mLastHeadingUpdateTime;

                if( dt < MIN_TIME_BETWEEN_UPDATES_IN_MS ) {
                    return;
                }

                mLastHeadingUpdateTime = timestamp;

                final float bearing = (float) heading;

                mLatestPosition.setBearing( bearing );
                mLatestBearing = bearing;

                reportPositionUpdate();
            }
        }

        @Override
        public void onOrientationChange( long timestamp, @Nullable double[] quaternion ) {
            if( mLatestPosition != null ) {
                final long dt = timestamp - mLastOrientationUpdateTime;

                if( dt < MIN_TIME_BETWEEN_UPDATES_IN_MS ) {
                    return;
                }

                mLastOrientationUpdateTime = timestamp;
            }
        }
    };

    /**
     * Reports to listeners, upon new positioning
     */
    public void reportPositionUpdate() {
        if(mIsRunning){
            for(OnPositionUpdateListener listener : onPositionUpdateListeners){
                if(listener != null && mLatestPosition != null){
                    listener.onPositionUpdate(mLatestPosition);
                }
            }
        }
    }

    private final IALocationListener locationListener = new IALocationListener() {
        @Override
        public void onLocationChanged( @Nullable final IALocation location ) {
            final double latitude = location.getLatitude();
            final double longitude = location.getLongitude();
            final int floorLevel = location.getFloorLevel();
            final float accuracy = location.getAccuracy();

            final boolean hasFloorLevel = location.hasFloorLevel();

            if( isRunning() ) {
                mIsIPSEnabled = true;

                final MPPositionResult newLocation = new MPPositionResult( new Point( latitude, longitude ), accuracy, mLatestBearing);
                newLocation.setAndroidLocation( location.toLocation() );
                mLatestPosition = newLocation;

                if( hasFloorLevel ) {
                    final int miFloorIndex;

                    if( mFloorMapping.containsKey(floorLevel) ) {
                        miFloorIndex = mFloorMapping.get(floorLevel);
                    } else {
                        miFloorIndex = Floor.DEFAULT_GROUND_FLOOR_INDEX;
                    }

                    mLatestPosition.setFloor( miFloorIndex );
                } else {
                    mLatestPosition.setFloor( Floor.DEFAULT_GROUND_FLOOR_INDEX );
                }

                mLatestPosition.setProvider( IndoorAtlasPositionProvider.this );
                reportPositionUpdate();
            }
        }

        @Override
        public void onStatusChanged(@Nullable final String provider, final int status, @Nullable final Bundle extras ) {

        }
    };
}
