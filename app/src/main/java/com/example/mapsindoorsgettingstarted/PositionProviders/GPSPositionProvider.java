package com.example.mapsindoorsgettingstarted.PositionProviders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.mapsindoors.mapssdk.Floor;
import com.mapsindoors.mapssdk.MPPositionResult;
import com.mapsindoors.mapssdk.OnPositionUpdateListener;
import com.mapsindoors.mapssdk.OnStateChangedListener;
import com.mapsindoors.mapssdk.PermissionsAndPSListener;
import com.mapsindoors.mapssdk.Point;
import com.mapsindoors.mapssdk.PositionProvider;
import com.mapsindoors.mapssdk.PositionResult;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class GPSPositionProvider implements PositionProvider {

    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
    };

    private boolean mIsRunning;
    private boolean mIsEnabled;

    protected final List<OnStateChangedListener> onStateChangedListenersList = new ArrayList<>();
    protected final List<OnPositionUpdateListener> onPositionUpdateListeners = new ArrayList<>();
    protected String mProviderId;
    protected Context mContext;
    protected PositionResult mLatestPosition;

    private FusedLocationProviderClient fusedLocationClient;

    public GPSPositionProvider(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    @Override
    public boolean isPSEnabled() {
        return mIsEnabled;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void startPositioning(@Nullable String args) {
        mIsRunning = true;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    public void stopPositioning(@Nullable String args) {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        mIsRunning = false;
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

    }

    @Override
    public void terminate() {

    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull @NotNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            onLocationChanged(locationResult.getLastLocation());
        }

        @Override
        public void onLocationAvailability(@NonNull @NotNull LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }
    };

    final void onLocationChanged( @Nullable final Location location )
    {
        if( location == null ) {
            mLatestPosition = null;
            mIsEnabled = false;
            return;
        }

        if( mIsRunning ) {
            mIsEnabled = true;

            final android.location.Location recLocation = new Location( location );

            final MPPositionResult newLocation = new MPPositionResult( new Point( recLocation ), recLocation.getAccuracy() );

            newLocation.setAndroidLocation( recLocation );

            // From Google's Santa tracker:
            // "Update our current location only if we've moved at least a metre, to avoid
            // jitter due to lack of accuracy in FusedLocationApi"
            if( mLatestPosition != null ) {

                final Point prevPoint = mLatestPosition.getPoint();
                final Point newPoint = newLocation.getPoint();

                if( (prevPoint != null) && (newPoint != null) )
                {
                    // Check the distance between the prev and new position in 2D (lat/lng)
                    final double dist = prevPoint.distanceTo( newPoint );
                    if( dist <= 1.0 ) {

                        // Get the altitude too. Just imagine the lady/guy is using a lift/elevator/"spiral staircase"...
                        // Use the prev position "android location object" altitude value to run the check
                        final android.location.Location prevLocation = mLatestPosition.getAndroidLocation();
                        if( prevLocation != null ) {
                            final double altDiff = Math.abs( recLocation.getAltitude() - prevLocation.getAltitude() );
                            if( altDiff <= 2.0 ) {
                                return;
                            }
                        }
                    }
                }
            }

            // GPS always gives the ground level
            newLocation.setFloor( Floor.DEFAULT_GROUND_FLOOR_INDEX );

            mLatestPosition = newLocation;
            mLatestPosition.setProvider( this );
            mLatestPosition.setAndroidLocation( recLocation );

            //setLatestPosition(mLatestPosition);
            reportPositionUpdate();
        }
    }

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
}
