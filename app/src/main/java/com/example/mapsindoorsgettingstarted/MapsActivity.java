package com.example.mapsindoorsgettingstarted;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.textfield.TextInputEditText;
import com.mapsindoors.mapssdk.MPDirectionsRenderer;
import com.mapsindoors.mapssdk.MPFilter;
import com.mapsindoors.mapssdk.MPLocation;
import com.mapsindoors.mapssdk.MPQuery;
import com.mapsindoors.mapssdk.MPRoutingProvider;
import com.mapsindoors.mapssdk.MapControl;
import com.mapsindoors.mapssdk.MapsIndoors;
import com.mapsindoors.mapssdk.OnRouteResultListener;
import com.mapsindoors.mapssdk.Point;
import com.mapsindoors.mapssdk.Route;
import com.mapsindoors.mapssdk.TravelMode;
import com.mapsindoors.mapssdk.Venue;
import com.mapsindoors.mapssdk.errors.MIError;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnRouteResultListener {

    private GoogleMap mMap;
    private MapControl mMapControl;
    private View mMapView;
    private ImageButton mSearchBtn;
    private TextInputEditText mSearchTxtField;
    private MPRoutingProvider mpRoutingProvider;
    private MPDirectionsRenderer mpDirectionsRenderer;
    private Point mUserLocation = new Point(38.897389429704695, -77.03740973527613,0);
    private NavigationFragment mNavigationFragment;
    private SearchFragment mSearchFragment;
    private FrameLayout bottomSheet;
    private Fragment currentFragment;
    private BottomSheetBehavior<FrameLayout> btmnSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapView = mapFragment.getView();

        //Initialize MapsIndoors and set the google api Key
        MapsIndoors.initialize(getApplicationContext(), "79f8e7daff76489dace4f9f9");
        MapsIndoors.setGoogleAPIKey(getString(R.string.google_maps_key));

        mSearchBtn = findViewById(R.id.search_btn);
        mSearchTxtField = findViewById(R.id.search_edit_txt);
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //ClickListener to start a search, when the user clicks the search button
        mSearchBtn.setOnClickListener(view -> {
            if (mSearchTxtField.getText().length() != 0) {
                //There is text inside the search field. So lets do the search.
                search(mSearchTxtField.getText().toString());
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        //Listener for when the user searches through the keyboard
        mSearchTxtField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.getText().length() != 0) {
                    //There is text inside the search field. So lets do the search.
                    search(textView.getText().toString());
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        bottomSheet = findViewById(R.id.standardBottomSheet);
        btmnSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        btmnSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (currentFragment != null) {
                        if (currentFragment instanceof NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer.clear();
                        }
                        //Clears the map if any searches has been done.
                        mMapControl.clearMap();
                        //Removes the current fragment from the BottomSheet.
                        getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
                        currentFragment = null;
                    }
                    mMapControl.setMapPadding(0,0,0,0);
                }else {
                    mMapControl.setMapPadding(0,0,0, btmnSheetBehavior.getPeekHeight());
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    /**
     * Public getter for the MapControl object
     * @return MapControl object for this activity
     */
    public MapControl getMapControl() {
        return mMapControl;
    }

    /**
     * Public getter for the
     * @return MPDirectionRenderer object for this activity
     */
    public MPDirectionsRenderer getMpDirectionsRenderer() {
        return mpDirectionsRenderer;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMapView != null) {
            initMapControl(mMapView);
        }

    }

    /**
     * Inits mapControl and sets the camera to the venue.
     * @param view the view assigned to the google map.
     */
    void initMapControl(View view) {
        //Creates a new instance of MapControl
        mMapControl = new MapControl(this);
        //Sets the Google map object and the map view to the MapControl
        mMapControl.setGoogleMap(mMap, view);
        //Initiates the MapControl
        mMapControl.init(miError -> {
            if (miError == null) {
                //No errors so getting the first venue (in the white house solution the only one)
                Venue venue = MapsIndoors.getVenues().getCurrentVenue();
                runOnUiThread( ()-> {
                    if (venue != null) {
                        //Animates the camera to fit the new venue
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(venue.getLatLngBoundingBox(), 19));
                    }
                });
            }
        });
    }

    /**
     * Performs a search for locations with mapsindoors and opens up a list of search results
     * @param searchQuery String to search for
     */
    void search(String searchQuery) {
        //Query with a string to search on
        MPQuery mpQuery = new MPQuery.Builder().setQuery(searchQuery).build();
        //Filter for the search query, only taking 30 locations
        MPFilter mpFilter = new MPFilter.Builder().setTake(30).build();

        //Gets locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter, (list, miError) -> {
            //Check if there is no error and the list is not empty
            if (miError == null && !list.isEmpty()) {
                //Create a new instance of the search fragment
                mSearchFragment = SearchFragment.newInstance(list, this);
                //Make a transaction to the bottomsheet
                getSupportFragmentManager().beginTransaction().replace(R.id.standardBottomSheet, mSearchFragment).commit();
                //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
                mMapControl.setMapPadding(0, 0,0,btmnSheetBehavior.getPeekHeight());
                if (btmnSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    btmnSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                //Assign search fragment to current fragment for ui logic
                currentFragment = mSearchFragment;
                //Clear the search text, since we got a result
                mSearchTxtField.getText().clear();
                //Calling displaySearch results on the ui thread as camera movement is involved
                runOnUiThread(()-> {
                    mMapControl.displaySearchResults(list, true);
                });
            }else {
                String alertDialogTitleTxt;
                String alertDialogTxt;
                if (list.isEmpty()) {
                    alertDialogTitleTxt = "No results found";
                    alertDialogTxt = "No results could be found for your search text. Try something else";
                }else {
                    if (miError != null) {
                        alertDialogTitleTxt = "Error: " + miError.code;
                        alertDialogTxt = miError.message;
                    }else {
                        alertDialogTitleTxt = "Unknown error";
                        alertDialogTxt = "Something went wrong, try another search text";
                    }
                }

                new AlertDialog.Builder(this)
                        .setTitle(alertDialogTitleTxt)
                        .setMessage(alertDialogTxt)
                        .show();
            }
        });
    }

    /**
     * Queries the MPRouting provider with a hardcoded user location and the location the user should be routed to
     * @param mpLocation A MPLocation to navigate to
     */
    void createRoute(MPLocation mpLocation) {
        //If MPRoutingProvider has not been instantiated create it here and assign the results call back to the activity.
        if (mpRoutingProvider == null) {
            mpRoutingProvider = new MPRoutingProvider();
            mpRoutingProvider.setOnRouteResultListener(this);
        }
        mpRoutingProvider.setTravelMode(TravelMode.WALKING);
        //Queries the MPRouting provider for a route with the hardcoded user location and the point from a location.
        mpRoutingProvider.query(mUserLocation, mpLocation.getPoint());
    }

    /**
     * The result callback from the route query. Starts the rendering of the route and opens up a new instance of the navigation fragment on the bottom sheet.
     * @param route the route model used to render a navigation view.
     * @param miError an MIError if anything goes wrong when generating a route
     */
    @Override
    public void onRouteResult(@Nullable Route route, @Nullable MIError miError) {
        //Return if either error is not null or the route is null
        if (miError != null || route == null) {
            //TODO: Tell the user about the route not being able to be created etc.
            return;
        }
        //Create the MPDirectionsRenderer if it has not been instantiated.
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer = new MPDirectionsRenderer(this, mMap, mMapControl, i -> {
                //Listener call back for when the user changes route leg. (By default is only called when a user presses the RouteLegs end marker)
                mpDirectionsRenderer.setRouteLegIndex(i);
                mMapControl.selectFloor(mpDirectionsRenderer.getCurrentFloor());
            });
        }
        //Set the route on the Directions renderer
        mpDirectionsRenderer.setRoute(route);
        //Create a new instance of the navigation fragment
        mNavigationFragment = NavigationFragment.newInstance(route, this);
        //Start a transaction and assign it to the BottomSheet
        getSupportFragmentManager().beginTransaction().replace(R.id.standardBottomSheet, mNavigationFragment).commit();
        if (btmnSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            btmnSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        //Assign the navigation fragment to current fragment. To handle ui logic
        currentFragment = mNavigationFragment;
        //As camera movement is involved run this on the UIThread
        runOnUiThread(()-> {
            //Starts drawing and adjusting the map according to the route
            mpDirectionsRenderer.initMap(true);
            mMapControl.setMapPadding(0, 0,0,btmnSheetBehavior.getPeekHeight());
        });
    }
}