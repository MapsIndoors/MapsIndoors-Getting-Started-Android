package com.example.mapsindoorsgettingstarted.PositionProviders;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapsindoors.mapssdk.MPPositionResult;
import com.mapsindoors.mapssdk.OnPositionUpdateListener;
import com.mapsindoors.mapssdk.OnStateChangedListener;
import com.mapsindoors.mapssdk.PermissionsAndPSListener;
import com.mapsindoors.mapssdk.Point;
import com.mapsindoors.mapssdk.PositionProvider;
import com.mapsindoors.mapssdk.PositionResult;
import com.pointrlabs.core.management.Pointr;
import com.pointrlabs.core.management.PositionManager;
import com.pointrlabs.core.management.interfaces.PointrListener;
import com.pointrlabs.core.management.models.Level;
import com.pointrlabs.core.management.models.LicenseKeyMap;
import com.pointrlabs.core.management.models.PointrEnvironment;
import com.pointrlabs.core.nativecore.wrappers.Plog;
import com.pointrlabs.core.positioning.model.CalculatedLocation;
import com.pointrlabs.core.positioning.model.PositioningTypes;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mapsindoors.mapssdk.Floor.DEFAULT_GROUND_FLOOR_INDEX;

public class PointrPositionProvider implements PositionProvider {

    static final String TAG = PointrPositionProvider.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSIONS = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};

    private Pointr mPointr;
    private PositionResult mLatestPosition;
    private CalculatedLocation mLatestPointrPosition;

    private boolean mIsRunning;
    private boolean mIsIPSEnabled;

    private String mProviderId = "pointr";

    private final List<OnStateChangedListener> mOnStateChangedListenersList = new ArrayList<>();
    private final List<OnPositionUpdateListener> mOnPositionUpdateListeners = new ArrayList<>();

    public PointrPositionProvider(Context context, String pointrLicenseKey){
        PSUtils.checkLocationPermissionAndServicesEnabled(getRequiredPermissions(), context, new PermissionsAndPSListener() {
            @Override
            public void onPermissionDenied() {

            }

            @Override
            public void onPermissionGranted() {
                setupPointr(context, pointrLicenseKey);
            }

            @Override
            public void onGPSPermissionAndServiceEnabled() {

            }

            @Override
            public void onPermissionRequestError() {

            }
        });
    }

    private void setupPointr(Context context, String licenseKey){
        final Map<PointrEnvironment, String> licenseKeyMap = new ArrayMap<>();
        licenseKeyMap.put(PointrEnvironment.PROD, licenseKey);
        final LicenseKeyMap licenseKeyMapInstance = new LicenseKeyMap(licenseKeyMap);
        Pointr.with(context, licenseKeyMapInstance, PointrEnvironment.PROD, Plog.LogLevel.WARNING);
        mPointr = Pointr.getPointr();
        mPointr.addListener(mPointerStateListener);
        mPointr.start();
    }

    private PointrListener mPointerStateListener = new PointrListener() {
        @Override
        public void onStateUpdated(Pointr.State state) {
            Log.d(TAG, state.toString());

            if(mPointr == null || mPointr.getPositionManager() == null){
                return;
            }

            switch (state){
                case RUNNING:
                    mPointr.getPositionManager().addListener(mPointrListener);
                    break;
                case OFF:
                case FAILED_VALIDATION:
                case FAILED_REGISTRATION:
                    mPointr.getPositionManager().removeListener(mPointrListener);
                    break;
                default:
                    break;
            }
        }
    };

    private PositionManager.Listener mPointrListener = new PositionManager.Listener() {
        @Override
        public void onPositionManagerCalculatedLocation(CalculatedLocation calculatedLocation) {
            handlePosition(calculatedLocation);
        }

        @Override
        public void onPositionManagerDetectedPositionLevelChange(@NonNull @NotNull Level level) {
            Log.d(TAG, "Level changed detected : " + level.getIndex());
        }

        @Override
        public void onPositionManagerPositionIsFading() {
            Log.d(TAG, "Position fading");
        }

        @Override
        public void onPositionManagerPositionIsLost() {
            Log.d(TAG, "Position lost");
        }

        @Override
        public void onPositionManagerPositioningServiceStateChangedTo(PositioningTypes.PositioningServiceState positioningServiceState) {
            Log.d(TAG, "Position service state changed to: " + positioningServiceState.name());
        }
    };

    private void handlePosition(CalculatedLocation pointrLocation){
        double lat = pointrLocation.getLatitude();
        double lng = pointrLocation.getLongitude();
        float accuracy = pointrLocation.getAccuracy();
        float bearing = (float) Math.toDegrees(pointrLocation.getOrientation());
        int level = DEFAULT_GROUND_FLOOR_INDEX;
        if(pointrLocation.getLevel() != null){
            level = pointrLocation.getLevel().getIndex();
        }

        // Convert Pointr floor index to MapsIndoors floor index
        // Pointr floor 1 -> MapsIndoors floor 10
        // Pointr floor 2 -> MapsIndoors floor 20
        // Pointr floor -1 -> MapsIndoors floor -10
        level = level * 10;

        // If Pointr's accuracy class is LOW, the bearing reading is uselss (-999999.0 value), so we just show a bearing-less bluedot
        if(pointrLocation.getOrientationAccuracyClass() == CalculatedLocation.OrientationAccuracyClass.HIGH){
            mLatestPosition = new MPPositionResult(new Point(lat, lng), accuracy, bearing, level);
        } else {
            mLatestPosition = new MPPositionResult(new Point(lat, lng), accuracy, level);
        }

        mLatestPointrPosition = pointrLocation.copy();

        Log.d(TAG, "Pointr position: " + lat + " \t " + lng);

        for(OnPositionUpdateListener listener : mOnPositionUpdateListeners){
            listener.onPositionUpdate(mLatestPosition);
        }
    }

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    @Override
    public boolean isPSEnabled() {
        return true;
    }

    @Override
    public void startPositioning(@Nullable String s) {
        mPointr.start();
        mIsRunning = true;
        mIsIPSEnabled = true;
    }

    @Override
    public void stopPositioning(@Nullable String s) {
        mPointr.stop();
        mIsRunning = false;
        mIsIPSEnabled = false;
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void addOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if( onPositionUpdateListener != null ) {
            mOnPositionUpdateListeners.remove( onPositionUpdateListener );
            mOnPositionUpdateListeners.add( onPositionUpdateListener );
        }
    }

    @Override
    public void removeOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if( onPositionUpdateListener != null ) {
            mOnPositionUpdateListeners.remove( onPositionUpdateListener );
        }
    }

    @Override
    public void setProviderId(@Nullable String s) {
        mProviderId = s;
    }

    @Override
    public void addOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if( onStateChangedListener != null ) {
            mOnStateChangedListenersList.remove( onStateChangedListener );
            mOnStateChangedListenersList.add( onStateChangedListener );
        }
    }

    @Override
    public void removeOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if( onStateChangedListener != null ) {
            mOnStateChangedListenersList.remove( onStateChangedListener );
        }
    }

    @Override
    public void checkPermissionsAndPSEnabled(@Nullable PermissionsAndPSListener permissionsAndPSListener) {
        // Do locations permissions check here...
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
}
