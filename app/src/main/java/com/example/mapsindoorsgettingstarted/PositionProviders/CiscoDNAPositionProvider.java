package com.example.mapsindoorsgettingstarted.PositionProviders;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mapsindoors.livesdk.CiscoDNATopic;
import com.mapsindoors.livesdk.LiveDataManager;
import com.mapsindoors.mapssdk.OnPositionUpdateListener;
import com.mapsindoors.mapssdk.OnStateChangedListener;
import com.mapsindoors.mapssdk.PermissionsAndPSListener;
import com.mapsindoors.mapssdk.PositionProvider;
import com.mapsindoors.mapssdk.PositionResult;
import com.mapsindoors.mapssdk.ReadyListener;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CiscoDNAPositionProvider implements PositionProvider {

    private static final String TAG = CiscoDNAPositionProvider.class.getSimpleName();

    private boolean mIsRunning;
    private boolean mIsSubscribed;
    private boolean mIsIPSEnabled;
    private String mProviderId;

    private Context mContext;
    private String mWan;
    private String mLan;
    private String mTenantId;
    private String mCiscoDeviceId;
    private CiscoDNATopic mTopic;
    private PositionResult mLatestPosition;

    public static final String MAPSINDOORS_CISCO_ENDPOINT = "https://ciscodna.mapsindoors.com/";

    private final List<OnStateChangedListener> mOnStateChangedListenersList = new ArrayList<>();
    private final List<OnPositionUpdateListener> mOnPositionUpdateListeners = new ArrayList<>();

    public CiscoDNAPositionProvider(@NonNull Context context, @NonNull String tenantId){
        mContext = context;
        mIsRunning = false;
        mTenantId = tenantId;

        LiveDataManager.getInstance().setOnTopicUnsubscribedListener(topic -> {
            if(topic.matchesCriteria(mTopic)){
                mIsSubscribed = false;
                mCiscoDeviceId = null;
                mIsIPSEnabled = false;
            }
        });

        LiveDataManager.getInstance().setOnReceivedLiveUpdateListener((topic, message) -> {
            if(message.getId().equals(mCiscoDeviceId)){
                mLatestPosition = message.getPositionResult();
                Log.i(TAG, "Position: " + mLatestPosition.getPoint().getLat() + " , " + mLatestPosition.getPoint().getLng());

                // Report to listeners
                for(OnPositionUpdateListener listener : mOnPositionUpdateListeners){
                    listener.onPositionUpdate(mLatestPosition);
                }
            }
        });

    }

    private void startSubscription(){
        mTopic = new CiscoDNATopic(mTenantId, mCiscoDeviceId);

        if(!mIsSubscribed){
            LiveDataManager.getInstance().setOnTopicSubscribedListener(topic -> {
                if(topic.equals(mTopic)){
                    mIsIPSEnabled = true;
                    mIsSubscribed = true;
                }
            });
            LiveDataManager.getInstance().subscribeTopic(mTopic);
        }
    }

    private void unsubscribe(){
        LiveDataManager.getInstance().unsubscribeTopic(mTopic);
        mIsSubscribed = false;
    }

    private void updateAddressesAndId(ReadyListener onComplete) {
        mLan = getLocalAddress();
        fetchExternalAddress(() -> {
            if(mTenantId != null && mLan != null && mWan != null){
                String url = MAPSINDOORS_CISCO_ENDPOINT + mTenantId + "/api/ciscodna/devicelookup?clientIp=" + mLan + "&wanIp=" + mWan;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        Gson gson = new Gson();
                        String json = response.body().string();
                        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                        mCiscoDeviceId = jsonObject.get("deviceId").getAsString();
                        Log.i(TAG, "DeviceID: " + mCiscoDeviceId);
                    } else {
                        Log.i(TAG, "Could not obtain deviceId from backend deviceID request! Code: " + response.code());
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(onComplete != null){
                onComplete.onResult();
            }
        });
    }

    @Nullable
    private String getLocalAddress(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ipv4 = inetAddress.getHostAddress().toString();
                        Log.i(TAG, "LAN: " + ipv4);
                        return ipv4;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(this.getClass().getSimpleName(), "Failed to resolve LAN address");
        }

        return null;
    }

    private void fetchExternalAddress(@NonNull ReadyListener listener){
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url("https://ipinfo.io/ip").build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.onResult();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String str = response.body().string();
                    mWan = str;
                    Log.i(TAG, "WAN: " + mWan);
                }
                listener.onResult();
            }
        });
    }

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return new String[0];
    }

    @Override
    public boolean isPSEnabled() {
        return mIsIPSEnabled;
    }

    @Override
    public void startPositioning(@Nullable String s) {
        if(!mIsRunning){
            mIsRunning = true;
            updateAddressesAndId(() -> {
                if(mCiscoDeviceId != null && !mCiscoDeviceId.isEmpty()){
                    startSubscription();
                }
            });
        }
    }

    @Override
    public void stopPositioning(@Nullable String s) {
        if(mIsRunning){
            mIsRunning = false;
            if(mIsSubscribed){
                unsubscribe();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void addOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if(onPositionUpdateListener != null && !mOnPositionUpdateListeners.contains(onPositionUpdateListener)){
            mOnPositionUpdateListeners.add(onPositionUpdateListener);
        }
    }

    @Override
    public void removeOnPositionUpdateListener(@Nullable OnPositionUpdateListener onPositionUpdateListener) {
        if(onPositionUpdateListener != null){
            mOnPositionUpdateListeners.remove(onPositionUpdateListener);
        }
    }

    @Override
    public void setProviderId(@Nullable String s) {
        mProviderId = s;
    }

    @Override
    public void addOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if(onStateChangedListener != null && !mOnStateChangedListenersList.contains(onStateChangedListener)){
            mOnStateChangedListenersList.add(onStateChangedListener);
        }
    }

    @Override
    public void removeOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener) {
        if(onStateChangedListener != null){
            mOnStateChangedListenersList.remove(onStateChangedListener);
        }
    }

    @Override
    public void checkPermissionsAndPSEnabled(@Nullable PermissionsAndPSListener permissionsAndPSListener) {
        // Do locations permissions check here... this is not necessary for CiscoDNA, as it is
        // a wifi based positioning system, meaning the positioning is completely external to the
        // device itself.
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
    public void startPositioningAfter(int i, @Nullable String s) { }

    @Override
    public void terminate() { }
}
